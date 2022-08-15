package orlov.home.centurapp.dto.app;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductHatorDto {
    private boolean isDefault = false;
    private Integer displayPrice;
    private Map<String, String> attributes = new HashMap<>();
}
