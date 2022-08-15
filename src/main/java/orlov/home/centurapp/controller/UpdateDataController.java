package orlov.home.centurapp.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orlov.home.centurapp.entity.app.AttributeApp;
import orlov.home.centurapp.entity.app.ManufacturerApp;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ManufacturerOpencart;
import orlov.home.centurapp.service.appservice.UpdateDataService;
import orlov.home.centurapp.service.daoservice.app.AttributeAppService;
import orlov.home.centurapp.service.daoservice.app.ManufacturerAppService;
import orlov.home.centurapp.service.daoservice.opencart.ManufacturerOpencartService;
import orlov.home.centurapp.service.parser.ParserServiceNowystyl;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@Controller
@RequestMapping("/update")
@SessionAttributes({"supplier"})
@Slf4j
public class UpdateDataController {

    private final UpdateDataService updateService;
    private final AttributeAppService attributeAppService;
    private final ParserServiceNowystyl parserServiceNowystyl;
    private final UpdateDataService updateDataService;
    private final ManufacturerAppService manufacturerAppService;
    private final ManufacturerOpencartService manufacturerOpencartService;

    @GetMapping("/file/product/{supplierId}")
    @ResponseBody
    public void getExcelFile(@PathVariable int supplierId,
                             HttpServletResponse response,
                             @ModelAttribute("supplier") SupplierApp supplier) {
        log.info("Get excel file product by supplier id: {}", supplierId);
        Path excelFileWithProduct = updateDataService.getExcelFile(supplierId);

        if (Files.exists(excelFileWithProduct)) {
//            response.setHeader("Content-disposition", "attachment;filename=" + "product_data.xlsx");
            response.setHeader("Content-disposition", "attachment;filename=" + excelFileWithProduct.getFileName());
            response.setContentType("application/vnd.ms-excel");
            try {
                Files.copy(excelFileWithProduct, response.getOutputStream());
                response.getOutputStream().flush();
            } catch (IOException e) {
                log.info("Error writing file to output stream. Filename was '{}'", excelFileWithProduct, e);
                throw new RuntimeException("IOError writing file to output stream");
            }
        }

    }

    @PostMapping("/file/product")
    public String uploadExcelFile(@RequestParam("file") MultipartFile file,
                                  @ModelAttribute("supplier") SupplierApp supplier,
                                  RedirectAttributes redirectAttributes) {

        log.info("Update product excel controller: {}", file.getSize());
        String fileFormat = FilenameUtils.getExtension(file.getOriginalFilename());
        log.info("File format: {}", fileFormat);

        log.info("Supplier: {}", supplier);


        if (file.getSize() == 0) {
            redirectAttributes.addFlashAttribute("uploadEmpty", "Порожній файл");
        } else if (!fileFormat.equals("xlsx")) {
            redirectAttributes.addFlashAttribute("uploadFormat", "Неправильний формат файлу");
        } else {
            updateDataService.uploadExcelFile(file);
            redirectAttributes.addFlashAttribute("uploadResult", "Файл успішно завантажено ");
        }

        return "redirect:/supplier/file";

    }

    @GetMapping("/file/image/{supplierId}")
    @ResponseBody
    public void getZipFileImage(@PathVariable int supplierId,
                                HttpServletResponse response,
                                @ModelAttribute("supplier") SupplierApp supplier) {

        log.info("Get file supplier product by id: {}", supplierId);
        Path zipFile = updateDataService.getZipFileImage(supplierId);

        if (Files.exists(zipFile)) {
            response.setHeader("Content-disposition", "attachment;filename=" + "images.zip");
            response.setContentType("application/zip");
            try {
                Files.copy(zipFile, response.getOutputStream());
                response.getOutputStream().flush();
            } catch (IOException e) {
                log.info("Error writing file to output stream. Filename was '{}'", zipFile, e);
                throw new RuntimeException("IOError writing file to output stream");
            }
        }

    }

    @PostMapping("/file/image")
    public String uploadZipFileImage(@RequestParam("file") MultipartFile file,
                                     @ModelAttribute("supplier") SupplierApp supplier,
                                     RedirectAttributes redirectAttributes) {

        log.info("Update product excel controller: {}", file.getSize());
        String fileFormat = FilenameUtils.getExtension(file.getOriginalFilename());
        log.info("File format: {}", fileFormat);

        log.info("Supplier: {}", supplier);


        if (file.getSize() == 0) {
            redirectAttributes.addFlashAttribute("uploadEmptyI", "Порожній файл");
        } else if (fileFormat.equals("zip")) {
            updateDataService.uploadZipFileImage(file, supplier);
            redirectAttributes.addFlashAttribute("uploadResultI", "Файл .zip успішно завантажений");
        } else {
            redirectAttributes.addFlashAttribute("uploadFormatI", "Невірний формат файлу (має бути .zip)");
        }

        return "redirect:/supplier/file";

    }

    @PostMapping("/manufacturer")
    public String updateManufacturer(@ModelAttribute("supplier") SupplierApp supplier,
                                     @ModelAttribute("manunew") ManufacturerApp manufacturerApp) {
        log.info("Manufacturer: {}", manufacturerApp);
        log.info("supplier: {}", supplier);
        updateDataService.updateManufacturer(manufacturerApp, supplier);
        return "redirect:/supplier/manufacturer";
    }






    //  TODO revise login attribute constructor
    @GetMapping("/attribute/data/{id}")
    public String updateDataAttribute(@PathVariable("id") int attributeId,
                                      Model model) {
        log.info("Update attributes by supplier id: {}", attributeId);
        AttributeApp byId = attributeAppService.getById(attributeId);
        model.addAttribute("attr", byId);
        return "t_attribute_data";
    }

    @PostMapping("/attribute")
    public String updateAttribute(@ModelAttribute("supplier") SupplierApp supplier,
                                  @ModelAttribute("newattr") AttributeApp attributeApp) {
        log.info("Attribute: {}", attributeApp);
        log.info("supplier: {}", supplier);
        updateDataService.updateAttribute(attributeApp, supplier);
        return "redirect:/supplier/attribute";
    }

    @PostMapping("/attribute/data")
    public String updateDataAttributePost(@ModelAttribute("attr") AttributeApp attributeApp,
                                          @ModelAttribute("supplier") SupplierApp supplierApp) {
        log.info("Attribute new data: {}", attributeApp);
        log.info("SupplierApp: {}", supplierApp);
        updateDataService.updateAttributeData(attributeApp, supplierApp);
        return "redirect:/update/attribute/data/".concat(String.valueOf(attributeApp.getAttributeId()));
    }






    @GetMapping("/test/text")
    public String testText(Model model,
                           @ModelAttribute("attr") AttributeApp testarrt,
                           @RequestParam(value = "testtext", required = false, defaultValue = "") String testText) {
        log.info("testarrt: {}", testarrt);
        log.info("testText: {}", testText);
        model.addAttribute("testarrt", testarrt);
        model.addAttribute("testtext", testText);
        if (!testText.isEmpty()) {
            String result = parserServiceNowystyl.changeAttributeValue(testarrt, testText);
            model.addAttribute("result", result);
        }

        return "t_attribute_data_test";
    }





//    @GetMapping("/price/{supplierId}")
//    public String updatePrice(@PathVariable("supplierId") int supplierId) {
//        log.info("Update price by supplier id: {}", supplierId);
//        updateService.updatePrice(supplierId);
//        return "redirect:/supplier";
//    }

}
