package io.ark.ark_client;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.math.RandomUtils;

import java.util.List;

@RequiredArgsConstructor
public class ArkNetwork {
    
    private final List<ArkNetworkHost> hosts;
    
    public ArkNetworkHost getRandomHost() {
        return hosts.get(RandomUtils.nextInt(hosts.size()));
    }
}
