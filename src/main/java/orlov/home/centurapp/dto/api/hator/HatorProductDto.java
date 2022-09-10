package orlov.home.centurapp.dto.api.hator;

import lombok.*;
import orlov.home.centurapp.dto.api.artinhead.OptionValuesDto;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HatorProductDto {
    private int id;
    private int price;
    private List<HatorOptionDto> hatorOptionDtoList;
    private List<OptionValuesDto> optionValuesDtoList;
}
