package example.ark_smartbridge_listener.ark_listener;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface MessageRepository extends PagingAndSortingRepository<Message, Long> {
    Message findOneByToken(String token);
}
