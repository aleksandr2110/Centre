package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.app.ProductAppDao;
import orlov.home.centurapp.entity.app.ProductApp;

import java.util.List;

@Repository
@Slf4j
@AllArgsConstructor
public class ProductAppService {

    private final ProductAppDao productAppDao;

    public ProductApp save(ProductApp productApp) {
        int id = productAppDao.save(productApp);
        productApp.setProductAppId(id);
        return productApp;
    }

    public List<ProductApp> getByOrderAndStatus(int orderProcessAppId, String status) {
        return productAppDao.getByOrderAndStatus(orderProcessAppId, status);
    }

    public void saveBatch(List<ProductApp> productApps){
        productAppDao.saveBatch(productApps);
    }

    public void deleteAll(){
        productAppDao.deleteAll();
    }

}
