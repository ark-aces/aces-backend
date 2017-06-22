package io.ark.ark_client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RequiredArgsConstructor
public class HttpArkClient implements ArkClient {
    
    private final String scheme;
    private final ArkNetwork arkNetwork;
    private final RestTemplate restTemplate;

    public List<Transaction> getTransactions() {
        return restTemplate
            .exchange(
                getRandomHostBaseUrl() + "/api/transactions", 
                HttpMethod.GET,
                null,
                TransactionsResponse.class
            )
            .getBody()
            .getTransactions();
    }
    
    private String getRandomHostBaseUrl() {
        ArkNetworkHost targetHost = arkNetwork.getRandomHost();
        return scheme + "://" + targetHost.getHostname() + ":" + targetHost.getPort();
    }
}
