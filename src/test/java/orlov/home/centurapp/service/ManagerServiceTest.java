package orlov.home.centurapp.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import orlov.home.centurapp.service.daoservice.opencart.AttributeOpencartService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ManagerServiceTest {

    @Autowired
    private ManagerService managerService;
    @Autowired
    private AttributeOpencartService attributeOpencartService;

    @Test
    void process() {
        managerService.processApp();
    }

    @Test
    void testBuildWebDriver(){
        managerService.testbuildWebDriver();
    }

    @Test
    void moveImagesToSupplierDir(){
        managerService.moveImagesToSupplierDir();
    }

}