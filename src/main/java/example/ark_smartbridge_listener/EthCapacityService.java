package example.ark_smartbridge_listener;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EthCapacityService {

    private final ConcurrentHashMap<String, BigDecimal> serviceCapacityCache;
    private final EthBalanceScriptExecutor ethBalanceScriptExecutor;

    public BigDecimal getBalance() {
        BigDecimal balance;
        if (serviceCapacityCache.containsKey("eth")) {
            balance = serviceCapacityCache.get("eth");
        } else {
            GetBalanceResult getBalanceResult = ethBalanceScriptExecutor.execute();
            balance = getBalanceResult.getBalance();
            serviceCapacityCache.put("eth", balance);
        }
        return balance;
    }

}
