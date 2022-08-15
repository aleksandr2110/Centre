package orlov.home.centurapp.entity.opencart;

import lombok.*;
import orlov.home.centurapp.util.OCConstant;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"productOptionId", "productId", "optionId"})
public class ProductOptionOpencart {
    private int productOptionId;
    private int productId;
    private int optionId;
    private String value = OCConstant.EMPTY_STRING;
    private boolean required = OCConstant.REQUIRED;
    private List<ProductOptionValueOpencart> optionValues = new ArrayList<>();

    @Override
    public String toString() {
        return "ProductOptionOpencart{" + "\n" +
                "\tproductOptionId=" + productOptionId + "\n" +
                "\tproductId=" + productId + "\n" +
                "\toptionId=" + optionId + "\n" +
                "\tvalue=" + value + "\n" +
                "\trequired=" + required + "\n" +
                "\toptionValues=" + optionValues + "\n" +
                "\t}";
    }
}
