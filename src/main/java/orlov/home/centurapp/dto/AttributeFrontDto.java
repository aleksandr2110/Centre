package orlov.home.centurapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.velocity.tools.config.SkipSetters;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttributeFrontDto {
    private int id;
    private String name;
    private String nameOpencart;
}
