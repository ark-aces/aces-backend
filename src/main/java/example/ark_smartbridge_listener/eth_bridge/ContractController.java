package example.ark_smartbridge_listener.eth_bridge;

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
public class ContractController {

    // todo: get the current eth per gas rate from the network http://ethgasstation.info/
    private final BigDecimal ethPerGas = new BigDecimal("0.00000002");

    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");

    private final BigInteger satoshisPerArk = new BigInteger("100000000");

    private final ContractMessageRepository contractMessageRepository;
    private final ContractMessageViewMapper contractMessageViewMapper;
    private final String serviceArkAddress;
    private final ScriptExecutorService scriptExecutorService;
    private final ExchangeRateService exchangeRateService;
    private final ArkClient arkClient;
    private final String serviceArkPassphrase;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
        .rootUri("http://localhost:8080/")
        .build();

    @PostMapping("/contracts")
    public ContractMessageView postContract(
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

        ContractMessage contractMessage = new ContractMessage();
        contractMessage.setStatus(ContractMessage.STATUS_PENDING);
        contractMessage.setToken(UUID.randomUUID().toString());
        contractMessage.setContractCode(code);
        contractMessage.setContractAbiJson(abiJson);
        contractMessage.setContractParamsJson(paramsJson);
        contractMessage.setGasLimit(gasLimit);
        contractMessage.setArkPerEthExchangeRate(arkPerEthExchangeRate.setScale(8, BigDecimal.ROUND_UP));
        contractMessage.setEstimatedGasCost(estimatedGasCost);
        contractMessage.setEstimatedEthCost(estimatedEthCost.setScale(8, BigDecimal.ROUND_UP));
        contractMessage.setRequiredArkCost(requiredArkCost.setScale(8, BigDecimal.ROUND_UP));
        contractMessage.setCreatedAt(ZonedDateTime.from(Instant.now().atOffset(ZoneOffset.UTC)));
        contractMessage.setServiceArkAddress(serviceArkAddress);
        contractMessage.setReturnArkAddress(returnArkAddress);
        contractMessageRepository.save(contractMessage);

        // Register contract message with listener so we get a callback when a matching ark transaction is found
        CreateMessageRequest createMessageRequest = new CreateMessageRequest();
        createMessageRequest.setCallbackUrl("http://localhost:8080/ark-transactions");
        createMessageRequest.setToken(contractMessage.getToken());
        listenerRestTemplate.postForObject("/messages", createMessageRequest, Void.class);

        return contractMessageViewMapper.map(contractMessage);
    }

    @GetMapping("/contracts/{token}")
    public ContractMessageView getContract(@PathVariable String token) {
        ContractMessage contractMessage = getContractMessageOrThrowNotFound(token);

        return contractMessageViewMapper.map(contractMessage);
    }

    @PostMapping("/ark-transactions")
    public void postArkTransactions(@RequestBody TransactionMatch transactionMatch) {
        String token = transactionMatch.getToken();
        ContractMessage contractMessage = getContractMessageOrThrowNotFound(token);

        // Skip already processed transactions
        if (! contractMessage.getStatus().equals(ContractMessage.STATUS_PENDING)) {
            return;
        }

        String code = contractMessage.getContractCode();

        Long estimatedGasCost = scriptExecutorService.executeEstimateGas(code).getGasEstimate();
        BigDecimal estimatedEthCost = ethPerGas.multiply(new BigDecimal(estimatedGasCost));
        // todo: we should sanity check the exchange rate to prevent a bad rate being used
        BigDecimal arkPerEthExchangeRate = exchangeRateService.getRate("ETH", "ARK");
        BigDecimal estimatedArkCost = estimatedEthCost.multiply(arkPerEthExchangeRate);
        BigDecimal requiredArkCost = estimatedArkCost.multiply(new BigDecimal("2.0")) // require a 2x buffer
            .add(arkTransactionFee); // add transaction fee for return transaction

        Transaction transaction = arkClient.getTransaction(transactionMatch.getArkTransactionId());

        BigDecimal transactionArkAmount = new BigDecimal(transaction.getAmount())
            .setScale(14, BigDecimal.ROUND_UP)
            .divide(new BigDecimal(satoshisPerArk), BigDecimal.ROUND_UP);

        // Ensure ark transaction contains enough ark to cover cost
        Long deploymentArkSatoshiCost = 0L;
        if (transactionArkAmount.compareTo(requiredArkCost) >= 0) {
            // todo: since deploying an eth contract breaks a transaction boundary (eth contract deployment
            // is not idempotent), we should handle the failure scenario in a better way

            // deploy eth contract corresponding to this ark transaction
            ContractDeployResult contractDeployResult = scriptExecutorService.executeContractDeploy(
                contractMessage.getContractAbiJson(),
                contractMessage.getContractCode(),
                contractMessage.getContractParamsJson(),
                contractMessage.getGasLimit()
            );
            contractMessage.setContractTransactionHash(contractDeployResult.getTransactionHash());
            contractMessage.setContractAddress(contractDeployResult.getAddress());
            contractMessage.setGasUsed(contractDeployResult.getGasUsed());

            // todo: need to figure out actual eth/ark costs and save to message
            BigDecimal actualEthCost = ethPerGas.multiply(new BigDecimal(contractDeployResult.getGasUsed()));
            BigDecimal actualArkCost = actualEthCost.multiply(arkPerEthExchangeRate);
            contractMessage.setDeploymentArkCost(actualArkCost);

            deploymentArkSatoshiCost = actualArkCost.multiply(new BigDecimal(satoshisPerArk))
                .toBigInteger()
                .longValueExact();

            contractMessage.setStatus(ContractMessage.STATUS_COMPLETED);
        } else {
            // The ark transaction does not contain sufficient ark to process
            contractMessage.setStatus(ContractMessage.STATUS_REJECTED);
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
        contractMessage.setReturnArkAmount(returnArkAmount);

        if (returnSatoshiAmount > 0) {
            // todo: handle the case where an error occurs sending return ark.
            // Create return ark transaction with remaining ark
            String returnArkTransactionId = arkClient.createTransaction(
                contractMessage.getReturnArkAddress(),
                returnSatoshiAmount,
                contractMessage.getToken(),
                serviceArkPassphrase
            );
            contractMessage.setReturnArkTransactionId(returnArkTransactionId);
        }

        contractMessageRepository.save(contractMessage);
    }

    private ContractMessage getContractMessageOrThrowNotFound(String token) {
        ContractMessage contractMessage = contractMessageRepository.findOneByToken(token);
        if (contractMessage == null) {
            throw new NotFoundException("Contract not found");
        }
        return contractMessage;
    }
}
