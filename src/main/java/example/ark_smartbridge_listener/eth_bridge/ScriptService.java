package example.ark_smartbridge_listener.eth_bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ScriptService {

    private final ObjectMapper defaultObjectMapper;

    public Long executeEstimateGas(String code) {
        ProcessBuilder pb = new ProcessBuilder("nodejs /vagrant/bin/estimate-gas.js", code);
        String output = executeAndCapture(pb);
        return Long.parseLong(output);
    }

    public ContractDeployResult executeContractDeploy(String abiJson, String code, String paramsJson) {
        ProcessBuilder pb = new ProcessBuilder("nodejs /vagrant/bin/deploy-contract.js", abiJson, code, paramsJson);
        String output = executeAndCapture(pb);

        try {
            return defaultObjectMapper.readValue(output, ContractDeployResult.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse output: " + output, e);
        }
    }

    private String executeAndCapture(ProcessBuilder processBuilder) {
        try {
            Process process = processBuilder.start();
            process.waitFor();

            if (process.getErrorStream().read() != -1) {
                throw new RuntimeException("Error running compile script: " + process.getErrorStream());
            }
            return IOUtils.toString(process.getInputStream(), "UTF-8");
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to run compile process", e);
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Compile process interrupted", e);
        }
    }
}
