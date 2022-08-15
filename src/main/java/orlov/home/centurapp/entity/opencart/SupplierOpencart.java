package orlov.home.centurapp.entity.opencart;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"supId", "supCode"})
public class SupplierOpencart {
    private String supId;
    private String supCode;
    private String name;
    private String isPdv;
    private String currency;
    private String contacts;
    private String sortOrder;
    private String status;
    private List<CurrencyOpencart> currencies = new ArrayList<>();
    private List<ProductSupplierOpencart> products = new ArrayList<>();
}
