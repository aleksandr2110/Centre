package orlov.home.centurapp.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"productId", "attributeId"})
public class ProductToAttributeDto {
    private int productId;
    private String sku;
    private int attributeId;
    private int newAttributeId;
    private String text;
}
