package example.ark_smartbridge_listener;

import io.ark.ark_client.ArkClient;
import io.ark.ark_client.ArkNetwork;
import io.ark.ark_client.ArkNetworkFactory;
import io.ark.ark_client.HttpArkClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfig {
    
    @Bean
    public ArkClient arkClient(Environment environment) {
        String arkNetworkName = environment.getProperty("arkNetwork.name");
        ArkNetwork arkNetwork = new ArkNetworkFactory().createFromYml("ark-config/" + arkNetworkName + ".yml");
        
        RestTemplate restTemplate = new RestTemplateBuilder()
            .build();
        
        String scheme = environment.getProperty("arkNetwork.scheme");
            
        return new HttpArkClient(scheme, arkNetwork, restTemplate);
    }
    
    @Bean
    public RestTemplate callbackRestTemplate() {
        return new RestTemplate();
    }
}
