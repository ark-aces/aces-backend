package io.ark.ark_client;

import java.util.List;

public interface ArkClient {
    List<Transaction> getTransactions(Integer offset);
    Transaction getTransaction(String arkTransactionId);
    Transaction createTransaction(String recipientId, Long satoshiAmount, String vendorField, String passphrase);
}