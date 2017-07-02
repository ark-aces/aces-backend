package example.ark_smartbridge_listener.eth_bridge;

import org.springframework.stereotype.Service;

@Service
public class ContractMessageViewMapper {

    public ContractMessageView map(ContractMessage contractMessage) {
        ContractMessageView contractMessageView = new ContractMessageView();
        contractMessageView.setToken(contractMessage.getToken());

        contractMessageView.setServiceArkAddress(contractMessage.getServiceArkAddress());
        contractMessageView.setEstimatedGasCost(contractMessage.getEstimatedGasCost().toPlainString());
        contractMessageView.setEstimatedArkCost(contractMessage.getEstimatedArkCost().toPlainString());
        contractMessageView.setReturnArkAddress(contractMessage.getReturnArkAddress());

        if (contractMessage.getReturnArkAmount() != null) {
            contractMessageView.setReturnArkAmount(contractMessage.getReturnArkAmount().toPlainString());
        }
        if (contractMessage.getActualArkCost() != null) {
            contractMessageView.setActualArkCost(contractMessage.getActualArkCost().toPlainString());
        }

        contractMessageView.setContractTransactionHash(contractMessage.getContractTransactionHash());
        contractMessageView.setContractAddress(contractMessage.getContractAddress());

        contractMessageView.setCreatedAt(contractMessage.getCreatedAt().toString());

        return contractMessageView;
    }

}
