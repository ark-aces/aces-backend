package example.ark_smartbridge_listener;

import io.ark.ark_client.ArkClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ArkClientBackgroundRunner {

    private final ArkClient arkClient;

    // Update peers every hour
    @Scheduled(initialDelay = 3600000, fixedDelay = 3600000)
    public void updatePeers() {
        log.info("Updating Ark client peers");
        arkClient.updatePeers();
    }

}
