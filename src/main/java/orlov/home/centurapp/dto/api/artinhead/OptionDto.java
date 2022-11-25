package orlov.home.centurapp.dto.api.artinhead;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"nameCode"})
public class OptionDto {
    private String nameCode;
    private String name;
    private int minOptionPrice;
    private String optionType;
    private int required = 1;
    private List<OptionValuesDto> values = new ArrayList<>();

    public OptionDto(String nameCode, String name, int minOptionPrice, String optionType, List<OptionValuesDto> values) {
        this.nameCode = nameCode;
        this.name = name;
        this.minOptionPrice = minOptionPrice;
        this.optionType = optionType;
        this.values = values;
    }

    @Override
    public String toString() {
        return "OptionDto{" + "\n" +
                "nameCode=" + nameCode + "\n" +
                "name=" + name + "\n" +
                "values=" + values + "\n}";
    }
}
