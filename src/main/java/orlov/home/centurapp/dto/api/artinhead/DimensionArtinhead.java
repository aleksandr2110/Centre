package orlov.home.centurapp.dto.api.artinhead;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DimensionArtinhead {
    private String length;
    private String width;
    private String height;
}
