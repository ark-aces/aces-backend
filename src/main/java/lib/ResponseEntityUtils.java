package lib;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class ResponseEntityUtils {

    public static ResponseEntity<Resource> resource(String content, MediaType contentType) {
        InputStreamResource resource = new InputStreamResource(
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );

        return ResponseEntity.ok()
            .contentType(contentType)
            .body(resource);
    }
}
