package example.ark_smartbridge_listener.eth_bridge;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

@Log4j
@Service
public class ContractMessageViewMapper {

    public ContractMessageView map(ContractMessage contractMessage) {
        ContractMessageView contractMessageView = new ContractMessageView();
        contractMessageView.setToken(contractMessage.getToken());
        contractMessageView.setStatus(contractMessage.getStatus());

        contractMessageView.setServiceArkAddress(contractMessage.getServiceArkAddress());
        contractMessageView.setEstimatedGasCost(contractMessage.getEstimatedGasCost());
        contractMessageView.setEstimatedEthCost(contractMessage.getEstimatedEthCost().toPlainString());
        contractMessageView.setArkPerEthExchangeRate(contractMessage.getArkPerEthExchangeRate().toPlainString());
        contractMessageView.setRequiredArkCost(contractMessage.getRequiredArkCost().toPlainString());
        contractMessageView.setReturnArkAddress(contractMessage.getReturnArkAddress());
        contractMessageView.setReturnArkTransactionId(contractMessage.getReturnArkTransactionId());

        contractMessageView.setContractCode(contractMessage.getContractCode());
        contractMessageView.setContractAbiJson(contractMessage.getContractAbiJson());
        contractMessageView.setContractParamsJson(contractMessage.getContractParamsJson());
        contractMessageView.setGasLimit(contractMessage.getGasLimit());


        if (contractMessage.getReturnArkAmount() != null) {
            contractMessageView.setReturnArkAmount(contractMessage.getReturnArkAmount().toPlainString());
        }
        if (contractMessage.getDeploymentArkCost() != null) {
            contractMessageView.setDeploymentArkCost(contractMessage.getDeploymentArkCost().toPlainString());
        }

        contractMessageView.setContractTransactionHash(contractMessage.getContractTransactionHash());
        contractMessageView.setContractAddress(contractMessage.getContractAddress());
        contractMessageView.setGasUsed(contractMessage.getGasUsed());

        contractMessageView.setCreatedAt(contractMessage.getCreatedAt().toString());

        log.info("contract message: " + contractMessage.toString());
        log.info("contract message view: " + contractMessageView.toString());

        return contractMessageView;
    }

}
