package io.ark.ark_client;

import lombok.Data;

@Data
public class Peer {
    private String ip;
    private Integer port;
    private String version;
    private Integer height;
    private String status;
    private Integer delay;
}
