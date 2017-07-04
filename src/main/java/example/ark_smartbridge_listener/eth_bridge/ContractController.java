package example.ark_smartbridge_listener.eth_bridge;

import example.ark_smartbridge_listener.ark_listener.CreateMessageRequest;
import lib.IOUtilsWrapper;
import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.ark_listener.TransactionMatch;
import lib.ResponseEntityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ContractController {

    private final ContractMessageRepository contractMessageRepository;
    private final ContractMessageViewMapper contractMessageViewMapper;
    private final String serviceArkAddress;
    private final ScriptExecutorService scriptExecutorService;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
        .rootUri("http://localhost:8080/")
        .build();

    @PostMapping("/contracts")
    public ContractMessageView postContract(
        @RequestParam("abiJson") MultipartFile abiJsonFile,
        @RequestParam("code") MultipartFile codeFile,
        @RequestParam("params") MultipartFile paramsFile,
        @RequestParam("returnArkAddress") String returnArkAddress
    ) {
        String code = "0x" + IOUtilsWrapper.read(codeFile);
        String abiJson = IOUtilsWrapper.read(abiJsonFile);
        String paramsJson = IOUtilsWrapper.read(paramsFile);

        // todo: estimate ark cost
        Long estimatedGasCost = scriptExecutorService.executeEstimateGas(code).getGasEstimate();
        BigDecimal estimatedArkCost = new BigDecimal("1.00000000");

        ContractMessage contractMessage = new ContractMessage();
        contractMessage.setToken(UUID.randomUUID().toString());
        contractMessage.setContractCode(code);
        contractMessage.setContractAbiJson(abiJson);
        contractMessage.setContractParamsJson(paramsJson);
        contractMessage.setEstimatedGasCost(new BigDecimal(estimatedGasCost));
        contractMessage.setEstimatedArkCost(estimatedArkCost);
        contractMessage.setCreatedAt(ZonedDateTime.from(Instant.now().atOffset(ZoneOffset.UTC)));
        contractMessage.setServiceArkAddress(serviceArkAddress);
        contractMessage.setReturnArkAddress(returnArkAddress);
        contractMessageRepository.save(contractMessage);

        // Register contract message with listener so we get a callback when a matching ark transaction is found
        CreateMessageRequest createMessageRequest = new CreateMessageRequest();
        createMessageRequest.setCallbackUrl("http://localhost:8080/ark-transactions");
        createMessageRequest.setToken(contractMessage.getToken());
        listenerRestTemplate.postForObject("/messages", createMessageRequest, Void.class);

        return contractMessageViewMapper.map(contractMessage);
    }

    @GetMapping("/contracts/{token}")
    public ContractMessageView getContract(@PathVariable String token) {
        ContractMessage contractMessage = getContractMessageOrThrowNotFound(token);

        return contractMessageViewMapper.map(contractMessage);
    }

    @PostMapping("/ark-transactions")
    public void postArkTransactions(@RequestBody TransactionMatch transactionMatch) {
        String token = transactionMatch.getToken();
        ContractMessage contractMessage = getContractMessageOrThrowNotFound(token);

        // todo: estimate ark cost again. We shouldn't even try executing if ark given is too low
        String code = contractMessage.getContractCode();
        Long estimatedGasCost = scriptExecutorService.executeEstimateGas(code).getGasEstimate();
        BigDecimal estimatedArkCost = new BigDecimal("1.00000000");

        // deploy eth contract corresponding to this ark transaction
        // todo: an error may occur during execution of before persisting the data, we should figure
        // out a way to make contract deployment idempotent
        ContractDeployResult contractDeployResult = scriptExecutorService.executeContractDeploy(
            contractMessage.getContractAbiJson(),
            contractMessage.getContractCode(),
            contractMessage.getContractParamsJson()
        );

        // todo: we should figure out actual eth/ark costs and save to message

        contractMessage.setContractTransactionHash(contractDeployResult.getTransactionHash());
        contractMessage.setContractAddress(contractDeployResult.getAddress());
        contractMessageRepository.save(contractMessage);

        // todo: we should refund remaining ark to returnArkAddress with message token in Ark VendorField
    }

    private ContractMessage getContractMessageOrThrowNotFound(String token) {
        ContractMessage contractMessage = contractMessageRepository.findOneByToken(token);
        if (contractMessage == null) {
            throw new NotFoundException("Contract not found");
        }
        return contractMessage;
    }
}
