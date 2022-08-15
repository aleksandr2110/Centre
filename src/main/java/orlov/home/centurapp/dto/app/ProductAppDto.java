package orlov.home.centurapp.dto.app;

import lombok.*;
import orlov.home.centurapp.util.AppConstant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"sku"})
public class ProductAppDto {
    private String url = AppConstant.EMPTY_STRING;
    private String sku = AppConstant.EMPTY_STRING;
    private String title = AppConstant.EMPTY_STRING;
    private String description = AppConstant.EMPTY_STRING;
    private String imageUrl = AppConstant.EMPTY_STRING;
    private List<String> imagesUrl = new ArrayList<>();
    private BigDecimal price = new BigDecimal("0.0").setScale(4);
    private boolean status;
}
