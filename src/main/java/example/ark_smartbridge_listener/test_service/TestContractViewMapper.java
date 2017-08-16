package example.ark_smartbridge_listener.test_service;

import org.springframework.stereotype.Service;

@Service
public class TestContractViewMapper {

    public TestContractView map(TestContractEntity testContractEntity) {
        TestContractView testContractView = new TestContractView();
        testContractView.setToken(testContractEntity.getToken());
        testContractView.setCreatedAt(testContractEntity.getCreatedAt().toString());
        testContractView.setStatus(testContractEntity.getStatus());
        testContractView.setRequiredArkAmount(testContractEntity.getRequiredArkAmount().toPlainString());
        testContractView.setServiceArkAddress(testContractEntity.getServiceArkAddress());
        testContractView.setReturnArkAddress(testContractEntity.getReturnArkAddress());
        testContractView.setDonationArkAmount(testContractEntity.getDonationArkAmount().toPlainString());

        if (testContractEntity.getReturnArkAmount() != null) {
            testContractView.setReturnArkAmount(testContractEntity.getReturnArkAmount().toPlainString());
        }
        testContractView.setReturnArkTransactionId(testContractEntity.getReturnArkTransactionId());

        return testContractView;
    }
}
