package orlov.home.centurapp.dao.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.app.OrderProcessApp;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class OrderProcessDaoTest {

    @Autowired
    private OrderProcessAppDao dao;

    @Test
    void save() {
        OrderProcessApp orderProcess = new OrderProcessApp();
        orderProcess.setSupplierAppId(1);
        int id = dao.save(orderProcess);
        log.info("Order id: {}", id);
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

    @Test
    void getJdbcTemplateApp() {
    }
}