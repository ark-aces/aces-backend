package example.ark_smartbridge_listener.ark_listener;

import io.ark.ark_client.ArkClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
@Log4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ArkTransactionListener {

    // Number of Transactions to scan through each execution cycle
    private final Integer scanDepthTransactions = 500;

    private final ArkClient arkClient;
    private final MessageRepository messageRepository;
    private final RestTemplate callbackRestTemplate;

    /**
     * Run every scan in the background, with 10 seconds between scans.
     */
//    @Scheduled(fixedDelay = 10000)
    public void scanTransactions() {
        try {
            // todo: review this scanning so that we don't miss any transactions
            Integer limit = 50;
            for (Integer offset = 0; offset < scanDepthTransactions; offset += limit) {
                log.info("Scanning transactions with offset = " + offset);
                arkClient.getTransactions(offset).parallelStream()
                    .forEach(transaction -> {
                        // Skip transaction with empty vendorField
                        if (StringUtils.isEmpty(transaction.getVendorField())) {
                            return;
                        }

                        log.info("Found ark transaction with vendor field: " + transaction.getVendorField());
                        Message message = messageRepository.findOneByToken(transaction.getVendorField());
                        if (message != null) {
                            log.info("Matched transaction with message token: " + message.getToken());
                            // We got a match! Send it to the corresponding message listener
                            TransactionMatch transactionMatch = new TransactionMatch(transaction.getId(), message.getToken());
                            try {
                                log.info("Posting to message to callback url " + message.getCallbackUrl() + ": " + message);
                                callbackRestTemplate.postForEntity(message.getCallbackUrl(), transactionMatch, Void.class);
                            } catch (RestClientResponseException e) {
                                log.error("Failed to post to callback url: " + message.getCallbackUrl(), e);
                                throw e;
                            }
                        }
                    });
            }
        }
        catch (Exception e) {
            log.error("Transaction listener threw exception while running", e);   
        }
    }
    
}
