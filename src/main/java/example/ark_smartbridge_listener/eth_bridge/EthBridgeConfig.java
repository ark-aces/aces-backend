package example.ark_smartbridge_listener.eth_bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import lib.NiceObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class EthBridgeConfig {

    @Bean
    public String serviceArkAddress(Environment environment) {
        return environment.getProperty("ethBridge.serviceArkWallet.address");
    }

    @Bean
    public String serviceArkPassphrase(Environment environment) {
        return environment.getProperty("ethBridge.serviceArkWallet.passphrase");
    }

    @Bean
    public String serviceEthAccountAddress(Environment environment) {
        return environment.getProperty("ethBridge.serviceEthAccount.address");
    }

    @Bean
    public String serviceEthAccountPassword(Environment environment) {
        return environment.getProperty("ethBridge.serviceEthAccount.password");
    }

    @Bean
    public String scriptPath(Environment environment) {
        return environment.getProperty("ethBridge.scriptPath");
    }

    @Bean
    public String nodeCommand(Environment environment) {
        return environment.getProperty("ethBridge.nodeCommand");
    }

    @Bean
    public String ethServerUrl(Environment environment) {
        return environment.getProperty("ethBridge.ethServerUrl");
    }

    @Bean
    public NiceObjectMapper defaultObjectMapper() {
        return new NiceObjectMapper(new ObjectMapper());
    }
}
