package orlov.home.centurapp.dto;

import lombok.*;
import orlov.home.centurapp.entity.app.ManufacturerApp;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ManufacturerOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ManufacturerUpdateDto {
    private String supplierName;
    private String sku;
    private int manufacturerOpencartId;
}
