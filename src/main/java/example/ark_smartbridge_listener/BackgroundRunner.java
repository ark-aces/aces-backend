package example.ark_smartbridge_listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class BackgroundRunner {

    private final ConcurrentHashMap<String, BigDecimal> serviceCapacityCache;
    private final EthBalanceScriptExecutor ethBalanceScriptExecutor;

    @Scheduled(fixedDelay = 10000)
    public void updateServiceCapacities() {
        GetBalanceResult getBalanceResult = ethBalanceScriptExecutor.execute();
        log.info("updating eth balance cache to " + getBalanceResult.getBalance().toPlainString());
        serviceCapacityCache.put("eth", getBalanceResult.getBalance());
    }

}
