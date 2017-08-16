package example.ark_smartbridge_listener.eth_contract_deploy;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

@Log4j
@Service
public class EthContractDeployContractViewMapper {

    public EthContractDeployContractView map(EthContractDeployContractEntity entity) {
        EthContractDeployContractView view = new EthContractDeployContractView();
        view.setToken(entity.getToken());
        view.setStatus(entity.getStatus());

        view.setServiceArkAddress(entity.getServiceArkAddress());
        view.setEstimatedGasCost(entity.getEstimatedGasCost());
        view.setEstimatedEthCost(entity.getEstimatedEthCost().toPlainString());
        view.setArkPerEthExchangeRate(entity.getArkPerEthExchangeRate().toPlainString());
        view.setRequiredArkCost(entity.getRequiredArkCost().toPlainString());
        view.setReturnArkAddress(entity.getReturnArkAddress());
        view.setReturnArkTransactionId(entity.getReturnArkTransactionId());

        view.setContractCode(entity.getContractCode());
        view.setContractAbiJson(entity.getContractAbiJson());
        view.setContractParamsJson(entity.getContractParamsJson());
        view.setGasLimit(entity.getGasLimit());

        if (entity.getReturnArkAmount() != null) {
            view.setReturnArkAmount(entity.getReturnArkAmount().toPlainString());
        }
        if (entity.getDeploymentArkCost() != null) {
            view.setDeploymentArkCost(entity.getDeploymentArkCost().toPlainString());
        }

        view.setContractTransactionHash(entity.getContractTransactionHash());
        view.setContractAddress(entity.getContractAddress());
        view.setGasUsed(entity.getGasUsed());

        view.setCreatedAt(entity.getCreatedAt().toString());

        return view;
    }

}
