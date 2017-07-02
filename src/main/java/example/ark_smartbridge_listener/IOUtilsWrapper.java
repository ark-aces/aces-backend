package example.ark_smartbridge_listener;

import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class IOUtilsWrapper {

    public static String read(MultipartFile multipartFile) {
        try {
            return IOUtils.toString(multipartFile.getInputStream(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read code content as UTF-8", e);
        }
    }

}
