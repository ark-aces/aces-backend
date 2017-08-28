package example.ark_smartbridge_listener.eth_transfer;

import example.ark_smartbridge_listener.EthBalanceScriptExecutor;
import example.ark_smartbridge_listener.GetBalanceResult;
import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.ScriptExecutorService;
import example.ark_smartbridge_listener.ServiceInfoView;
import example.ark_smartbridge_listener.ark_listener.CreateMessageRequest;
import example.ark_smartbridge_listener.ark_listener.TransactionMatch;
import example.ark_smartbridge_listener.eth_contract_deploy.EthContractDeployContractEntity;
import example.ark_smartbridge_listener.exchange_rate_service.ExchangeRateService;
import io.ark.ark_client.ArkClient;
import io.ark.ark_client.EthTransactionResult;
import io.ark.ark_client.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@CrossOrigin
@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class EthTransferController {

    private final BigDecimal arkFlatFee = new BigDecimal("1.0000000");
    private final BigDecimal arkFeePercent = new BigDecimal("1.25");
    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");
    private final BigInteger satoshisPerArk = new BigInteger("100000000");

    private final EthTransferContractRepository ethTransferContractRepository;
    private final EthTransferContractViewMapper ethTransferContractViewMapper;
    private final String serviceArkAddress;
    private final ScriptExecutorService scriptExecutorService;
    private final ExchangeRateService exchangeRateService;
    private final ArkClient arkClient;
    private final String serviceArkPassphrase;
    private final RetryTemplate arkClientRetryTemplate;
    private final EthBalanceScriptExecutor ethBalanceScriptExecutor;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
            .rootUri("http://localhost:8080/")
            .build();

    @GetMapping("/eth-transfer-service-info")
    public ServiceInfoView getServiceInfo() {
        GetBalanceResult getBalanceResult = ethBalanceScriptExecutor.execute();

        ServiceInfoView serviceInfoView = new ServiceInfoView();
        serviceInfoView.setCapacity(getBalanceResult.getBalance().toPlainString() + " Eth");
        serviceInfoView.setFlatFeeArk(arkFlatFee.toPlainString());
        serviceInfoView.setPercentFee(arkFeePercent.toPlainString());
        serviceInfoView.setStatus(ServiceInfoView.STATUS_UP);

        return serviceInfoView;
    }

    @PostMapping("/eth-transfer-contracts")
    public EthTransferContractView createEthTransaction(
            @RequestParam("returnArkAddress") String returnArkAddress,
            @RequestParam("recipientEthAddress") String recipientEthAddress,
            @RequestParam("ethAmount") String ethAmountStr
    ) {
        BigDecimal ethAmount = new BigDecimal(ethAmountStr);

        // todo: we should sanity check the exchange rate to prevent a bad rate being used
        BigDecimal arkPerEthExchangeRate = exchangeRateService.getRate("ETH", "ARK");
        BigDecimal baseArkCost = ethAmount.multiply(arkPerEthExchangeRate)
                .add(arkTransactionFee); // add transaction fee for return transaction
        BigDecimal arkFeeTotal = baseArkCost.multiply(arkFeePercent.divide(new BigDecimal("100"), BigDecimal.ROUND_UP))
            .add(arkFlatFee);
        BigDecimal requiredArkCost = baseArkCost.add(arkFeeTotal);

        EthTransferContractEntity entity = new EthTransferContractEntity();
        entity.setToken(UUID.randomUUID().toString());
        entity.setCreatedAt(ZonedDateTime.from(Instant.now().atOffset(ZoneOffset.UTC)));
        entity.setStatus(EthTransferContractEntity.STATUS_PENDING);
        entity.setServiceArkAddress(serviceArkAddress);
        entity.setArkFlatFee(arkFlatFee);
        entity.setArkFeePercent(arkFeePercent);
        entity.setArkFeeTotal(arkFeeTotal);
        entity.setRequiredArkAmount(requiredArkCost.setScale(8, BigDecimal.ROUND_UP));
        entity.setReturnArkAddress(returnArkAddress);
        entity.setRecipientEthAddress(recipientEthAddress);
        entity.setEthAmount(ethAmount);
        entity.setArkPerEthExchangeRate(arkPerEthExchangeRate.setScale(8, BigDecimal.ROUND_UP));
        ethTransferContractRepository.save(entity);

        // Register contract message with listener so we get a callback when a matching ark transaction is found
        CreateMessageRequest createMessageRequest = new CreateMessageRequest();
        createMessageRequest.setCallbackUrl("http://localhost:8080/eth-transfer-contracts/ark-transaction-matches");
        createMessageRequest.setToken(entity.getToken());
        listenerRestTemplate.postForObject("/messages", createMessageRequest, Void.class);

        return ethTransferContractViewMapper.map(entity);
    }

    @GetMapping("/eth-transfer-contracts/{token}")
    public EthTransferContractView getTransaction(@PathVariable String token) {
        EthTransferContractEntity ethTransferContractEntity = getEthTransferContractOrThrowException(token);

        return ethTransferContractViewMapper.map(ethTransferContractEntity);
    }

    @PostMapping("/eth-transfer-contracts/ark-transaction-matches")
    public void postArkTransactionMatches(@RequestBody TransactionMatch transactionMatch) {
        String token = transactionMatch.getToken();
        EthTransferContractEntity ethTransferContractEntity = getEthTransferContractOrThrowException(token);

        // Skip already processed transactions
        if (!ethTransferContractEntity.getStatus().equals(EthContractDeployContractEntity.STATUS_PENDING)) {
            return;
        }

        Transaction transaction = arkClientRetryTemplate.execute(retryContext ->
                arkClient.getTransaction(transactionMatch.getArkTransactionId()));

        BigDecimal transactionArkAmount = new BigDecimal(transaction.getAmount())
                .setScale(14, BigDecimal.ROUND_UP)
                .divide(new BigDecimal(satoshisPerArk), BigDecimal.ROUND_UP);

        // Ensure ark transaction contains enough ark to cover cost
        Long deploymentArkSatoshiCost = 0L;
        if (transactionArkAmount.compareTo(ethTransferContractEntity.getRequiredArkAmount()) >= 0) {
            // todo: since deploying an eth contract breaks a transaction boundary (eth contract deployment
            // is not idempotent), we should handle the failure scenario in a better way

            // deploy eth contract corresponding to this ark transaction
            EthTransactionResult ethTransactionResult;
            try {
                ethTransactionResult = scriptExecutorService.executeEthTransaction(
                    ethTransferContractEntity.getRecipientEthAddress(),
                    ethTransferContractEntity.getEthAmount().toPlainString()
                );
                ethTransferContractEntity.setEthTransactionHash(ethTransactionResult.getTransactionHash());

            }
            catch (Exception e) {
                log.error("Failed to execute deploy script", e);
                ethTransferContractEntity.setStatus(EthTransferContractEntity.STATUS_FAILED);
            }

            // todo: need to figure out actual eth/ark costs and save to message
            BigDecimal actualArkAmount = ethTransferContractEntity.getEthAmount().multiply(ethTransferContractEntity.getArkPerEthExchangeRate());
            ethTransferContractEntity.setActualArkAmount(actualArkAmount);

            deploymentArkSatoshiCost = actualArkAmount.multiply(new BigDecimal(satoshisPerArk))
                    .toBigInteger()
                    .longValueExact();

            ethTransferContractEntity.setStatus(EthContractDeployContractEntity.STATUS_COMPLETED);
        } else {
            // The ark transaction does not contain sufficient ark to process
            ethTransferContractEntity.setStatus(EthContractDeployContractEntity.STATUS_REJECTED);
        }

        // Subtract ark transaction fees from return amount
        Long arkTransactionFeeSatoshis = arkTransactionFee
                .multiply(new BigDecimal(satoshisPerArk))
                .toBigIntegerExact().longValue();

        Long feeSatoshis = ethTransferContractEntity.getArkFeeTotal()
            .multiply(new BigDecimal(satoshisPerArk))
            .toBigIntegerExact().longValue();

        Long returnSatoshiAmount = transaction.getAmount() - deploymentArkSatoshiCost - arkTransactionFeeSatoshis
            - feeSatoshis;
        if (returnSatoshiAmount < 0) {
            returnSatoshiAmount = 0L;
        }

        BigDecimal returnArkAmount = new BigDecimal(returnSatoshiAmount)
                .setScale(14, BigDecimal.ROUND_UP)
                .divide(new BigDecimal(satoshisPerArk), BigDecimal.ROUND_UP);
        ethTransferContractEntity.setReturnArkAmount(returnArkAmount);

        if (returnSatoshiAmount > 0) {
            // todo: handle the case where an error occurs sending return ark.
            // Create return ark transaction with remaining ark
            Long finalReturnSatoshiAmount = returnSatoshiAmount;

            String returnArkTransactionId;
            try {
                returnArkTransactionId = arkClientRetryTemplate.execute(retryContext ->
                    arkClient.createTransaction(
                        ethTransferContractEntity.getReturnArkAddress(),
                        finalReturnSatoshiAmount,
                        ethTransferContractEntity.getToken(),
                        serviceArkPassphrase
                    ));
                ethTransferContractEntity.setReturnArkTransactionId(returnArkTransactionId);
            }
            catch (Exception e) {
                log.error("Failed to send return ark transaction", e);
                ethTransferContractEntity.setStatus(EthTransferContractEntity.STATUS_FAILED);
            }
        }

        ethTransferContractRepository.save(ethTransferContractEntity);
    }

    private EthTransferContractEntity getEthTransferContractOrThrowException(String token) {
        EthTransferContractEntity ethTransactionMessage = ethTransferContractRepository.findOneByToken(token);
        if (ethTransactionMessage == null) {
            throw new NotFoundException("Contract not found");
        }
        return ethTransactionMessage;
    }
}
