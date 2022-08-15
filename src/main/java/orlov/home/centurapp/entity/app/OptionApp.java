package orlov.home.centurapp.entity.app;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"optionId", "productProfileId", "valueId"})
public class OptionApp {
    private int optionId;
    private int productProfileId;
    private int valueId;
    private String optionValue;
    private BigDecimal optionPrice;
}
