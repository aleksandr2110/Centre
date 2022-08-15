package orlov.home.centurapp.dto;

import lombok.Getter;
import lombok.Setter;
import orlov.home.centurapp.entity.app.OrderProcessApp;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.List;

@Getter
@Setter
public class OpencartDto {
    private OrderProcessApp orderProcessApp;
    private List<ProductOpencart> newProduct;
    private List<ProductOpencart> availableProducts;
    private List<ProductOpencart> productsOpencartDB;
}
