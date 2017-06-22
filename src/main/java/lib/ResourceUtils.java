package lib;

import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

public class ResourceUtils {
    
    public static InputStream getInputStream(String filename) {
        InputStream fileInputStream;
        try {
            fileInputStream = new ClassPathResource(filename).getInputStream();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load file: " + filename, e);
        }
        return fileInputStream;
    }
    
}
