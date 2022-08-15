package orlov.home.centurapp.entity.opencart;

import lombok.*;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"optionValueId", "optionId", "languageId", "name"})
public class OptionValueDescriptionOpencart {
    private int optionValueId;
    private int optionId;
    private int languageId = OCConstant.UA_LANGUAGE_ID;
    private String name = OCConstant.EMPTY_STRING;

    @Override
    public String toString() {
        return "OptionValueDescriptionOpencart{" + "\n" +
                "\t\toptionValueId=" + optionValueId + "\n" +
                "\t\toptionId=" + optionId + "\n" +
                "\t\tlanguageId=" + languageId + "\n" +
                "\t\tname=" + name + "}\n";
    }
}
