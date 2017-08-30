package example.ark_smartbridge_listener;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.ark.ark_client.ArkClient;
import io.ark.ark_client.ArkNetwork;
import io.ark.ark_client.ArkNetworkFactory;
import io.ark.ark_client.HttpArkClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableScheduling
public class ApplicationConfig {
    
    @Bean
    public ArkClient arkClient(Environment environment) {
        // todo: we should probably just network config in json format so it can directly consume ark-node configs
        String arkNetworkName = environment.getProperty("arkNetwork.name");
        ArkNetwork arkNetwork = new ArkNetworkFactory()
            .createFromYml("ark-config/" + arkNetworkName + ".yml");
        
        RestTemplate restTemplate = new RestTemplateBuilder().build();

        return new HttpArkClient(arkNetwork, restTemplate);
    }
    
    @Bean
    public RestTemplate callbackRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Jackson2ObjectMapperBuilder objectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.indentOutput(true);
        return builder;
    }

    @Bean
    public ConcurrentHashMap<String, BigDecimal> serviceCapacityCache() {
        return new ConcurrentHashMap<>();
    }
}
