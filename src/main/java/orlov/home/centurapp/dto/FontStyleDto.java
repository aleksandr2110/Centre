package orlov.home.centurapp.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FontStyleDto {
    private String hexColor;
    private boolean isBold;
    private boolean isStrikeout;
    private short fontSize;
    private String fontName;
    private boolean isItalic;
    private byte underline;
}
