package example.ark_smartbridge_listener;

import io.ark.ark_client.ArkClient;
import io.ark.ark_client.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ArkService {

    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");
    private final BigInteger satoshisPerArk = new BigInteger("100000000");

    private final RetryTemplate arkClientRetryTemplate;
    private final ArkClient arkClient;
    private final String serviceArkPassphrase;

    public String sendArk(String address, BigDecimal arkAmount, String vendorField) {
        // todo: handle the case where an error occurs sending return ark.
        final Long satoshiAmount = arkAmount
            .multiply(new BigDecimal(satoshisPerArk))
            .toBigIntegerExact()
            .longValue();

        log.info("Sending ark transaction to " + address + " for " + arkAmount + " ark");

        return arkClientRetryTemplate.execute(retryContext ->
            arkClient.createTransaction(address, satoshiAmount, vendorField, serviceArkPassphrase)
        );
    }

    public BigDecimal getArkAmount(String transactionId) {
        Transaction transaction = arkClientRetryTemplate.execute(retryContext ->
            arkClient.getTransaction(transactionId)
        );

        BigDecimal arkAmount = new BigDecimal(transaction.getAmount())
            .setScale(14, BigDecimal.ROUND_UP)
            .divide(new BigDecimal(satoshisPerArk), BigDecimal.ROUND_UP);

        log.info("Fetched ark transaction " + transactionId + " with " + arkAmount + " ark");

        return arkAmount;
    }

    public BigDecimal calculateReturnArkAmount(BigDecimal sentArkAmount, BigDecimal usedArkAmount) {
        BigDecimal delta = sentArkAmount.subtract(usedArkAmount);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            return delta;
        } else {
            return BigDecimal.ZERO;
        }
    }
}
