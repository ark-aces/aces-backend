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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ContractController {

    // todo: get the current gas to eth rate from the network http://ethgasstation.info/
    private final BigDecimal gasPerEth = new BigDecimal("0.00000002");

    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");

    private final ContractMessageRepository contractMessageRepository;
    private final ContractMessageViewMapper contractMessageViewMapper;
    private final String serviceArkAddress;
    private final ScriptExecutorService scriptExecutorService;
    private final ExchangeRateService exchangeRateService;
    private final ArkClient arkClient;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
        .rootUri("http://localhost:8080/")
        .build();

    @PostMapping("/contracts")
    public ContractMessageView postContract(
        @RequestParam("abiJson") MultipartFile abiJsonFile,
        @RequestParam("code") MultipartFile codeFile,
        @RequestParam("params") MultipartFile paramsFile,
        @RequestParam("returnArkAddress") String returnArkAddress
    ) {
        String code = "0x" + IOUtilsWrapper.read(codeFile);
        String abiJson = IOUtilsWrapper.read(abiJsonFile);
        String paramsJson = IOUtilsWrapper.read(paramsFile);

        Long estimatedGasCost = scriptExecutorService.executeEstimateGas(code).getGasEstimate();
        BigDecimal estimatedEthCost = gasPerEth.multiply(new BigDecimal(estimatedGasCost));
        // todo: we should sanity check the exchange rate to prevent a bad rate being used
        BigDecimal ethPerArkExchangeRate = exchangeRateService.getRate("ETH", "ARK");
        BigDecimal estimatedArkCost = estimatedEthCost.divide(ethPerArkExchangeRate, RoundingMode.HALF_UP);

        ContractMessage contractMessage = new ContractMessage();
        contractMessage.setStatus(ContractMessage.STATUS_PENDING);
        contractMessage.setToken(UUID.randomUUID().toString());
        contractMessage.setContractCode(code);
        contractMessage.setContractAbiJson(abiJson);
        contractMessage.setContractParamsJson(paramsJson);
        contractMessage.setEthPerArkExchangeRate(ethPerArkExchangeRate.setScale(10, BigDecimal.ROUND_UP));
        contractMessage.setEstimatedGasCost(new BigDecimal(estimatedGasCost));
        contractMessage.setEstimatedEthCost(estimatedEthCost);
        contractMessage.setEstimatedArkCost(estimatedArkCost);
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

        String code = contractMessage.getContractCode();

        Long estimatedGasCost = scriptExecutorService.executeEstimateGas(code).getGasEstimate();
        BigDecimal estimatedEthCost = gasPerEth.multiply(new BigDecimal(estimatedGasCost));
        // todo: we should sanity check the exchange rate to prevent a bad rate being used
        BigDecimal ethPerArkExchangeRate = exchangeRateService.getRate("ETH", "ARK");
        BigDecimal estimatedArkCost = estimatedEthCost.divide(ethPerArkExchangeRate, RoundingMode.HALF_UP);

        // Ensure ark transaction contains enough ark to cover cost
        Transaction transaction = arkClient.getTransaction(transactionMatch.getArkTransactionId());

        // allow 2 * estimated ark + ark return transaction fee
        BigDecimal acceptableArkAmount = estimatedArkCost.multiply(new BigDecimal("2.0"))
            .add(arkTransactionFee);
        if (new BigDecimal(transaction.getAmount()).compareTo(acceptableArkAmount) > 0) {
            // The ark transaction does not contain sufficient ark to process
            contractMessage.setStatus(ContractMessage.STATUS_REJECTED);

            // todo: we should subtract ark transaction fees from return amount
            BigInteger returnArkAmount = transaction.getAmount();
            contractMessage.setReturnArkAmount(new BigDecimal(returnArkAmount));

            // todo create return ark transaction with remaining ark
            // contractMessage.setReturnArkTransactionId("...");

            contractMessageRepository.save(contractMessage);
        }

        // deploy eth contract corresponding to this ark transaction
        // todo: an error may occur during execution of before persisting the data, we should figure
        // out a way to make contract deployment idempotent
        ContractDeployResult contractDeployResult = scriptExecutorService.executeContractDeploy(
            contractMessage.getContractAbiJson(),
            contractMessage.getContractCode(),
            contractMessage.getContractParamsJson()
        );
        contractMessage.setContractTransactionHash(contractDeployResult.getTransactionHash());
        contractMessage.setContractAddress(contractDeployResult.getAddress());

        // todo: need to figure out actual eth/ark costs and save to message
        // contractMessage.setActualArkCost(actualArkCost);

        // todo: we should refund remaining ark to returnArkAddress
        // contractMessage.setReturnArkTransactionId("...");

        contractMessage.setStatus(ContractMessage.STATUS_COMPLETED);
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
