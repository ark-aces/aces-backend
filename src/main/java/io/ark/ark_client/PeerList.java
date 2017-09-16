package io.ark.ark_client;

import lombok.Data;

import java.util.List;

@Data
public class PeerList {
    private Boolean success;
    private List<Peer> peers;
}
