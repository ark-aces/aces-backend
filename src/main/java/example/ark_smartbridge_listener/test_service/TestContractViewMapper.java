package example.ark_smartbridge_listener.test_service;

import org.springframework.stereotype.Service;

@Service
public class TestContractViewMapper {

    public TestContractView map(TestContractEntity testContractEntity) {
        TestContractView ethTransactionMessageView = new TestContractView();
        ethTransactionMessageView.setToken(testContractEntity.getToken());
        ethTransactionMessageView.setCreatedAt(testContractEntity.getCreatedAt().toString());
        ethTransactionMessageView.setStatus(testContractEntity.getStatus());
        ethTransactionMessageView.setRequiredArkAmount(testContractEntity.getRequiredArkAmount().toPlainString());
        ethTransactionMessageView.setServiceArkAddress(testContractEntity.getServiceArkAddress());
        ethTransactionMessageView.setReturnArkAddress(testContractEntity.getReturnArkAddress());
        ethTransactionMessageView.setDonationArkAmount(testContractEntity.getDonationArkAmount().toPlainString());
        ethTransactionMessageView.setReturnArkAmount(testContractEntity.getReturnArkAmount().toPlainString());
        ethTransactionMessageView.setReturnArkTransactionId(testContractEntity.getReturnArkTransactionId());

        return ethTransactionMessageView;
    }
}
