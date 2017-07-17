package io.ark.ark_client;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.math.RandomUtils;

import java.util.List;

@RequiredArgsConstructor
public class ArkNetwork {

    private final String httpScheme;
    private final List<ArkNetworkPeer> hosts;
    
    public ArkNetworkPeer getRandomHost() {
        return hosts.get(RandomUtils.nextInt(hosts.size()));
    }

    public String getHttpScheme() {
        return httpScheme;
    }
}
