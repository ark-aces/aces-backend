package example.ark_smartbridge_listener.eth_contract_deploy;

import lombok.Data;

@Data
public class EthContractDeployContractView {

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
    private String arkFlatFee;
    private String arkFeePercent;
    private String arkFeeTotal;
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
