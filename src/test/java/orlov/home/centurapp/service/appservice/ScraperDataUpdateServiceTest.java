package orlov.home.centurapp.service.appservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.service.daoservice.app.SupplierAppService;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ScraperDataUpdateServiceTest {

    @Autowired
    private ScraperDataUpdateService scraperDataUpdateService;

    @Autowired
    private SupplierAppService supplierAppService;


    @Test
    void updateProductSupplierOpencart() {
        SupplierApp byName = supplierAppService.getByName("maresto");
//        scraperDataUpdateService.updateProductSupplierOpencart(byName);
    }
}