package example.ark_smartbridge_listener.eth_contract_deploy;

import example.ark_smartbridge_listener.ArkService;
import example.ark_smartbridge_listener.EthBalanceScriptExecutor;
import example.ark_smartbridge_listener.GetBalanceResult;
import example.ark_smartbridge_listener.ScriptExecutorService;
import example.ark_smartbridge_listener.ServiceInfoView;
import example.ark_smartbridge_listener.ark_listener.CreateMessageRequest;
import example.ark_smartbridge_listener.exchange_rate_service.ExchangeRateService;
import lib.IOUtilsWrapper;
import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.ark_listener.TransactionMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@CrossOrigin
@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class EthContractDeployController {

    private final BigDecimal arkFlatFee = new BigDecimal("2.0000000");
    private final BigDecimal arkFeePercent = new BigDecimal("2.25000000");

    // todo: get the current eth per gas rate from the network http://ethgasstation.info/
    private final BigDecimal ethPerGas = new BigDecimal("0.00000002");

    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");

    private final EthContractDeployContractRepository ethContractDeployContractRepository;
    private final EthContractDeployContractViewMapper ethContractDeployContractViewMapper;
    private final String serviceArkAddress;
    private final ScriptExecutorService scriptExecutorService;
    private final ExchangeRateService exchangeRateService;
    private final EthBalanceScriptExecutor ethBalanceScriptExecutor;
    private final ArkService arkService;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
        .rootUri("http://localhost:8080/")
        .build();

    @GetMapping("/eth-contract-deploy-service-info")
    public ServiceInfoView getServiceInfo() {
        GetBalanceResult getBalanceResult = ethBalanceScriptExecutor.execute();

        ServiceInfoView serviceInfoView = new ServiceInfoView();
        serviceInfoView.setCapacity(
            getBalanceResult.getBalance().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + " Eth");
        serviceInfoView.setFlatFeeArk(arkFlatFee.toPlainString());
        serviceInfoView.setPercentFee(arkFeePercent.toPlainString());
        serviceInfoView.setStatus(ServiceInfoView.STATUS_UP);

        return serviceInfoView;
    }

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
        BigDecimal arkPerEthExchangeRate = exchangeRateService.getRate("ETH", "ARK");
        BigDecimal estimatedArkCost = estimatedEthCost.multiply(arkPerEthExchangeRate);
        BigDecimal baseArkCost = estimatedArkCost.multiply(new BigDecimal("1.20000000")); // require a 1.2 buffer
        BigDecimal arkFeeTotal = baseArkCost
            .multiply(arkFeePercent.divide(new BigDecimal("100.00000000"), BigDecimal.ROUND_UP))
            .add(arkFlatFee)
            .add(arkTransactionFee);
        BigDecimal requiredArkCost = baseArkCost.add(arkFeeTotal);

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
        ethContractDeployContractEntity.setArkFlatFee(arkFlatFee);
        ethContractDeployContractEntity.setArkFeePercent(arkFeePercent);
        ethContractDeployContractEntity.setArkFeeTotal(arkFeeTotal);
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
        EthContractDeployContractEntity ethContractDeployContractEntity = getContractEntityOrThrowNotFound(token);

        return ethContractDeployContractViewMapper.map(ethContractDeployContractEntity);
    }

    @PostMapping("/eth-contract-deploy-contracts/ark-transactions")
    public void postArkTransactions(@RequestBody TransactionMatch transactionMatch) {
        String token = transactionMatch.getToken();
        EthContractDeployContractEntity contractEntity = getContractEntityOrThrowNotFound(token);

        // Skip already processed transactions
        if (!contractEntity.getStatus().equals(EthContractDeployContractEntity.STATUS_PENDING)) {
            return;
        }

        log.info("Processing eth contract deploy contract with token " + token);

        try {
            String code = contractEntity.getContractCode();
            Long estimatedGasCost = scriptExecutorService.executeEstimateGas(code).getGasEstimate();
            BigDecimal estimatedEthCost = ethPerGas.multiply(new BigDecimal(estimatedGasCost));
            BigDecimal arkPerEthExchangeRate = exchangeRateService.getRate("ETH", "ARK");
            BigDecimal baseArkCost = estimatedEthCost.multiply(arkPerEthExchangeRate);
            BigDecimal arkFeeTotal = baseArkCost
                .multiply(arkFeePercent.divide(new BigDecimal("100.00000000"), BigDecimal.ROUND_UP))
                .add(arkFlatFee)
                .add(arkTransactionFee);
            BigDecimal requiredArkCost = baseArkCost.add(arkFeeTotal);

            BigDecimal sentArkAmount = arkService.getArkAmount(transactionMatch.getArkTransactionId());
            BigDecimal usedArkAmount = BigDecimal.ZERO;
            if (sentArkAmount.compareTo(requiredArkCost) >= 0) {
                // todo: since deploying an eth contract breaks a transaction boundary (eth contract deployment
                // is not idempotent), we should handle the failure scenario in a better way

                // deploy eth contract corresponding to this ark transaction
                EthContractDeployResult ethContractDeployResult = scriptExecutorService.executeContractDeploy(
                    contractEntity.getContractAbiJson(),
                    contractEntity.getContractCode(),
                    contractEntity.getContractParamsJson(),
                    contractEntity.getGasLimit()
                );
                contractEntity.setContractTransactionHash(ethContractDeployResult.getTransactionHash());
                contractEntity.setContractAddress(ethContractDeployResult.getAddress());
                contractEntity.setGasUsed(ethContractDeployResult.getGasUsed());

                // We should only charge for eth consumed in deployment if successful. Since the contract
                // deployment is async on the eth blockchain and we won't know the actual gas used at this point,
                // we'll need to charge the full gasLimit here.
                // A better solution might be to check the eth contract deployment transaction at a later time
                // and get that information to send return transaction, but that complicates the code a little since
                // we need to add an async background worker.
                BigDecimal actualEthCost = ethPerGas.multiply(new BigDecimal(contractEntity.getGasLimit()));
                BigDecimal actualArkCost = actualEthCost.multiply(arkPerEthExchangeRate);
                contractEntity.setDeploymentArkCost(actualArkCost);

                usedArkAmount = actualArkCost.add(contractEntity.getArkFeeTotal());

                contractEntity.setStatus(EthContractDeployContractEntity.STATUS_COMPLETED);
            } else {
                usedArkAmount = arkTransactionFee;
                contractEntity.setStatus(EthContractDeployContractEntity.STATUS_REJECTED);
            }

            BigDecimal returnArkAmount = arkService.calculateReturnArkAmount(sentArkAmount, usedArkAmount);
            contractEntity.setReturnArkAmount(returnArkAmount);

            if (returnArkAmount.compareTo(BigDecimal.ZERO) > 0) {
                String returnArkTransactionId = arkService
                    .sendArk(contractEntity.getReturnArkAddress(), returnArkAmount, contractEntity.getToken());
                contractEntity.setReturnArkTransactionId(returnArkTransactionId);
            }
        }
        catch (Exception e) {
            log.error("Failed to execute deploy script", e);
            contractEntity.setStatus(EthContractDeployContractEntity.STATUS_FAILED);
        }

        ethContractDeployContractRepository.save(contractEntity);
    }

    private EthContractDeployContractEntity getContractEntityOrThrowNotFound(String token) {
        EthContractDeployContractEntity ethContractDeployContractEntity = ethContractDeployContractRepository.findOneByToken(token);
        if (ethContractDeployContractEntity == null) {
            throw new NotFoundException("Contract not found");
        }
        return ethContractDeployContractEntity;
    }
}
