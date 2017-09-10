package example.ark_smartbridge_listener.exchange_rate_service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ExchangeRateService
{
    private final BigDecimal arkPerEthAdjustment;

    private final RestTemplate restTemplate = new RestTemplateBuilder()
        .rootUri("https://min-api.cryptocompare.com/data/")
        .build();

    public BigDecimal getRate(String fromCurrencyCode, String toCurrencyCode) {
        // todo: we should sanity check the exchange rate to prevent a bad rate being used
        BigDecimal rate = restTemplate
            .exchange(
                "/price?fsym={from}&tsyms={to}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, BigDecimal>>() {},
                fromCurrencyCode,
                toCurrencyCode
            )
            .getBody()
            .get(toCurrencyCode);

        return rate.divide(arkPerEthAdjustment, BigDecimal.ROUND_HALF_UP);
    }
}
