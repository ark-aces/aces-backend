package example.ark_smartbridge_listener.eth_bridge;

import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.TransactionMatch;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ContractController {

    private final ContractMessageRepository contractMessageRepository;
    private final ContractMessageViewMapper contractMessageViewMapper;

    // todo: externalize service ark address
    private final String serviceArkAddress = "target ark address";
    
    @PostMapping("/contracts")
    public ContractMessageView postContract(
        @RequestParam("returnArkAddress") String returnArkAddress,
        @RequestParam("code") MultipartFile file
    ) {
        String code = null;
        try {
            code = IOUtils.toString(file.getInputStream(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read code content as UTF-8");
        }

        // todo: we should compile the contract code and make sure it works before saving

        // todo: estimate ark cost
        BigDecimal estimatedArkCost = new BigDecimal("1.00000000");

        ContractMessage contractMessage = new ContractMessage();
        contractMessage.setToken(UUID.randomUUID().toString());
        contractMessage.setCode(code);
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
            new ByteArrayInputStream(contractMessage.getCode().getBytes(StandardCharsets.UTF_8))
        );

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .body(resource);
    }

    @PostMapping("/ark-transactions")
    public void postArkTransactions(@RequestBody TransactionMatch transactionMatch) {
        // todo execute ethereum contract and return un-used ark
        String smartContractCode = transactionMatch.getMessage().getData();
    }

    private ContractMessage getContractMessageOrThrowNotFound(String token) {
        ContractMessage contractMessage = contractMessageRepository.findOneByToken(token);
        if (contractMessage == null) {
            throw new NotFoundException("Contract not found");
        }
        return contractMessage;
    }
}
