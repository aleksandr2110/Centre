package orlov.home.centurapp.entity.app;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"productProfileAppId", "attributeAppId"})
public class ProductAttributeApp {
    private int productProfileAppId;
    private int attributeAppId;
    private String attributeValue;
}
