package example.ark_smartbridge_listener.eth_bridge;

import org.springframework.stereotype.Service;

@Service
public class ContractMessageViewMapper {

    public ContractMessageView map(ContractMessage contractMessage) {
        ContractMessageView contractMessageView = new ContractMessageView();
        contractMessageView.setToken(contractMessage.getToken());

        contractMessageView.setServiceArkAddress(contractMessage.getServiceArkAddress());
        contractMessageView.setEstimatedArkCost(contractMessage.getEstimatedArkCost().toPlainString());

        contractMessageView.setReturnArkAddress(contractMessage.getReturnArkAddress());
        contractMessageView.setReturnArkAmount(contractMessage.getReturnArkAmount().toPlainString());
        contractMessageView.setActualArkCost(contractMessage.getActualArkCost().toPlainString());

        contractMessageView.setEthContractAddress(contractMessage.getEthContractAddress());

        contractMessageView.setCreatedAt(contractMessage.getCreatedAt().toString());

        return contractMessageView;
    }

}
