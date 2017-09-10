package example.ark_smartbridge_listener.eth_transfer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "ethTransferService")
public class EthTransferConfig {
    private BigDecimal arkFlatFee;
    private BigDecimal arkPercentFee;
    private BigDecimal ethPerGas;
    private BigDecimal requiredArkMultiplier;
}
