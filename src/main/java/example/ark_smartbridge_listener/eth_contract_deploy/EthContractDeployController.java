package example.ark_smartbridge_listener.eth_contract_deploy;

import example.ark_smartbridge_listener.ScriptExecutorService;
import example.ark_smartbridge_listener.ark_listener.CreateMessageRequest;
import example.ark_smartbridge_listener.exchange_rate_service.ExchangeRateService;
import io.ark.ark_client.ArkClient;
import io.ark.ark_client.Transaction;
import lib.IOUtilsWrapper;
import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.ark_listener.TransactionMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class EthContractDeployController {

    // todo: get the current eth per gas rate from the network http://ethgasstation.info/
    private final BigDecimal ethPerGas = new BigDecimal("0.00000002");

    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");

    private final BigInteger satoshisPerArk = new BigInteger("100000000");

    private final EthContractDeployContractRepository ethContractDeployContractRepository;
    private final EthContractDeployContractViewMapper ethContractDeployContractViewMapper;
    private final String serviceArkAddress;
    private final ScriptExecutorService scriptExecutorService;
    private final ExchangeRateService exchangeRateService;
    private final ArkClient arkClient;
    private final String serviceArkPassphrase;
    private final RetryTemplate arkClientRetryTemplate;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
        .rootUri("http://localhost:8080/")
        .build();

    @PostMapping("/eth-contract-deploy-contracts")
    public EthContractDeployContractView postContract(
        @RequestParam("abiJson") MultipartFile abiJsonFile,
        @RequestParam("code") MultipartFile codeFile,
        @RequestParam("params") MultipartFile paramsFile,
        @RequestParam("gasLimit") Long gasLimit,
        @RequestParam("returnArkAddress") String returnArkAddress
    ) {
        String code = "0x" + IOUtilsWrapper.read(codeFile);
        String abiJson = IOUtilsWrapper.read(abiJsonFile);
        String paramsJson = IOUtilsWrapper.read(paramsFile);

        Long estimatedGasCost = scriptExecutorService.executeEstimateGas(code).getGasEstimate();
        if (gasLimit < estimatedGasCost) {
            // todo: return an error to the user and do not create contract message
            // it doesn't really matter if we continue on since the contract will get rejected
            // when ark transaction is sent, but we should be nice and return an error early to
            // avoid transaction fees for the client
        }

        BigDecimal estimatedEthCost = ethPerGas.multiply(new BigDecimal(estimatedGasCost));
        // todo: we should sanity check the exchange rate to prevent a bad rate being used
        BigDecimal arkPerEthExchangeRate = exchangeRateService.getRate("ETH", "ARK");
        BigDecimal estimatedArkCost = estimatedEthCost.multiply(arkPerEthExchangeRate);
        BigDecimal requiredArkCost = estimatedArkCost.multiply(new BigDecimal("2.0")) // require a 2x buffer
            .add(arkTransactionFee); // add transaction fee for return transaction

        EthContractDeployContractEntity ethContractDeployContractEntity = new EthContractDeployContractEntity();
        ethContractDeployContractEntity.setStatus(EthContractDeployContractEntity.STATUS_PENDING);
        ethContractDeployContractEntity.setToken(UUID.randomUUID().toString());
        ethContractDeployContractEntity.setContractCode(code);
        ethContractDeployContractEntity.setContractAbiJson(abiJson);
        ethContractDeployContractEntity.setContractParamsJson(paramsJson);
        ethContractDeployContractEntity.setGasLimit(gasLimit);
        ethContractDeployContractEntity.setArkPerEthExchangeRate(arkPerEthExchangeRate.setScale(8, BigDecimal.ROUND_UP));
        ethContractDeployContractEntity.setEstimatedGasCost(estimatedGasCost);
        ethContractDeployContractEntity.setEstimatedEthCost(estimatedEthCost.setScale(8, BigDecimal.ROUND_UP));
        ethContractDeployContractEntity.setRequiredArkCost(requiredArkCost.setScale(8, BigDecimal.ROUND_UP));
        ethContractDeployContractEntity.setCreatedAt(ZonedDateTime.from(Instant.now().atOffset(ZoneOffset.UTC)));
        ethContractDeployContractEntity.setServiceArkAddress(serviceArkAddress);
        ethContractDeployContractEntity.setReturnArkAddress(returnArkAddress);
        ethContractDeployContractRepository.save(ethContractDeployContractEntity);

        // Register contract message with listener so we get a callback when a matching ark transaction is found
        CreateMessageRequest createMessageRequest = new CreateMessageRequest();
        createMessageRequest.setCallbackUrl("http://localhost:8080/eth-contract-deploy-contracts/ark-transactions");
        createMessageRequest.setToken(ethContractDeployContractEntity.getToken());
        listenerRestTemplate.postForObject("/messages", createMessageRequest, Void.class);

        return ethContractDeployContractViewMapper.map(ethContractDeployContractEntity);
    }

    @GetMapping("/eth-contract-deploy-contracts/{token}")
    public EthContractDeployContractView getContract(@PathVariable String token) {
        EthContractDeployContractEntity ethContractDeployContractEntity = getContractMessageOrThrowNotFound(token);

        return ethContractDeployContractViewMapper.map(ethContractDeployContractEntity);
    }

    @PostMapping("/eth-contract-deploy-contracts/ark-transactions")
    public void postArkTransactions(@RequestBody TransactionMatch transactionMatch) {
        String token = transactionMatch.getToken();
        EthContractDeployContractEntity ethContractDeployContractEntity = getContractMessageOrThrowNotFound(token);

        // Skip already processed transactions
        if (! ethContractDeployContractEntity.getStatus().equals(EthContractDeployContractEntity.STATUS_PENDING)) {
            return;
        }

        String code = ethContractDeployContractEntity.getContractCode();

        Long estimatedGasCost = scriptExecutorService.executeEstimateGas(code).getGasEstimate();
        BigDecimal estimatedEthCost = ethPerGas.multiply(new BigDecimal(estimatedGasCost));
        // todo: we should sanity check the exchange rate to prevent a bad rate being used
        BigDecimal arkPerEthExchangeRate = exchangeRateService.getRate("ETH", "ARK");
        BigDecimal estimatedArkCost = estimatedEthCost.multiply(arkPerEthExchangeRate);
        BigDecimal requiredArkCost = estimatedArkCost.multiply(new BigDecimal("2.0")) // require a 2x buffer
            .add(arkTransactionFee); // add transaction fee for return transaction

        Transaction transaction = arkClientRetryTemplate.execute(retryContext ->
            arkClient.getTransaction(transactionMatch.getArkTransactionId()));

        BigDecimal transactionArkAmount = new BigDecimal(transaction.getAmount())
            .setScale(14, BigDecimal.ROUND_UP)
            .divide(new BigDecimal(satoshisPerArk), BigDecimal.ROUND_UP);

        // Ensure ark transaction contains enough ark to cover cost
        Long deploymentArkSatoshiCost = 0L;
        if (transactionArkAmount.compareTo(requiredArkCost) >= 0) {
            // todo: since deploying an eth contract breaks a transaction boundary (eth contract deployment
            // is not idempotent), we should handle the failure scenario in a better way

            // deploy eth contract corresponding to this ark transaction
            EthContractDeployResult ethContractDeployResult = scriptExecutorService.executeContractDeploy(
                ethContractDeployContractEntity.getContractAbiJson(),
                ethContractDeployContractEntity.getContractCode(),
                ethContractDeployContractEntity.getContractParamsJson(),
                ethContractDeployContractEntity.getGasLimit()
            );
            ethContractDeployContractEntity.setContractTransactionHash(ethContractDeployResult.getTransactionHash());
            ethContractDeployContractEntity.setContractAddress(ethContractDeployResult.getAddress());
            ethContractDeployContractEntity.setGasUsed(ethContractDeployResult.getGasUsed());

            // todo: need to figure out actual eth/ark costs and save to message
            BigDecimal actualEthCost = ethPerGas.multiply(new BigDecimal(ethContractDeployResult.getGasUsed()));
            BigDecimal actualArkCost = actualEthCost.multiply(arkPerEthExchangeRate);
            ethContractDeployContractEntity.setDeploymentArkCost(actualArkCost);

            deploymentArkSatoshiCost = actualArkCost.multiply(new BigDecimal(satoshisPerArk))
                .toBigInteger()
                .longValueExact();

            ethContractDeployContractEntity.setStatus(EthContractDeployContractEntity.STATUS_COMPLETED);
        } else {
            // The ark transaction does not contain sufficient ark to process
            ethContractDeployContractEntity.setStatus(EthContractDeployContractEntity.STATUS_REJECTED);
        }

        // Subtract ark transaction fees from return amount
        Long arkTransactionFeeSatoshis = arkTransactionFee
            .multiply(new BigDecimal(satoshisPerArk))
            .toBigIntegerExact().longValue();

        Long returnSatoshiAmount = transaction.getAmount() - deploymentArkSatoshiCost - arkTransactionFeeSatoshis;
        if (returnSatoshiAmount < 0) {
            returnSatoshiAmount = 0L;
        }

        BigDecimal returnArkAmount = new BigDecimal(returnSatoshiAmount)
            .setScale(14, BigDecimal.ROUND_UP)
            .divide(new BigDecimal(satoshisPerArk), BigDecimal.ROUND_UP);
        ethContractDeployContractEntity.setReturnArkAmount(returnArkAmount);

        if (returnSatoshiAmount > 0) {
            // todo: handle the case where an error occurs sending return ark.
            // Create return ark transaction with remaining ark
            Long finalReturnSatoshiAmount = returnSatoshiAmount;
            String returnArkTransactionId = arkClientRetryTemplate.execute(retryContext ->
                arkClient.createTransaction(
                    ethContractDeployContractEntity.getReturnArkAddress(),
                    finalReturnSatoshiAmount,
                    ethContractDeployContractEntity.getToken(),
                    serviceArkPassphrase
                ));

            ethContractDeployContractEntity.setReturnArkTransactionId(returnArkTransactionId);
        }

        ethContractDeployContractRepository.save(ethContractDeployContractEntity);
    }

    private EthContractDeployContractEntity getContractMessageOrThrowNotFound(String token) {
        EthContractDeployContractEntity ethContractDeployContractEntity = ethContractDeployContractRepository.findOneByToken(token);
        if (ethContractDeployContractEntity == null) {
            throw new NotFoundException("Contract not found");
        }
        return ethContractDeployContractEntity;
    }
}