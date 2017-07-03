package example.ark_smartbridge_listener.ark_listener;

import lombok.Data;

@Data
public class CreateMessageRequest {
    private String token;
    private String callbackUrl;
}
