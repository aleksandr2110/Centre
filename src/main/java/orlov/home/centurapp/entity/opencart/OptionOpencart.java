package orlov.home.centurapp.entity.opencart;

import lombok.*;
import orlov.home.centurapp.util.OCConstant;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"optionId"})
public class OptionOpencart {
    private int optionId;
    private String type = OCConstant.OPTION_TYPE_RADIO;
    private int sortOrder = OCConstant.SORT_ORDER;
    private String uuid = OCConstant.EMPTY_STRING;
    private List<OptionDescriptionOpencart> descriptions = new ArrayList<>();
    private List<OptionValueOpencart> values = new ArrayList<>();
    private ProductOptionOpencart productOptionOpencart;

    @Override
    public String toString() {
        return "\nOptionOpencart{" + "\n" +
                "\toptionId=" + optionId + "\n" +
                "\ttype=" + type + "\n" +
                "\tsortOrder=" + sortOrder + "\n" +
                "\tuuid=" + uuid + "\n" +
                "\tdescriptions=" + descriptions + "\n" +
                "\tproductOptionOpencart=" + productOptionOpencart + "\n" +
                "\tvalues=" + values + "\n}";
    }
}
