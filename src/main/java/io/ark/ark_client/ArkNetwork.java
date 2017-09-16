package io.ark.ark_client;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Data
public class ArkNetwork {

    private final String httpScheme;
    private final List<ArkNetworkPeer> hosts;
    private final String netHash;
    private final String port;
    private final String version;

}
