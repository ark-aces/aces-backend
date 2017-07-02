package example.ark_smartbridge_listener.eth_bridge;

import lombok.Data;

@Data
public class ContractDeployResult {
    private String transactionHash;
    private String address;
}
