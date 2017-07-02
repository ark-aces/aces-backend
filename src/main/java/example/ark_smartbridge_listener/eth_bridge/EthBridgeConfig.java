package example.ark_smartbridge_listener.eth_bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class EthBridgeConfig {

    @Bean
    public String serviceArkAddress(Environment environment) {
        return environment.getProperty("arkBridge.serviceArkAddress");
    }

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return new ObjectMapper();
    }
}
