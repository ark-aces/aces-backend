package example.ark_smartbridge_listener.eth_bridge;

import lombok.Data;

@Data
public class ContractMessageView {

    private String token;
    private String createdAt;
    private String status;

    private String serviceArkAddress;
    private String arkPerEthExchangeRate;
    private Long estimatedGasCost;
    private String estimatedEthCost;
    private String requiredArkCost;

    private String returnArkAddress;
    private String deploymentArkCost;
    private String returnArkAmount;
    private String returnArkTransactionId;

    private String contractCode;
    private String contractAbiJson;
    private String contractParamsJson;
    private Long gasLimit;

    private String contractTransactionHash;
    private String contractAddress;
    private Long gasUsed;
}
