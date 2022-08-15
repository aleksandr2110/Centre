package orlov.home.centurapp.dto.api.artinhead;

import lombok.*;
import orlov.home.centurapp.util.OCConstant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"valueCode"})
public class OptionValuesDto {
    private String valueCode;
    private String value;
    private boolean isDefault = false;
    private int margin;
    private int price;
    private String imgUrl = OCConstant.EMPTY_STRING;
    private String dbpathImage = OCConstant.EMPTY_STRING;


    @Override
    public String toString() {
        return "OptionValuesDto{" + "\n" +
                "\t\tvalueCode=" + valueCode + "\n" +
                "\t\tvalue=" + value + "\n" +
                "\t\tisDefault=" + isDefault + "\n" +
                "\t\timgUrl=" + imgUrl + "\n" +
                "\t\tdbpathImage=" + dbpathImage + "\n}";
    }
}
