package example.ark_smartbridge_listener.eth_bridge;

import lombok.Data;

@Data
public class ContractMessageView {

    private String token;
    private String createdAt;

    private String serviceArkAddress;
    private String estimatedGasCost;
    private String estimatedEthCost;
    private String requiredArkCost;

    private String returnArkAddress;
    private String actualArkCost;
    private String returnArkAmount;
    private String returnArkTransactionId;

    private String contractCode;
    private String contractAbiJson;
    private String contractParamsJson;

    private String contractTransactionHash;
    private String contractAddress;
}
