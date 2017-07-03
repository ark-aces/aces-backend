package example.ark_smartbridge_listener.eth_bridge;

import lib.NiceObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ScriptExecutorService {

    private final NiceObjectMapper niceObjectMapper;
    private final String ethServerUrl;
    private final String scriptPath;
    private final String nodeCommand;

    public GasEstimateResult executeEstimateGas(String code) {
        String script = scriptPath + "/estimate-gas.js";
        ProcessBuilder pb = new ProcessBuilder(nodeCommand, script, ethServerUrl, code);
        String output = executeAndCapture(pb);

        return niceObjectMapper.readValue(output, GasEstimateResult.class);
    }

    public ContractDeployResult executeContractDeploy(String abiJson, String code, String paramsJson) {
        String script = nodeCommand + " " + scriptPath + "/deploy-contract.js";
        ProcessBuilder pb = new ProcessBuilder(script, ethServerUrl, abiJson, code, paramsJson);
        String output = executeAndCapture(pb);

        return niceObjectMapper.readValue(output, ContractDeployResult.class);
    }

    private String executeAndCapture(ProcessBuilder processBuilder) {
        try {
            Process process = processBuilder.start();
            process.waitFor();

            String output = IOUtils.toString(process.getInputStream(), "UTF-8");
            if (process.exitValue() != 0) {
                throw new RuntimeException("Error running compile script: " + output);
            } else {
                return output;
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to run compile process", e);
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Compile process interrupted", e);
        }
    }
}
