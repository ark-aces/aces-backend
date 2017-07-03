package example.ark_smartbridge_listener.ark_listener;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.ZonedDateTime;

@Entity
@Data
public class Message {
    
    @Id
    @GeneratedValue
    private Long id;
    
    private String token;
    
    private String callbackUrl;
    
    private ZonedDateTime createdAt;
}
