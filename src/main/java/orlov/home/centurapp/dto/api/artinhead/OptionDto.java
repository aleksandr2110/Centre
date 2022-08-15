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
    private List<OptionValuesDto> values = new ArrayList<>();

    @Override
    public String toString() {
        return "OptionDto{" + "\n" +
                "nameCode=" + nameCode + "\n" +
                "name=" + name + "\n" +
                "values=" + values + "\n}";
    }
}
