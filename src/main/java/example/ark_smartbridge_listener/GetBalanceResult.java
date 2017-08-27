package example.ark_smartbridge_listener;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetBalanceResult {
    private BigDecimal balance;
}
