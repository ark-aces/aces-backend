package example.ark_smartbridge_listener.eth_transfer;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface EthTransferContractRepository extends PagingAndSortingRepository<EthTransferContractEntity, Long> {

    EthTransferContractEntity findOneByToken(String token);
}
