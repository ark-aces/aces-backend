package io.ark.ark_client;

import lombok.Data;

import java.util.List;

@Data
public class TransactionsResponse {
    private Boolean success;
    private List<Transaction> transactions;
}
