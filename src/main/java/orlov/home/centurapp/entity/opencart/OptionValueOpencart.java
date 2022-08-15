package orlov.home.centurapp.entity.opencart;

import lombok.*;
import orlov.home.centurapp.util.OCConstant;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"optionValueId", "optionId"})
public class OptionValueOpencart {
    private int optionValueId;
    private int optionId;
    private String image = OCConstant.EMPTY_STRING;
    private int sortOrder = OCConstant.SORT_ORDER;
    private String uuid = OCConstant.EMPTY_STRING;
    private List<OptionValueDescriptionOpencart> descriptionValue = new ArrayList<>();
    private ProductOptionValueOpencart productOptionValueOpencart;

    @Override
    public String toString() {
        return "OptionValueOpencart{" + "\n" +
                "\t\toptionValueId=" + optionValueId +"\n" +
                "\t\toptionId=" + optionId +"\n" +
                "\t\timage=" + image + "\n" +
                "\t\tsortOrder=" + sortOrder +"\n" +
                "\t\tuuid=" + uuid + "\n" +
                "\t\tproductOptionValueOpencart=" + productOptionValueOpencart + "\n" +
                "\t\tdescriptionValue=" + descriptionValue + "}\n";
    }
}
