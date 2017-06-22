package example.ark_smartbridge_listener;

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
    
    private final ArkClient arkClient;
    private final MessageRepository messageRepository;
    private final RestTemplate callbackRestTemplate;

    /**
     * Run every scan in the background, with 5 seconds between scans.
     */
    @Scheduled(fixedDelay = 5000)
    public void scanTransactions() {
        try {
            // todo: this is kind of dumb because it scans the whole transaction history
            // each cycle. It's good enough for a proof-of-concept though.
            // It should track the last processed transaction and scan from there instead.
            arkClient.getTransactions().parallelStream().forEach(transaction -> {
                // Skip transaction with empty vendorField
                if (StringUtils.isEmpty(transaction.getVendorField())) {
                    return;
                }
                
                Message message = messageRepository.findOneByToken(transaction.getVendorField());
                if (message != null) {
                    // We got a match! Send it to the corresponding message listener
                    TransactionMatch transactionMatch = new TransactionMatch(transaction.getId(), message);
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
        catch (Exception e) {
            log.error("Transaction listener threw exception while running", e);   
        }
    }
    
}
