package example.ark_smartbridge_listener.eth_contract_deploy;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface EthContractDeployContractRepository extends PagingAndSortingRepository<EthContractDeployContractEntity, Long> {

    EthContractDeployContractEntity findOneByToken(String token);

}
