package orlov.home.centurapp.service.appservice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ProductOpencart;
import orlov.home.centurapp.entity.opencart.ProductSupplierOpencart;
import orlov.home.centurapp.entity.opencart.SupplierOpencart;
import orlov.home.centurapp.service.daoservice.opencart.ProductOpencartService;
import orlov.home.centurapp.service.daoservice.opencart.SupplierOpencartService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ScraperDataUpdateService {

    private final SupplierOpencartService supplierOpencartService;
    private final ProductOpencartService productOpencartService;

    public void updateProductSupplierOpencartBySupplierApp(SupplierApp supplierApp) {
        SupplierOpencart supplierOpencart = supplierOpencartService.getBySubCode(supplierApp.getDisplayName());
        String availability = "В наявності";
        if (Objects.nonNull(supplierOpencart)) {

            List<ProductOpencart> allProductOpencart = productOpencartService.getAllBySupplier(supplierApp.getName());

            List<ProductSupplierOpencart> allProductSupplierOpencart = allProductOpencart
                    .stream()
                    .map(p -> new ProductSupplierOpencart(p.getId(), supplierOpencart.getSupCode(), p.getPrice(), supplierOpencart.getIsPdv(), supplierOpencart.getCurrency(), availability, ""))
                    .collect(Collectors.toList());

            List<ProductSupplierOpencart> productsSupplierOpencartDB = productOpencartService.getAllProductSupplierBySupCode(supplierOpencart.getSupCode());

            allProductSupplierOpencart
                    .forEach(po -> {
                        boolean contains = productsSupplierOpencartDB.contains(po);
                        if (contains){
                            ProductSupplierOpencart productSupplierOpencart = productsSupplierOpencartDB.get(productsSupplierOpencartDB.indexOf(po));
                            if (!productSupplierOpencart.getPrice().equals(po.getPrice())){
                                productOpencartService.updatePriceProductSupplier(po);
                            }
                        } else {
                            productOpencartService.saveProductSupplier(po);
                        }
                    });

        } else {
            log.warn("Not found supplier opencart by display name: {}", supplierApp.getDisplayName());
        }

    }

}
