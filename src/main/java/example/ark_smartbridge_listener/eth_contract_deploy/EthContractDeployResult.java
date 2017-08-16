package example.ark_smartbridge_listener.eth_contract_deploy;

import lombok.Data;

@Data
public class EthContractDeployResult {
    private String transactionHash;
    private String address;
    private Long gasUsed;
}
