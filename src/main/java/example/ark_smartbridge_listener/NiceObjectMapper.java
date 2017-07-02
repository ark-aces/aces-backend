package example.ark_smartbridge_listener;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class NiceObjectMapper {

    private final ObjectMapper objectMapper;

    public NiceObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T readValue(String content, Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json: " + content, e);
        }
    }
}
