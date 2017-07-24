package io.ark.ark_client;

import lombok.Data;

import java.util.List;

@Data
public class CreateArkTransactionsRequest {

    private List<CreateArkTransactionRequest> transactions;

}
