package example.ark_smartbridge_listener.eth_transfer;

import org.springframework.stereotype.Service;

@Service
public class EthTransferContractViewMapper {

    public EthTransferContractView map(EthTransferContractEntity ethTransferContractEntity) {
        EthTransferContractView ethTransferContractView = new EthTransferContractView();

        ethTransferContractView.setToken(ethTransferContractEntity.getToken());
        ethTransferContractView.setCreatedAt(ethTransferContractEntity.getCreatedAt().toString());
        ethTransferContractView.setStatus(ethTransferContractEntity.getStatus());
        ethTransferContractView.setArkPerEthExchangeRate(ethTransferContractEntity.getArkPerEthExchangeRate().toPlainString());
        ethTransferContractView.setServiceArkAddress(ethTransferContractEntity.getServiceArkAddress());
        ethTransferContractView.setRequiredArkAmount(ethTransferContractEntity.getRequiredArkAmount().toPlainString());
        ethTransferContractView.setReturnArkAddress(ethTransferContractEntity.getReturnArkAddress());
        if (ethTransferContractEntity.getReturnArkAmount() != null) {
            ethTransferContractView.setReturnArkAmount(ethTransferContractEntity.getReturnArkAmount().toPlainString());
        }
        ethTransferContractView.setReturnArkTransactionId(ethTransferContractEntity.getReturnArkTransactionId());
        ethTransferContractView.setRecipientEthAddress(ethTransferContractEntity.getRecipientEthAddress());
        ethTransferContractView.setEthAmount(ethTransferContractEntity.getEthAmount().toPlainString());
        ethTransferContractView.setEthTransactionHash(ethTransferContractEntity.getEthTransactionHash());

        return ethTransferContractView;
    }
}
