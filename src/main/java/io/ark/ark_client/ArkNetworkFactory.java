package io.ark.ark_client;

import lib.ResourceUtils;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;

public class ArkNetworkFactory {
    
    public ArkNetwork createFromYml(String configFilename){
        Yaml yaml = new Yaml();
        InputStream fileInputStream = ResourceUtils.getInputStream(configFilename);
        ArkNetworkSettings arkNetworkSettings = yaml.loadAs(fileInputStream, ArkNetworkSettings.class);
        
        return new ArkNetwork(arkNetworkSettings.getScheme(), arkNetworkSettings.getPeers());
    }
}
