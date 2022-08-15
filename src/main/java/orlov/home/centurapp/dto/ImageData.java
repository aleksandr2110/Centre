package orlov.home.centurapp.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ImageData {
    private String thumb;
    private String img;
    private String full;
    private String caption;
    private Integer position;
    private Boolean isMain;
    private String type;
    private String videoUrl;
}
