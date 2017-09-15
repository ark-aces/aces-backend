package example.ark_smartbridge_listener;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NumberFormatter {

    public String formatNumber(BigDecimal value) {
        if (value.compareTo(new BigDecimal("1000000")) > 0) {
            return value.divide(new BigDecimal("1000000"), 0, BigDecimal.ROUND_DOWN).toPlainString() + "M";

        } else if (value.compareTo(new BigDecimal("1000")) > 0) {
            return value.divide(new BigDecimal("1000"), 0, BigDecimal.ROUND_DOWN).toPlainString() + "k";
        } else {
            return value.setScale(2, RoundingMode.DOWN).toPlainString();
        }
    }

}
