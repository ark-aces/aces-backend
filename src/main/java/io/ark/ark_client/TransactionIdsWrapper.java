package io.ark.ark_client;

import lombok.Data;

import java.util.List;

@Data
public class TransactionIdsWrapper {
    private String status;
    private List<String> transactionIds;
}
