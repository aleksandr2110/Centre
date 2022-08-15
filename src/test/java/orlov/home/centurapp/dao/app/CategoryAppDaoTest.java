package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.CategoryApp;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class CategoryAppDaoTest {

    @Autowired
    private CategoryAppDao categoryAppDao;

    @Test
    void save() {
        CategoryApp categoryApp = new CategoryApp.Builder()
                .withSupplierId(8)
                .withSupplierTitle("test s category")
                .withOpencartTitle("test oc category")
                .withMarkup(3)
                .build();
        categoryAppDao.save(categoryApp);
    }

    @Test
    void getById() {
    }

    @Test
    void deleteById() {
    }

    @Test
    void update() {
        CategoryApp categoryApp = new CategoryApp.Builder()
                .withCategoryId(1)
                .withSupplierId(8)
                .withSupplierTitle("2 test s category")
                .withOpencartTitle("2 test oc category")
                .withMarkup(13)
                .build();
        categoryAppDao.update(categoryApp);
    }

    @Test
    void getAll() {
        List<CategoryApp> all = categoryAppDao.getAll();
        all.forEach(c -> log.info("Category app: {}", c));
    }

    @Test
    void getAllBySupplierId() {
//        List<CategoryApp> all = categoryAppDao.getAllBySupplierId(8);
//        all.forEach(c -> log.info("Category app: {}", c));
    }
}