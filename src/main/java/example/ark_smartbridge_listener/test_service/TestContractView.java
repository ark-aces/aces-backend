package example.ark_smartbridge_listener.test_service;

import lombok.Data;

@Data
public class TestContractView {
    private String token;
    private String createdAt;
    private String status;
    private String requiredArkAmount;
    private String serviceArkAddress;
    private String donationArkAmount;
    private String returnArkAddress;
    private String returnArkAmount;
    private String returnArkTransactionId;
}
