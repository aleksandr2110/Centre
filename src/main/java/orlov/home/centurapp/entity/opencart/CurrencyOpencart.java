package orlov.home.centurapp.entity.opencart;

import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"currencyId", "title", "code"})
public class CurrencyOpencart {
    private int currencyId;
    private String title;
    private String code;
    private String symbolLeft;
    private String symbolRight;
    private String decimalPlace;
    private BigDecimal value;
    private boolean status;
    private Timestamp dateModified;
    private String uuid;
}
