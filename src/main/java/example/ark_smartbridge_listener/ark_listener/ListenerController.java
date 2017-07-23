package example.ark_smartbridge_listener.ark_listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@RestController
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ListenerController {

    private final MessageRepository messageRepository;

    @PostMapping("/messages")
    public Message postMessage(@RequestBody CreateMessageRequest createMessageRequest) {
        Message message = new Message();
        message.setCallbackUrl(createMessageRequest.getCallbackUrl());
        message.setToken(createMessageRequest.getToken());
        message.setCreatedAt(ZonedDateTime.from(Instant.now().atOffset(ZoneOffset.UTC)));
        messageRepository.save(message);

        return message;
    }

}