package example.ark_smartbridge_listener.eth_bridge;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface EthTransactionMessageRepository extends PagingAndSortingRepository<EthTransactionMessage, Long> {

    EthTransactionMessage findOneByToken(String token);
}
