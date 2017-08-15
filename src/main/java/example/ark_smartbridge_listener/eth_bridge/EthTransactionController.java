package example.ark_smartbridge_listener.eth_bridge;

import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.ark_listener.CreateMessageRequest;
import example.ark_smartbridge_listener.ark_listener.TransactionMatch;
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

@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class EthTransactionController {

    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");
    private final BigInteger satoshisPerArk = new BigInteger("100000000");

    private final EthTransactionMessageRepository ethTransactionMessageRepository;
    private final EthTransactionMessageViewMapper ethTransactionMessageViewMapper;
    private final String serviceArkAddress;
    private final ScriptExecutorService scriptExecutorService;
    private final ExchangeRateService exchangeRateService;
    private final ArkClient arkClient;
    private final String serviceArkPassphrase;
    private final RetryTemplate arkClientRetryTemplate;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
            .rootUri("http://localhost:8080/")
            .build();

    @PostMapping("/eth-transactions")
    public EthTransactionMessageView createEthTransaction(
            @RequestParam("returnArkAddress") String returnArkAddress,
            @RequestParam("recipientEthAddress") String recipientEthAddress,
            @RequestParam("ethAmount") String ethAmountStr
    ) {
        BigDecimal ethAmount = new BigDecimal(ethAmountStr);

        // todo: we should sanity check the exchange rate to prevent a bad rate being used
        BigDecimal arkPerEthExchangeRate = exchangeRateService.getRate("ETH", "ARK");
        BigDecimal requiredArkCost = ethAmount.multiply(arkPerEthExchangeRate)
                .add(arkTransactionFee); // add transaction fee for return transaction

        EthTransactionMessage ethTransactionMessage = new EthTransactionMessage();
        ethTransactionMessage.setToken(UUID.randomUUID().toString());
        ethTransactionMessage.setCreatedAt(ZonedDateTime.from(Instant.now().atOffset(ZoneOffset.UTC)));
        ethTransactionMessage.setStatus(ContractMessage.STATUS_PENDING);
        ethTransactionMessage.setServiceArkAddress(serviceArkAddress);
        ethTransactionMessage.setRequiredArkAmount(requiredArkCost.setScale(8, BigDecimal.ROUND_UP));
        ethTransactionMessage.setReturnArkAddress(returnArkAddress);
        ethTransactionMessage.setRecipientEthAddress(recipientEthAddress);
        ethTransactionMessage.setEthAmount(ethAmount);
        ethTransactionMessage.setArkPerEthExchangeRate(arkPerEthExchangeRate.setScale(8, BigDecimal.ROUND_UP));
        ethTransactionMessageRepository.save(ethTransactionMessage);

        // Register contract message with listener so we get a callback when a matching ark transaction is found
        CreateMessageRequest createMessageRequest = new CreateMessageRequest();
        createMessageRequest.setCallbackUrl("http://localhost:8080/eth-transaction-ark-transactions");
        createMessageRequest.setToken(ethTransactionMessage.getToken());
        listenerRestTemplate.postForObject("/messages", createMessageRequest, Void.class);

        return ethTransactionMessageViewMapper.map(ethTransactionMessage);
    }

    @GetMapping("/eth-transactions/{token}")
    public EthTransactionMessageView getTransaction(@PathVariable String token) {
        EthTransactionMessage ethTransactionMessage = getEthTransactionMessageOrThrowException(token);

        return ethTransactionMessageViewMapper.map(ethTransactionMessage);
    }

    @PostMapping("/eth-transaction-ark-transactions")
    public void postEthTransactionArkTransactions(@RequestBody TransactionMatch transactionMatch) {
        String token = transactionMatch.getToken();
        EthTransactionMessage ethTransactionMessage = getEthTransactionMessageOrThrowException(token);

        // Skip already processed transactions
        if (!ethTransactionMessage.getStatus().equals(ContractMessage.STATUS_PENDING)) {
            return;
        }

        Transaction transaction = arkClientRetryTemplate.execute(retryContext ->
                arkClient.getTransaction(transactionMatch.getArkTransactionId()));

        BigDecimal transactionArkAmount = new BigDecimal(transaction.getAmount())
                .setScale(14, BigDecimal.ROUND_UP)
                .divide(new BigDecimal(satoshisPerArk), BigDecimal.ROUND_UP);

        // Ensure ark transaction contains enough ark to cover cost
        Long deploymentArkSatoshiCost = 0L;
        if (transactionArkAmount.compareTo(ethTransactionMessage.getRequiredArkAmount()) >= 0) {
            // todo: since deploying an eth contract breaks a transaction boundary (eth contract deployment
            // is not idempotent), we should handle the failure scenario in a better way

            // deploy eth contract corresponding to this ark transaction
            EthTransactionResult ethTransactionResult = scriptExecutorService.executeEthTransaction(
                    ethTransactionMessage.getRecipientEthAddress(),
                    ethTransactionMessage.getEthAmount().toPlainString()
            );
            ethTransactionMessage.setEthTransactionHash(ethTransactionResult.getTransactionHash());

            // todo: need to figure out actual eth/ark costs and save to message
            BigDecimal actualArkAmount = ethTransactionMessage.getEthAmount().multiply(ethTransactionMessage.getArkPerEthExchangeRate());
            ethTransactionMessage.setActualArkAmount(actualArkAmount);

            deploymentArkSatoshiCost = actualArkAmount.multiply(new BigDecimal(satoshisPerArk))
                    .toBigInteger()
                    .longValueExact();

            ethTransactionMessage.setStatus(ContractMessage.STATUS_COMPLETED);
        } else {
            // The ark transaction does not contain sufficient ark to process
            ethTransactionMessage.setStatus(ContractMessage.STATUS_REJECTED);
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
        ethTransactionMessage.setReturnArkAmount(returnArkAmount);

        if (returnSatoshiAmount > 0) {
            // todo: handle the case where an error occurs sending return ark.
            // Create return ark transaction with remaining ark
            Long finalReturnSatoshiAmount = returnSatoshiAmount;
            String returnArkTransactionId = arkClientRetryTemplate.execute(retryContext ->
                    arkClient.createTransaction(
                            ethTransactionMessage.getReturnArkAddress(),
                            finalReturnSatoshiAmount,
                            ethTransactionMessage.getToken(),
                            serviceArkPassphrase
                    ));

            ethTransactionMessage.setReturnArkTransactionId(returnArkTransactionId);
        }

        ethTransactionMessageRepository.save(ethTransactionMessage);
    }

    private EthTransactionMessage getEthTransactionMessageOrThrowException(String token) {
        EthTransactionMessage ethTransactionMessage = ethTransactionMessageRepository.findOneByToken(token);

        if (ethTransactionMessage == null) {
            throw new NotFoundException("ETH transaction not found");
        }

        return ethTransactionMessage;
    }
}
