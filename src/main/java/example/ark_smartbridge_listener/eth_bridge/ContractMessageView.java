package example.ark_smartbridge_listener.eth_bridge;

import lombok.Data;

@Data
public class ContractMessageView {

    private String token;
    private String createdAt;

    private String serviceArkAddress;
    private String estimatedGasCost;
    private String estimatedArkCost;

    private String returnArkAddress;
    private String actualArkCost;
    private String returnArkAmount;
    private String returnArkTransactionId;

    private String contractTransactionHash;
    private String contractAddress;
}
