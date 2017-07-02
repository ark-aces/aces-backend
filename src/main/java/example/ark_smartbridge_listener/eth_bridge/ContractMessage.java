package example.ark_smartbridge_listener.eth_bridge;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Entity
public class ContractMessage {

    @Id
    @GeneratedValue
    private Long id;

    private String token;

    @Column(columnDefinition="TEXT")
    private String code;

    private BigDecimal estimatedArkCost;

    private String serviceArkAddress;

    private String returnArkAddress;

    private BigDecimal actualArkCost;

    private BigDecimal returnArkAmount;

    private String ethContractAddress;

    private ZonedDateTime createdAt;
}
