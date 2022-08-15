package orlov.home.centurapp.service.daoservice.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.ProductProfileApp;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class AppDaoServiceTest {
    @Autowired
    private AppDaoService appDaoService;

    @Test
    void getAllProductProfileAppBySupplierId() {
        ProductProfileApp productProfileApp = appDaoService
                .getAllProductProfileAppBySupplierId(8)
                .stream()
                .filter(p -> p.getProductProfileId() == 12508)
                .findFirst()
                .get();
        Objects.nonNull(productProfileApp);
    }
}