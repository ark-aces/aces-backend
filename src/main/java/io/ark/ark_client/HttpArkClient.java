package io.ark.ark_client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RequiredArgsConstructor
public class HttpArkClient implements ArkClient {
    
    private final ArkNetwork arkNetwork;
    private final RestTemplate restTemplate;

    public List<Transaction> getTransactions(Integer offset) {
        return restTemplate
            .exchange(
                getRandomHostBaseUrl() + "/api/transactions?orderBy=timestamp:desc&limit=50&offset={offset}",
                HttpMethod.GET,
                null,
                TransactionsResponse.class,
                offset
            )
            .getBody()
            .getTransactions();
    }

    public Transaction getTransaction(String id) {
        return restTemplate
            .exchange(
                getRandomHostBaseUrl() + "/api/transactions/get?id={id}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<TransactionWrapper>() {},
                id
            ).getBody().getTransaction();
    }
    
    private String getRandomHostBaseUrl() {
        String httpScheme = arkNetwork.getHttpScheme();
        ArkNetworkPeer targetHost = arkNetwork.getRandomHost();
        return httpScheme + "://" + targetHost.getHostname() + ":" + targetHost.getPort();
    }
}
