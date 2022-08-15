package orlov.home.centurapp.entity.opencart;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"productId", "supCode"})
public class ProductSupplierOpencart {
    private int productId;
    private String supCode;
    private BigDecimal price;
    private String isPdv;
    private String currency;
    private String availability;
    private String model;
}
