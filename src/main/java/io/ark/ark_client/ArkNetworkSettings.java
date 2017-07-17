package io.ark.ark_client;

import lombok.Data;

import java.util.List;

@Data
public class ArkNetworkSettings {
    private String scheme;
    private List<ArkNetworkPeer> peers;
}
