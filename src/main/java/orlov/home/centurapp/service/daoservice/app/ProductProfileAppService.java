package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.app.ProductProfileAppDao;
import orlov.home.centurapp.entity.app.ProductProfileApp;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class ProductProfileAppService {

    private final ProductProfileAppDao productProfileAppDao;


    public ProductProfileApp getProductProfileBySkyJan(String sku, int supplierAppId) {
        return productProfileAppDao.getProductProfileBySkyJan(sku, supplierAppId);
    }

    public ProductProfileApp save(ProductProfileApp productProfileApp){
        int id = productProfileAppDao.save(productProfileApp);
        productProfileApp.setProductProfileId(id);
        return productProfileApp;
    }

    public ProductProfileApp update(ProductProfileApp productProfileApp){
        productProfileAppDao.update(productProfileApp);
        return productProfileApp;
    }

    public List<ProductProfileApp> getAll(){
        return productProfileAppDao.getAll();
    }

    public List<ProductProfileApp> getAllBySupplierId(int supplierId){
        return productProfileAppDao.getAllBySupplierId(supplierId);
    }

    public List<ProductProfileApp> getProductProfileByManufacturerAppId(int manufacturerAppId){
        return productProfileAppDao.getProductProfileByManufacturerAppId(manufacturerAppId);
    }


    public void deleteAll(){
        productProfileAppDao.deleteAll();
    }


}
