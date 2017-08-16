package example.ark_smartbridge_listener.test_service;

import org.springframework.data.repository.CrudRepository;

public interface TestContractRepository extends CrudRepository<TestContractEntity, Long> {

    TestContractEntity findOneByToken(String token);
}
