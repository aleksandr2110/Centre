package orlov.home.centurapp.entity.opencart;

import lombok.*;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"optionId", "languageId", "name"})
public class OptionDescriptionOpencart {
    private int optionId;
    private int languageId = OCConstant.UA_LANGUAGE_ID;
    private String name = OCConstant.EMPTY_STRING;

    @Override
    public String toString() {
        return "OptionDescriptionOpencart{" + "\n" +
                "\t\toptionId=" + optionId + "\n" +
                "\t\tlanguageId=" + languageId + "\n" +
                "\t\tname=" + name + "}\n";
    }
}
