package example.ark_smartbridge_listener.eth_bridge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ScriptServiceInitializer {

    private final String scriptPath;

    @PostConstruct
    public void installScripts() {
        // We need to copy node script in java/main/resources/bin into scriptPath so they can
        // be executed by system commands.
        // todo: copy file contents over into scriptPath
    }

}
