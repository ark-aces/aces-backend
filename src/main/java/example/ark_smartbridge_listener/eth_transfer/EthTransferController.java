package example.ark_smartbridge_listener.eth_transfer;

import example.ark_smartbridge_listener.ArkService;
import example.ark_smartbridge_listener.EthBalanceScriptExecutor;
import example.ark_smartbridge_listener.GetBalanceResult;
import example.ark_smartbridge_listener.NotFoundException;
import example.ark_smartbridge_listener.ScriptExecutorService;
import example.ark_smartbridge_listener.ServiceInfoView;
import example.ark_smartbridge_listener.ark_listener.CreateMessageRequest;
import example.ark_smartbridge_listener.ark_listener.TransactionMatch;
import example.ark_smartbridge_listener.eth_contract_deploy.EthContractDeployContractEntity;
import example.ark_smartbridge_listener.exchange_rate_service.ExchangeRateService;
import io.ark.ark_client.EthTransactionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
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
public class EthTransferController {

    private final BigDecimal arkFlatFee = new BigDecimal("1.0000000");
    private final BigDecimal arkFeePercent = new BigDecimal("1.25000000");
    private final BigDecimal arkTransactionFee = new BigDecimal("0.10000000");

    private final EthTransferContractRepository ethTransferContractRepository;
    private final EthTransferContractViewMapper ethTransferContractViewMapper;
    private final String serviceArkAddress;
    private final ScriptExecutorService scriptExecutorService;
    private final ExchangeRateService exchangeRateService;
    private final EthBalanceScriptExecutor ethBalanceScriptExecutor;

    private final ArkService arkService;

    private final RestTemplate listenerRestTemplate = new RestTemplateBuilder()
            .rootUri("http://localhost:8080/")
            .build();

    @GetMapping("/eth-transfer-service-info")
    public ServiceInfoView getServiceInfo() {
        GetBalanceResult getBalanceResult = ethBalanceScriptExecutor.execute();

        ServiceInfoView serviceInfoView = new ServiceInfoView();
        serviceInfoView.setCapacity(
            getBalanceResult.getBalance().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + " Eth");
        serviceInfoView.setFlatFeeArk(arkFlatFee.toPlainString());
        serviceInfoView.setPercentFee(arkFeePercent.toPlainString());
        serviceInfoView.setStatus(ServiceInfoView.STATUS_UP);

        return serviceInfoView;
    }

    @PostMapping("/eth-transfer-contracts")
    public EthTransferContractView createEthTransaction(
            @RequestParam("returnArkAddress") String returnArkAddress,
            @RequestParam("recipientEthAddress") String recipientEthAddress,
            @RequestParam("ethAmount") String ethAmountStr
    ) {
        BigDecimal ethAmount = new BigDecimal(ethAmountStr).setScale(8, BigDecimal.ROUND_HALF_UP);

        BigDecimal arkPerEthExchangeRate = exchangeRateService.getRate("ETH", "ARK");
        BigDecimal baseArkCost = ethAmount.multiply(arkPerEthExchangeRate);

        BigDecimal arkFeeTotal = baseArkCost
            .multiply(arkFeePercent.divide(new BigDecimal("100.00000000"), BigDecimal.ROUND_UP))
            .add(arkFlatFee)
            .add(arkTransactionFee);

        BigDecimal requiredArkCost = baseArkCost.add(arkFeeTotal);

        EthTransferContractEntity entity = new EthTransferContractEntity();
        entity.setToken(UUID.randomUUID().toString());
        entity.setCreatedAt(ZonedDateTime.from(Instant.now().atOffset(ZoneOffset.UTC)));
        entity.setStatus(EthTransferContractEntity.STATUS_PENDING);
        entity.setServiceArkAddress(serviceArkAddress);
        entity.setArkFlatFee(arkFlatFee);
        entity.setArkFeePercent(arkFeePercent);
        entity.setArkFeeTotal(arkFeeTotal);
        entity.setRequiredArkAmount(requiredArkCost.setScale(8, BigDecimal.ROUND_UP));
        entity.setReturnArkAddress(returnArkAddress);
        entity.setRecipientEthAddress(recipientEthAddress);
        entity.setEthAmount(ethAmount);
        entity.setArkPerEthExchangeRate(arkPerEthExchangeRate.setScale(8, BigDecimal.ROUND_UP));
        ethTransferContractRepository.save(entity);

        // Register contract message with listener so we get a callback when a matching ark transaction is found
        CreateMessageRequest createMessageRequest = new CreateMessageRequest();
        createMessageRequest.setCallbackUrl("http://localhost:8080/eth-transfer-contracts/ark-transaction-matches");
        createMessageRequest.setToken(entity.getToken());
        listenerRestTemplate.postForObject("/messages", createMessageRequest, Void.class);

        return ethTransferContractViewMapper.map(entity);
    }

    @GetMapping("/eth-transfer-contracts/{token}")
    public EthTransferContractView getTransaction(@PathVariable String token) {
        EthTransferContractEntity ethTransferContractEntity = getContractEntityOrThrowException(token);

        return ethTransferContractViewMapper.map(ethTransferContractEntity);
    }

    @PostMapping("/eth-transfer-contracts/ark-transaction-matches")
    public void postArkTransactionMatches(@RequestBody TransactionMatch transactionMatch) {
        String token = transactionMatch.getToken();
        EthTransferContractEntity contractEntity = getContractEntityOrThrowException(token);

        // Skip already processed transactions
        if (!contractEntity.getStatus().equals(EthTransferContractEntity.STATUS_PENDING)) {
            return;
        }

        try {
            BigDecimal sentArkAmount = arkService.getArkAmount(transactionMatch.getArkTransactionId());
            BigDecimal usedArkAmount = BigDecimal.ZERO;
            if (sentArkAmount.compareTo(contractEntity.getRequiredArkAmount()) >= 0) {
                // todo: re-estimate cost and fail if given amount is too low
                try {
                    BigDecimal arkPerEthExchangeRate = exchangeRateService.getRate("ETH", "ARK");
                    usedArkAmount = contractEntity.getEthAmount().multiply(arkPerEthExchangeRate)
                        .add(contractEntity.getArkFeeTotal());

                    // todo: since deploying an eth contract breaks a transaction boundary (eth contract deployment
                    // is not idempotent), we should handle the failure scenario in a better way
                    EthTransactionResult ethTransactionResult = scriptExecutorService.executeEthTransaction(
                        contractEntity.getRecipientEthAddress(),
                        contractEntity.getEthAmount().toPlainString()
                    );
                    contractEntity.setEthTransactionHash(ethTransactionResult.getTransactionHash());

                    contractEntity.setStatus(EthContractDeployContractEntity.STATUS_COMPLETED);
                } catch (Exception e) {
                    log.error("Failed to execute deploy script", e);
                    contractEntity.setStatus(EthTransferContractEntity.STATUS_FAILED);

                    // assume we didn't spend any eth
                    usedArkAmount = arkTransactionFee;
                }
            } else {
                // The ark transaction does not contain sufficient ark to process
                usedArkAmount = arkTransactionFee;
                contractEntity.setStatus(EthContractDeployContractEntity.STATUS_REJECTED);
            }

            BigDecimal returnArkAmount = arkService.calculateReturnArkAmount(sentArkAmount, usedArkAmount);
            contractEntity.setReturnArkAmount(returnArkAmount);

            if (returnArkAmount.compareTo(BigDecimal.ZERO) > 0) {
                String returnArkTransactionId = arkService
                    .sendArk(contractEntity.getReturnArkAddress(), returnArkAmount, contractEntity.getToken());
                contractEntity.setReturnArkTransactionId(returnArkTransactionId);
            }
        } catch (Exception e) {
            log.error("Error occurred processing contract", e);
            contractEntity.setStatus(EthTransferContractEntity.STATUS_FAILED);
        }

        ethTransferContractRepository.save(contractEntity);
    }

    private EthTransferContractEntity getContractEntityOrThrowException(String token) {
        EthTransferContractEntity ethTransactionMessage = ethTransferContractRepository.findOneByToken(token);
        if (ethTransactionMessage == null) {
            throw new NotFoundException("Contract not found");
        }
        return ethTransactionMessage;
    }
}
