package orlov.home.centurapp.controller;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import orlov.home.centurapp.service.ManagerService;

import java.util.List;

@Controller
@RequestMapping("/admin/block")
@AllArgsConstructor
@Slf4j
public class AdminController {

    private final ManagerService managerService;

    @GetMapping
    public String admin() {
        return "t_admin";
    }

    @GetMapping("/category")
    public String updateCategory() {
        log.info("updateCategory");
//        managerService.updateCategory();
        return "redirect:/admin/block";
    }




    @GetMapping("/rp/image")
    public String downloadImagesRP() {
        managerService.downloadImageRP();
        return "redirect:/admin/block";
    }

    @GetMapping("/attrapp/value")
    public String updateAttributeValue() {
        managerService.updateAttributeAppValue();
        return "redirect:/admin/block";
    }


    @GetMapping("/google")
    public String chekBrowser() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920x1080");

        WebDriver driver = new ChromeDriver(options);

        driver.get("https://nordvpn.com/ru/what-is-my-ip/");
        List<WebElement> until = new WebDriverWait(driver, 20, 500)
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//h1[@class='Title h3 mb-6 js-ipdata-ip-address']")));

        log.info("Uril size: {}", until.size());
        WebElement webElement = until.get(0);
        String text = webElement.getText();
        log.info("text ip: {}", text);
        return "redirect:/admin/block";
    }

    @GetMapping("/imgupd")
    public String updateImageRPMain() {
        log.info("updateImage");
        managerService.downloadMainImageRP();
        return "redirect:/admin/block";
    }


    @GetMapping("/image")
    public String updateImage() {
        log.info("updateImage");
//        managerService.updateImages();
        return "redirect:/admin/block";
    }

    @GetMapping("/parsing")
    public String updateParsing() {
        log.info("updateParsing");
        managerService.processApp();
        return "redirect:/admin/block";
    }

    @GetMapping("/model")
    public String updateModel() {
        log.info("updateParsing");
        managerService.updateModel();
        return "redirect:/admin/block";
    }

    @GetMapping("/newmodel")
    public String updateNewModel() {
        log.info("updateParsing");
        managerService.updateNewModel();
        return "redirect:/admin/block";
    }


    @GetMapping("/desc/kodaki")
    public String updateDescKodaki() {
        log.info("updateDesc");
        managerService.updateDescriptioKodki();
        return "redirect:/admin/block";
    }

    @GetMapping("/desc/maresto")
    public String updateDescMaresto() {
        log.info("updateDesc");
        managerService.updateDescriptionMaresto();
        return "redirect:/admin/block";
    }

    @GetMapping("/desc/astim")
    public String updateDescAstim() {
        log.info("updateDesc");
        managerService.updateDescriptionAstim();
        return "redirect:/admin/block";
    }

    @GetMapping("/fulldesc")
    public String updateDescFull() {
        log.info("updateDescFull");
//        managerService.updateFullDescription();
        return "redirect:/admin/block";
    }

    @GetMapping("/deleteproduct")
    public String deleteProductData() {
        log.info("deleteProductData");
//        managerService.deleteProductData();
        return "redirect:/admin/block";
    }

    @GetMapping("/pso")
    public String updateProductSupplier() {
        log.info("deleteProductData");
//        managerService.updateProductSupplier();
        return "redirect:/admin/block";
    }

    @GetMapping("/transgood")
    public String translateGoodfood() {
        log.info("Starts translate");
        managerService.translateGoodfood();
        log.info("End translate");
        return "redirect:/admin/block";
    }

    @GetMapping("/transrp")
    public String translateRP() {
        log.info("Starts translate");
        managerService.translateRP();
        log.info("End translate");
        return "redirect:/admin/block";
    }

    @GetMapping("/hatorimg")
    public String updateHatorImg() {
        log.info("Starts translate");
        managerService.importVivat();
        log.info("End translate");
        return "redirect:/admin/block";
    }

    @GetMapping("/testfrizel")
    public String testFrizel() {
        log.info("Starts frizel");
        managerService.testFrizel();
        log.info("End frizel");
        return "redirect:/admin/block";
    }

    @GetMapping("/hatorchangeimg")
    public String hatorChangeImage() {
        log.info("Starts frizel");
        managerService.hatorChangeImage();
        log.info("End frizel");
        return "redirect:/admin/block";
    }

    @GetMapping("/oscarupdateimg")
    public String oscarImageUpdate() {
        log.info("Starts frizel");
        managerService.oscarImageUpdate();
        log.info("End frizel");
        return "redirect:/admin/block";
    }

    @GetMapping("/deletecustomdata")
    public String deleteCustomData() {
        log.info("Starts frizel");
        managerService.deleteCustomProducts();
        log.info("End frizel");
        return "redirect:/admin/block";
    }



    @GetMapping("/translateFrizel")
    public String translateFrizel() {
        log.info("Starts frizel");
        managerService.translateFrizel();
        log.info("End frizel");
        return "redirect:/admin/block";
    }

    @GetMapping("/buildWebDriver")
    public String buildWebDriver() {
        log.info("Starts frizel");
        managerService.testbuildWebDriver();
        log.info("End frizel");
        return "redirect:/admin/block";
    }

    @GetMapping("/moveimg")
    public String moveImages() {
        log.info("Starts move img");
        managerService.moveImagesToSupplierDir();
        log.info("End move img");
        return "redirect:/admin/block";
    }

}
