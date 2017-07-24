package io.ark.ark_client;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CreateArkTransactionRequest {

    long timestamp;
    String recipientId;
    Long amount;
    Long fee;
    byte type;
    String vendorField;
    String signature;
    String signSignature;
    String senderPublicKey;
    String requesterPublicKey;
    Map<String, Object> asset = new HashMap<>();
    String id;

}
