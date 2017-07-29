package example.ark_smartbridge_listener.eth_bridge;

import org.springframework.stereotype.Service;

@Service
public class ContractMessageViewMapper {

    public ContractMessageView map(ContractMessage contractMessage) {
        ContractMessageView contractMessageView = new ContractMessageView();
        contractMessageView.setToken(contractMessage.getToken());

        contractMessageView.setServiceArkAddress(contractMessage.getServiceArkAddress());
        contractMessageView.setEstimatedGasCost(contractMessage.getEstimatedGasCost());
        contractMessageView.setEstimatedEthCost(contractMessage.getEstimatedEthCost().toPlainString());
        contractMessageView.setArkPerEthExchangeRate(contractMessage.getArkPerEthExchangeRate().toPlainString());
        contractMessageView.setRequiredArkCost(contractMessage.getRequiredArkCost().toPlainString());
        contractMessageView.setReturnArkAddress(contractMessage.getReturnArkAddress());

        contractMessageView.setContractCode(contractMessage.getContractCode());
        contractMessageView.setContractAbiJson(contractMessage.getContractAbiJson());
        contractMessageView.setContractParamsJson(contractMessage.getContractParamsJson());
        contractMessageView.setGasLimit(contractMessage.getGasLimit());

        if (contractMessage.getReturnArkAmount() != null) {
            contractMessageView.setReturnArkAmount(contractMessage.getReturnArkAmount().toPlainString());
        }
        if (contractMessage.getDeploymentArkCost() != null) {
            contractMessageView.setActualArkCost(contractMessage.getDeploymentArkCost().toPlainString());
        }

        contractMessageView.setContractTransactionHash(contractMessage.getContractTransactionHash());
        contractMessageView.setContractAddress(contractMessage.getContractAddress());
        contractMessageView.setGasUsed(contractMessage.getGasUsed());

        contractMessageView.setCreatedAt(contractMessage.getCreatedAt().toString());

        return contractMessageView;
    }

}
