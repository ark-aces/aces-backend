package example.ark_smartbridge_listener.test_service;

import example.ark_smartbridge_listener.ArkService;
import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.ServiceInfoView;
import example.ark_smartbridge_listener.ark_listener.CreateMessageRequest;
import example.ark_smartbridge_listener.ark_listener.TransactionMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@CrossOrigin
@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class TestContractController {

    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");

    private final TestContractRepository testContractRepository;
    private final TestContractViewMapper testContractViewMapper;
    private final String serviceArkAddress;
    private final ArkService arkService;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
            .rootUri("http://localhost:8080/")
            .build();

    @GetMapping("/test-service-info")
    public ServiceInfoView getServiceInfo() {
        ServiceInfoView serviceInfoView = new ServiceInfoView();
        serviceInfoView.setCapacity("∞");
        serviceInfoView.setFlatFeeArk("0");
        serviceInfoView.setPercentFee("0");
        serviceInfoView.setStatus(ServiceInfoView.STATUS_UP);
        return serviceInfoView;
    }

    @PostMapping("/test-contracts")
    public TestContractView createContract(
            @RequestParam("returnArkAddress") String returnArkAddress,
            @RequestParam("donationArkAmount") String donationArkAmount
    ) {
        BigDecimal donationArkAmountValue = new BigDecimal(donationArkAmount);

        BigDecimal requiredArkAmount = donationArkAmountValue.add(arkTransactionFee);

        TestContractEntity testContractEntity = new TestContractEntity();
        testContractEntity.setToken(UUID.randomUUID().toString());
        testContractEntity.setCreatedAt(ZonedDateTime.from(Instant.now().atOffset(ZoneOffset.UTC)));
        testContractEntity.setStatus(TestContractEntity.STATUS_PENDING);
        testContractEntity.setServiceArkAddress(serviceArkAddress);
        testContractEntity.setDonationArkAmount(donationArkAmountValue.setScale(8, BigDecimal.ROUND_UP));
        testContractEntity.setReturnArkAddress(returnArkAddress);
        testContractEntity.setRequiredArkAmount(requiredArkAmount);
        testContractRepository.save(testContractEntity);

        // Register contract message with listener so we get a callback when a matching ark transaction is found
        CreateMessageRequest createMessageRequest = new CreateMessageRequest();
        createMessageRequest.setCallbackUrl("http://localhost:8080/test-contracts/ark-transaction-matches");
        createMessageRequest.setToken(testContractEntity.getToken());
        listenerRestTemplate.postForObject("/messages", createMessageRequest, Void.class);

        return testContractViewMapper.map(testContractEntity);
    }

    @GetMapping("/test-contracts/{token}")
    public TestContractView getContract(@PathVariable String token) {
        TestContractEntity testContractEntity = getContractEntityOrThrowException(token);

        return testContractViewMapper.map(testContractEntity);
    }

    @PostMapping("/test-contracts/ark-transaction-matches")
    public void postArkTransactionMatch(@RequestBody TransactionMatch transactionMatch) {
        TestContractEntity contractEntity = getContractEntityOrThrowException(transactionMatch.getToken());

        // Skip already processed transactions
        if (! contractEntity.getStatus().equals(TestContractEntity.STATUS_PENDING)) {
            return;
        }

        BigDecimal sentArkAmount = arkService.getArkAmount(transactionMatch.getArkTransactionId());

        // Ensure ark transaction contains enough ark to cover cost
        BigDecimal usedArkAmount;
        if (sentArkAmount.compareTo(contractEntity.getRequiredArkAmount()) >= 0) {
            usedArkAmount = contractEntity.getDonationArkAmount()
                .add(arkTransactionFee);
            contractEntity.setStatus(TestContractEntity.STATUS_COMPLETED);
        } else {
            usedArkAmount = BigDecimal.ZERO;
            contractEntity.setStatus(TestContractEntity.STATUS_REJECTED);
        }

        BigDecimal returnArkAmount = arkService.calculateReturnArkAmount(sentArkAmount, usedArkAmount);
        contractEntity.setReturnArkAmount(returnArkAmount);

        if (returnArkAmount.compareTo(BigDecimal.ZERO) > 0) {
            String returnArkTransactionId = arkService
                .sendArk(contractEntity.getReturnArkAddress(), returnArkAmount, contractEntity.getToken());
            contractEntity.setReturnArkTransactionId(returnArkTransactionId);
        }

        testContractRepository.save(contractEntity);
    }

    private TestContractEntity getContractEntityOrThrowException(String token) {
        TestContractEntity testContractEntity = testContractRepository.findOneByToken(token);
        if (testContractEntity == null) {
            throw new NotFoundException("Contract not found");
        }
        return testContractEntity;
    }
}
