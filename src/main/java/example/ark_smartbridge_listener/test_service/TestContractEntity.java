package example.ark_smartbridge_listener.test_service;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Entity
public class TestContractEntity {

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

    private String status;

    private ZonedDateTime createdAt;

    @Column(precision = 20, scale = 8)
    private BigDecimal donationArkAmount;

    @Column(precision = 20, scale = 8)
    private BigDecimal requiredArkAmount;

    private String serviceArkAddress;

    private String returnArkAddress;

    @Column(precision = 20, scale = 8)
    private BigDecimal returnArkAmount;

    private String returnArkTransactionId;
}
