package example.ark_smartbridge_listener.eth_bridge;

import example.ark_smartbridge_listener.IOUtilsWrapper;
import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.TransactionMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
    private final ScriptService scriptService;
    
    @PostMapping("/contracts")
    public ContractMessageView postContract(
        @RequestParam("abiJson") MultipartFile abiJsonFile,
        @RequestParam("code") MultipartFile codeFile,
        @RequestParam("params") MultipartFile paramsFile,
        @RequestParam("returnArkAddress") String returnArkAddress
    ) {
        String code = IOUtilsWrapper.read(codeFile);
        String abiJson = IOUtilsWrapper.read(abiJsonFile);
        String paramsJson = IOUtilsWrapper.read(paramsFile);

        // todo: estimate ark cost
        Long estimatedGasCost = scriptService.executeEstimateGas(code);
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

        return contractMessageViewMapper.map(contractMessage);
    }

    @GetMapping("/contracts/{token}")
    public ContractMessageView getContract(@PathVariable String token) {
        ContractMessage contractMessage = getContractMessageOrThrowNotFound(token);

        return contractMessageViewMapper.map(contractMessage);
    }

    @GetMapping("/contracts/{token}/code")
    public ResponseEntity<Resource> getContractCode(@PathVariable String token) {
        ContractMessage contractMessage = getContractMessageOrThrowNotFound(token);

        InputStreamResource resource = new InputStreamResource(
            new ByteArrayInputStream(contractMessage.getContractCode().getBytes(StandardCharsets.UTF_8))
        );

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    @GetMapping("/contracts/{token}/abi")
    public ResponseEntity<Resource> getContractAbi(@PathVariable String token) {
        ContractMessage contractMessage = getContractMessageOrThrowNotFound(token);

        InputStreamResource resource = new InputStreamResource(
            new ByteArrayInputStream(contractMessage.getContractAbiJson().getBytes(StandardCharsets.UTF_8))
        );

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(resource);
    }

    @GetMapping("/contracts/{token}/params")
    public ResponseEntity<Resource> getContractParams(@PathVariable String token) {
        ContractMessage contractMessage = getContractMessageOrThrowNotFound(token);

        InputStreamResource resource = new InputStreamResource(
            new ByteArrayInputStream(contractMessage.getContractParamsJson().getBytes(StandardCharsets.UTF_8))
        );

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(resource);
    }

    @PostMapping("/ark-transactions")
    public void postArkTransactions(@RequestBody TransactionMatch transactionMatch) {
        String token = transactionMatch.getMessage().getToken();
        ContractMessage contractMessage = getContractMessageOrThrowNotFound(token);

        // todo: estimate ark cost again. We shouldn't even try executing if ark given is too low
        String code = contractMessage.getContractCode();
        Long estimatedGasCost = scriptService.executeEstimateGas(code);
        BigDecimal estimatedArkCost = new BigDecimal("1.00000000");

        // deploy eth contract corresponding to this ark transaction
        // todo: an error may occur during execution of before persisting the data, we should figure
        // out a way to make contract deployment idempotent
        ContractDeployResult contractDeployResult = scriptService.executeContractDeploy(
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
