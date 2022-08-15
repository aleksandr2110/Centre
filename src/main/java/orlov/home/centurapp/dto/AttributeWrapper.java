package orlov.home.centurapp.dto;

import lombok.*;
import orlov.home.centurapp.entity.opencart.AttributeOpencart;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"keySite"})
public class AttributeWrapper {
    private String keySite;
    private String valueSite;
    private AttributeOpencart attributeOpencart;
}
