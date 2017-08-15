package example.ark_smartbridge_listener.eth_bridge;

import org.springframework.stereotype.Service;

@Service
public class EthTransactionMessageViewMapper {

    public EthTransactionMessageView map(EthTransactionMessage ethTransactionMessage) {
        EthTransactionMessageView ethTransactionMessageView = new EthTransactionMessageView();

        ethTransactionMessageView.setToken(ethTransactionMessage.getToken());
        ethTransactionMessageView.setCreatedAt(ethTransactionMessage.getCreatedAt().toString());
        ethTransactionMessageView.setStatus(ethTransactionMessage.getStatus());
        ethTransactionMessageView.setServiceArkAddress(ethTransactionMessage.getServiceArkAddress());
        ethTransactionMessageView.setRequiredArkAmount(ethTransactionMessage.getRequiredArkAmount().toPlainString());
        ethTransactionMessageView.setReturnArkAddress(ethTransactionMessage.getReturnArkAddress());
        ethTransactionMessageView.setRecipientEthAddress(ethTransactionMessage.getRecipientEthAddress());
        ethTransactionMessageView.setEthAmount(ethTransactionMessage.getEthAmount().toPlainString());

        return ethTransactionMessageView;
    }
}
