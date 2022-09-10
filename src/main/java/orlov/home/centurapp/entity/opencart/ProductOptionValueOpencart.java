package orlov.home.centurapp.entity.opencart;

import lombok.*;
import orlov.home.centurapp.util.OCConstant;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"productOptionValueId", "productOptionId", "productId", "optionId", "optionValueId"})
public class ProductOptionValueOpencart {
    private int productOptionValueId;
    private int productOptionId;
    private int productId;
    private int optionId;
    private int optionValueId;
    private int quantity = OCConstant.QUANTITY;
    private boolean subtract = OCConstant.SUBTRACT;
    private BigDecimal price = OCConstant.PRICE;
    private String pricePrefix = OCConstant.PLUS_SIGN;
    private int points = OCConstant.ZERO;
    private String pointsPrefix = OCConstant.PLUS_SIGN;
    private BigDecimal weight = OCConstant.WEIGHT;
    private String weightPrefix = OCConstant.PLUS_SIGN;
    private BigDecimal reward = OCConstant.PRICE;
    private String rewardPrefix = OCConstant.PLUS_SIGN;
    private String optsku = OCConstant.EMPTY_STRING;


    @Override
    public String toString() {
        return "\tProductOptionValueOpencart{" + "\n" +
                "\t\tproductOptionValueId=" + productOptionValueId + "\n" +
                "\t\tproductOptionId=" + productOptionId + "\n" +
                "\t\tproductId=" + productId + "\n" +
                "\t\toptionId=" + optionId + "\n" +
                "\t\toptionValueId=" + optionValueId + "\n" +
                "\t\tquantity=" + quantity + "\n" +
                "\t\tsubtract=" + subtract + "\n" +
                "\t\tprice=" + price + "\n" +
                "\t\tpricePrefix=" + pricePrefix + "\n" +
                "\t\tpoints=" + points +"\n" +
                "\t\tpointsPrefix=" + pointsPrefix + "\n" +
                "\t\tweight=" + weight +"\n" +
                "\t\tweightPrefix=" + weightPrefix + "\n" +
                "\t\treward=" + reward +"\n" +
                "\t\trewardPrefix=" + rewardPrefix + "\n" +
                "\t\toptsku=" + optsku + "\n" +
                "\t}";
    }
}
