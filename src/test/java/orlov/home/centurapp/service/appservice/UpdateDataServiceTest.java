package orlov.home.centurapp.service.appservice;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.service.daoservice.app.SupplierAppService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class UpdateDataServiceTest {

    @Autowired
    private UpdateDataService updateDataService;
    @Autowired
    private SupplierAppService supplierAppService;

    @Test
    void getExcelFileWithProduct() {
        SupplierApp nowystyl = supplierAppService.getByName("nowystyl");
        updateDataService.getExcelFile(nowystyl.getSupplierAppId());
    }

    @Test
    void giveZipImage() {
        SupplierApp nowystyl = supplierAppService.getByName("nowystyl");
        updateDataService.getZipFileImage(nowystyl.getSupplierAppId());
    }


    @Test
    void deleteUsersProduct() {
        updateDataService.deleteUsersProduct();
    }


    @Test
    void deleteAllProductData(){
//        updateDataService.deleteAllProductData();
    }

}