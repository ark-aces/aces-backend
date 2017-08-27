package example.ark_smartbridge_listener.eth_contract_deploy;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Entity
public class EthContractDeployContractEntity {

    /**
     * Contract is waiting for Ark Transaction to be found
     */
    public static final String STATUS_PENDING = "pending";

    /**
     * Contract was rejected because of insufficient ark
     */
    public static final String STATUS_REJECTED = "rejected";

    /**
     * Failure occurred trying to create Ethereum contract
     */
    public static final String STATUS_FAILED = "failed";

    /**
     * Contract successfully created on Ethereum
     */
    public static final String STATUS_COMPLETED = "completed";

    @Id
    @GeneratedValue
    private Long id;

    private String token;

    @Column(columnDefinition="TEXT")
    private String contractAbiJson;

    @Column(columnDefinition="TEXT")
    private String contractCode;

    @Column(columnDefinition = "TEXT")
    private String contractParamsJson;

    private Long gasLimit;

    private String status;

    @Column(precision = 20, scale = 8)
    private BigDecimal arkPerEthExchangeRate;

    private Long estimatedGasCost;

    @Column(precision = 20, scale = 8)
    private BigDecimal estimatedEthCost;

    @Column(precision = 20, scale = 8)
    private BigDecimal arkFlatFee;

    @Column(precision = 20, scale = 8)
    private BigDecimal arkFeePercent;

    @Column(precision = 20, scale = 8)
    private BigDecimal arkFeeTotal;

    @Column(precision = 20, scale = 8)
    private BigDecimal requiredArkCost;

    private String serviceArkAddress;

    private String returnArkAddress;

    @Column(precision = 20, scale = 8)
    private BigDecimal deploymentArkCost;

    @Column(precision = 20, scale = 8)
    private BigDecimal returnArkAmount;

    private String returnArkTransactionId;

    private String contractTransactionHash;

    private String contractAddress;

    private Long gasUsed;

    private ZonedDateTime createdAt;
}
