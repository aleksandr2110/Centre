package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.entity.opencart.SupplierOpencart;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class SupplierOpencartDaoTest {
    @Autowired
    private SupplierOpencartDao supplierOpencartDao;

    @Test
    void getAll() {
        List<SupplierOpencart> supplierOpencartList = supplierOpencartDao.getAll().stream().distinct().collect(Collectors.toList());
        supplierOpencartList.forEach(s -> log.info("Supplier opencart: {}", s));
    }
}