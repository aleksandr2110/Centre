package orlov.home.centurapp.dao.opencart;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class CategoryOpencartDaoTest {

    @Autowired
    private CategoryOpencartDao categoryOpencartDao;

    @Test
    void save() {
        CategoryOpencart categoryOpencart = new CategoryOpencart.Builder()
                .withImage("catalog/app/playstation-ps5-ps4-sony.jpg")
                .withParentId(0)
                .withTop(true)
                .withColumn(1)
                .withSortOrder(15)
                .build();
        int id = categoryOpencartDao.save(categoryOpencart);
        log.info("Category id: {}", id);

    }

    @Test
    void getCategoryOpencartByName() {
        SupplierApp supplierApp = new SupplierApp();
        supplierApp.setMarkup(10);
        supplierApp.setName("nowystyl");

        List<CategoryOpencart> nowystyl = categoryOpencartDao.getAllSupplierCategoryOpencart(supplierApp);
        log.info("Supplier category size: {}", nowystyl.size());
    }

    @Test
    void getById() {
    }

    @Test
    void deleteById() {
    }

    @Test
    void update() {
    }

    @Test
    void getAll() {
    }


}