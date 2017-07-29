package example.ark_smartbridge_listener.eth_bridge;

import lib.NiceObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j
public class ScriptExecutorService {

    private final NiceObjectMapper niceObjectMapper;
    private final String ethServerUrl;
    private final String scriptPath;
    private final String nodeCommand;

    private final String serviceEthAccountAddress;
    private final String serviceEthAccountPassword;


    public GasEstimateResult executeEstimateGas(String code) {
        String script = scriptPath + "/estimate-gas.js";
        ProcessBuilder pb = new ProcessBuilder(nodeCommand, script, ethServerUrl, code);
        String output = executeAndCapture(pb);

        return niceObjectMapper.readValue(output, GasEstimateResult.class);
    }

    public ContractDeployResult executeContractDeploy(String abiJson, String code, String paramsJson, Long gasLimit) {
        String script = scriptPath + "/deploy-contract.js";

        ProcessBuilder pb = new ProcessBuilder(
            nodeCommand,
            script,
            ethServerUrl,
            serviceEthAccountAddress,
            serviceEthAccountPassword,
            abiJson,
            code,
            paramsJson,
            gasLimit.toString()
        );
        String output = executeAndCapture(pb);

        return niceObjectMapper.readValue(output, ContractDeployResult.class);
    }

    private String executeAndCapture(ProcessBuilder processBuilder) {
        try {
            log.info("executing command: " + StringUtils.join(processBuilder.command().toArray(), " "));
            Process process = processBuilder.start();
            process.waitFor();

            String output = IOUtils.toString(process.getInputStream(), "UTF-8");
            if (process.exitValue() != 0) {
                String errorOutput =  IOUtils.toString(process.getErrorStream(), "UTF-8");
                throw new RuntimeException("Error running script: " + errorOutput);
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
