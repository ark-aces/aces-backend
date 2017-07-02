package example.ark_smartbridge_listener.eth_bridge;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ContractMessageRepository extends PagingAndSortingRepository<ContractMessage, Long> {

    ContractMessage findOneByToken(String token);

}
