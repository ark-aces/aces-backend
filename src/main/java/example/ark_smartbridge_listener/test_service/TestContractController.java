package example.ark_smartbridge_listener.test_service;

import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.ark_listener.CreateMessageRequest;
import example.ark_smartbridge_listener.ark_listener.TransactionMatch;
import example.ark_smartbridge_listener.eth_bridge.ContractMessage;
import io.ark.ark_client.ArkClient;
import io.ark.ark_client.Transaction;
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
public class TestContractController {

    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");
    private final BigInteger satoshisPerArk = new BigInteger("100000000");

    private final TestContractRepository testContractRepository;
    private final TestContractViewMapper testContractViewMapper;

    private final String serviceArkAddress;
    private final String serviceArkPassphrase;

    private final ArkClient arkClient;
    private final RetryTemplate arkClientRetryTemplate;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
            .rootUri("http://localhost:8080/")
            .build();

    @PostMapping("/test-contracts")
    public TestContractView createContract(
            @RequestParam("returnArkAddress") String returnArkAddress,
            @RequestParam("donationArkAmount") String donationArkAmountString
    ) {
        BigDecimal donationArkAmount = new BigDecimal(donationArkAmountString);

        TestContractEntity testContractEntity = new TestContractEntity();
        testContractEntity.setToken(UUID.randomUUID().toString());
        testContractEntity.setCreatedAt(ZonedDateTime.from(Instant.now().atOffset(ZoneOffset.UTC)));
        testContractEntity.setStatus(ContractMessage.STATUS_PENDING);
        testContractEntity.setServiceArkAddress(serviceArkAddress);
        testContractEntity.setDonationArkAmount(donationArkAmount.setScale(8, BigDecimal.ROUND_UP));
        testContractEntity.setReturnArkAddress(returnArkAddress);
        testContractRepository.save(testContractEntity);

        // Register contract message with listener so we get a callback when a matching ark transaction is found
        CreateMessageRequest createMessageRequest = new CreateMessageRequest();
        createMessageRequest.setCallbackUrl("http://localhost:8080/test-contracts/ark-transactions-matches");
        createMessageRequest.setToken(testContractEntity.getToken());
        listenerRestTemplate.postForObject("/messages", createMessageRequest, Void.class);

        return testContractViewMapper.map(testContractEntity);
    }

    @GetMapping("/test-contracts/{token}")
    public TestContractView getContract(@PathVariable String token) {
        TestContractEntity testContractEntity = getTestContractEntityOrThrowException(token);

        return testContractViewMapper.map(testContractEntity);
    }

    @PostMapping("/test-contracts/ark-transaction-matches")
    public void postArkTransactionMatch(@RequestBody TransactionMatch transactionMatch) {
        String token = transactionMatch.getToken();
        TestContractEntity testContractEntity = getTestContractEntityOrThrowException(token);

        // Skip already processed transactions
        if (! testContractEntity.getStatus().equals(ContractMessage.STATUS_PENDING)) {
            return;
        }

        Transaction transaction = arkClientRetryTemplate.execute(retryContext ->
                arkClient.getTransaction(transactionMatch.getArkTransactionId()));

        BigDecimal transactionArkAmount = new BigDecimal(transaction.getAmount())
                .setScale(14, BigDecimal.ROUND_UP)
                .divide(new BigDecimal(satoshisPerArk), BigDecimal.ROUND_UP);

        BigDecimal donationArkAmount = testContractEntity.getDonationArkAmount();
        BigDecimal requiredArkAmount = donationArkAmount.add(arkTransactionFee);

        // Ensure ark transaction contains enough ark to cover cost
        if (transactionArkAmount.compareTo(requiredArkAmount) >= 0) {
            testContractEntity.setStatus(ContractMessage.STATUS_COMPLETED);
        } else {
            // The ark transaction does not contain sufficient ark to process
            testContractEntity.setStatus(ContractMessage.STATUS_REJECTED);
        }

        BigDecimal returnArkAmount = transactionArkAmount
            .subtract(donationArkAmount)
            .subtract(arkTransactionFee);

        testContractEntity.setReturnArkAmount(returnArkAmount);

        if (returnArkAmount.compareTo(BigDecimal.ZERO) >= 0) {
            // Subtract ark transaction fees from return amount
            Long arkTransactionFeeSatoshis = arkTransactionFee
                .multiply(new BigDecimal(satoshisPerArk))
                .toBigIntegerExact().longValue();

            Long arkDonationSatoshis = testContractEntity.getDonationArkAmount()
                .multiply(new BigDecimal(satoshisPerArk))
                .toBigIntegerExact().longValue();

            Long returnSatoshiAmount = transaction.getAmount() - arkDonationSatoshis - arkTransactionFeeSatoshis;
            if (returnSatoshiAmount < 0) {
                returnSatoshiAmount = 0L;
            }

            if (returnSatoshiAmount > 0) {
                // todo: handle the case where an error occurs sending return ark.
                // Create return ark transaction with remaining ark
                Long finalReturnSatoshiAmount = returnSatoshiAmount;
                String returnArkTransactionId = arkClientRetryTemplate.execute(retryContext ->
                    arkClient.createTransaction(
                        testContractEntity.getReturnArkAddress(),
                        finalReturnSatoshiAmount,
                        testContractEntity.getToken(),
                        serviceArkPassphrase
                    ));

                testContractEntity.setReturnArkTransactionId(returnArkTransactionId);
            }
        }

        testContractRepository.save(testContractEntity);
    }

    private TestContractEntity getTestContractEntityOrThrowException(String token) {
        TestContractEntity testContractEntity = testContractRepository.findOneByToken(token);
        if (testContractEntity == null) {
            throw new NotFoundException("Contract not found");
        }
        return testContractEntity;
    }
}
