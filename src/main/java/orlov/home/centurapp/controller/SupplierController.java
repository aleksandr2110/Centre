package orlov.home.centurapp.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orlov.home.centurapp.dto.AttributeFrontDto;
import orlov.home.centurapp.dto.ProductInfoDto;
import orlov.home.centurapp.entity.app.AttributeApp;
import orlov.home.centurapp.entity.app.CategoryApp;
import orlov.home.centurapp.entity.app.ManufacturerApp;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.AttributeOpencart;
import orlov.home.centurapp.entity.opencart.ManufacturerOpencart;
import orlov.home.centurapp.service.appservice.UpdateDataService;
import orlov.home.centurapp.service.daoservice.app.AttributeAppService;
import orlov.home.centurapp.service.daoservice.app.CategoryAppService;
import orlov.home.centurapp.service.daoservice.app.ManufacturerAppService;
import orlov.home.centurapp.service.daoservice.app.SupplierAppService;
import orlov.home.centurapp.service.daoservice.opencart.AttributeOpencartService;
import orlov.home.centurapp.service.daoservice.opencart.ManufacturerOpencartService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;
import orlov.home.centurapp.service.daoservice.opencart.SupplierOpencartService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
@Controller
@RequestMapping("/supplier")
@SessionAttributes({"supplier"})
public class SupplierController {

    private final AttributeAppService attributeAppService;
    private final AttributeOpencartService attributeOpencartService;
    private final ManufacturerOpencartService manufacturerOpencartService;
    private final ManufacturerAppService manufacturerAppService;
    private final CategoryAppService categoryAppService;
    private final SupplierAppService supplierAppService;
    private final UpdateDataService updateDataService;
    private final SupplierOpencartService supplierOpencartService;
    private final OpencartDaoService opencartDaoService;


    @GetMapping
    public String supplier(Model model) {
        List<SupplierApp> suppliers = supplierAppService.getAll();
        model.addAttribute("suppliers", suppliers);
        return "t_supplier";
    }


    @GetMapping("/attr")
    public String supplierAttr(Model model, @RequestParam(required = false, defaultValue = "") String search) {
        List<AttributeApp> attrs;
        if (search.isEmpty()) {
            attrs = attributeAppService.getAll()
                    .stream()
                    .sorted(Comparator.comparing(AttributeApp::getSupplierTitle))
                    .collect(Collectors.toList());
        } else {
            attrs = attributeAppService.getAllByLikeName(search)
                    .stream()
                    .sorted(Comparator.comparing(AttributeApp::getSupplierTitle))
                    .collect(Collectors.toList());
        }
        log.info("Attr size: {}", attrs.size());
        model.addAttribute("attr", attrs);
        return "t_supplier_data_attr";
    }

    @GetMapping("/attr/{attrid}")
    public String opencartDataAttr(Model model, @PathVariable("attrid") int attrId) {
        AttributeApp byId = attributeAppService.getById(attrId);
        log.info("Attr APP by id: {}", byId);
        String titleAttr = byId.getOpencartTitle();
        if (titleAttr.isEmpty()) {
            titleAttr = byId.getSupplierTitle();
        }
        log.info("Title : {}", titleAttr);
        AttributeOpencart attributeOpencartByName = opencartDaoService.getAttributeOpencartByName(titleAttr.replaceAll(String.valueOf((char) 160), " "));
        log.info("Atr id : {}", attributeOpencartByName.getAttributeId());

        List<ProductInfoDto> models = opencartDaoService.getAllModelByAttributeId(attributeOpencartByName.getAttributeId())
                .stream()
                .sorted(Comparator.comparing(ProductInfoDto::getModel))
                .collect(Collectors.toList());
        log.info("Attr size: {}", models.size());
        model.addAttribute("models", models);
        return "t_supplier_data_attr_prod";
    }

    @GetMapping("/{supplierId}")
    public String supplierData(@PathVariable int supplierId, Model model) {
        log.info("Suppliers: {}", supplierId);
        SupplierApp supplier = supplierAppService.getById(supplierId);
        model.addAttribute("supplier", supplier);
        return "t_supplier_data";
    }

    @GetMapping("/file")
    public String dataFile(@ModelAttribute("supplier") SupplierApp supplier) {
        log.info(" /file Supplier: {}", supplier);
        return "t_supplier_file";
    }

    @GetMapping("/manufacturer")
    public String manufacturer(Model model,
                               @ModelAttribute("supplier") SupplierApp supplier) {

        List<ManufacturerOpencart> manufacturersOpencart = manufacturerOpencartService.getAll();
        List<ManufacturerApp> manufacturersApp = manufacturerAppService.getAllBySupplierId(supplier.getSupplierAppId());

        manufacturersOpencart = manufacturersOpencart
                .stream()
                .sorted(Comparator.comparing(ManufacturerOpencart::getName))
                .collect(Collectors.toList());

        manufacturersApp = manufacturersApp
                .stream()
                .sorted(Comparator.comparing(ManufacturerApp::getSupplierTitle))
                .filter(m -> !m.getSupplierTitle().isEmpty())
                .collect(Collectors.toList());

        model.addAttribute("manuapp", manufacturersApp);
        model.addAttribute("manuoc", manufacturersOpencart);
        model.addAttribute("manunew", new ManufacturerApp());

        return "t_supplier_manu";
    }

    @GetMapping("/attribute")
    public String dataAttribute(Model model,
                                @ModelAttribute("supplier") SupplierApp supplier) {
        List<AttributeApp> attrapp = attributeAppService.getAllBySupplierId(supplier.getSupplierAppId());
        List<AttributeOpencart> attroc = attributeOpencartService.getAllWithDesc();

        attrapp = attrapp
                .stream()
                .sorted(Comparator.comparing(AttributeApp::getSupplierTitle))
                .filter(a -> !a.getSupplierTitle().isEmpty())
                .collect(Collectors.toList());

        attroc = attroc
                .stream()
                .sorted(Comparator.comparing(o -> o.getDescriptions().get(0).getName()))
                .collect(Collectors.toList());

        model.addAttribute("attrapp", attrapp);
        model.addAttribute("attroc", attroc);
        model.addAttribute("newattr", new AttributeApp());

        return "t_supplier_attr";
    }


    @GetMapping("/markup/manufacturer")
    public String updateManufacturerMarkup(Model model,
                                           @ModelAttribute("supplier") SupplierApp supplier) {
        List<ManufacturerApp> manufacturersAppDB = manufacturerAppService.getAllBySupplierId(supplier.getSupplierAppId());
        manufacturersAppDB = manufacturersAppDB
                .stream()
                .sorted(Comparator.comparing(ManufacturerApp::getSupplierTitle))
                .filter(m -> !m.getSupplierTitle().isEmpty())
                .collect(Collectors.toList());
        log.info("Supplier: {}", supplier.getSupplierAppId());
        log.info("manufacturersAppDB size: {}", manufacturersAppDB.size());
        model.addAttribute("mans", manufacturersAppDB);
        model.addAttribute("mann", new CategoryApp());
        return "t_supplier_markup_manu";
    }

    @PostMapping("/markup/manufacturer/{id}")
    public String updateManufacturerMarkup(@PathVariable("id") int categoryId,
                                           @RequestParam(value = "markup", required = false, defaultValue = "") String markup,
                                           @ModelAttribute("supplier") SupplierApp supplier,
                                           @ModelAttribute("catn") ManufacturerApp mann,
                                           RedirectAttributes redirectAttributes) {


        log.info("supplier: {}", supplier);
        log.info("mann: {}", mann);
        log.info("markup: {}", markup);
        int status = 0;
        if (markup.isEmpty()) {
            status = -1;
            redirectAttributes.addFlashAttribute("status", status);
            return "redirect:/supplier/update/".concat(String.valueOf(supplier.getSupplierAppId()));
        } else if (markup.matches("-?\\d+")) {
            int newMarkup = Integer.parseInt(markup);
            if (newMarkup >= -100 && newMarkup <= 100) {
                status = 1;
                manufacturerAppService.update(mann);
                updateDataService.updatePrice(supplier.getSupplierAppId());
            } else {
                status = -1;
            }
        }
        redirectAttributes.addFlashAttribute("status", status);
        return "redirect:/supplier/markup/manufacturer";

    }


    @GetMapping("/markup/category")
    public String updateCategoryMarkup(Model model,
                                       @ModelAttribute("supplier") SupplierApp supplier) {

        List<CategoryApp> categoriesAppDB = categoryAppService.getAllCategoryAppBySupplierAppId(supplier.getSupplierAppId());

        categoriesAppDB = categoriesAppDB
                .stream()
                .sorted(Comparator.comparing(CategoryApp::getSupplierTitle))
                .filter(c -> !c.getSupplierTitle().isEmpty())
                .collect(Collectors.toList());

        log.info("Supplier: {}", supplier.getSupplierAppId());
        log.info("categoriesAppDB size: {}", categoriesAppDB.size());
        model.addAttribute("cats", categoriesAppDB);
        model.addAttribute("catn", new CategoryApp());
        return "t_supplier_markup_cat";
    }

    @PostMapping("/markup/category/{id}")
    public String updateCategoryMarkup(@PathVariable("id") int categoryId,
                                       @RequestParam(value = "markup", required = false, defaultValue = "") String markup,
                                       @ModelAttribute("supplier") SupplierApp supplier,
                                       @ModelAttribute("catn") CategoryApp catn,
                                       RedirectAttributes redirectAttributes) {


        log.info("supplier: {}", supplier);
        log.info("catn: {}", catn);
        log.info("markup: {}", markup);
        int status = 0;
        if (markup.isEmpty()) {
            status = -1;
            redirectAttributes.addFlashAttribute("status", status);
            return "redirect:/supplier/update/".concat(String.valueOf(supplier.getSupplierAppId()));
        } else if (markup.matches("-?\\d+")) {
            int newMarkup = Integer.parseInt(markup);
            if (newMarkup >= -100 && newMarkup <= 100) {
                status = 1;
                categoryAppService.update(catn);
            } else {
                status = -1;
            }
        }
        redirectAttributes.addFlashAttribute("status", status);
        return "redirect:/supplier/markup/category";

    }


    @PostMapping("/markup/supplier/{id}")
    public String updateSupplierMargin(@PathVariable("id") int supplierId,
                                       @RequestParam(value = "markup", required = false, defaultValue = "") String markup,
                                       @ModelAttribute("supplier") SupplierApp supplier,
                                       RedirectAttributes redirectAttributes) {

        SupplierApp supplierApp = supplierAppService.getById(supplierId);
        log.info("supplier: {}", supplier);
        log.info("markup: {}", markup);
        int status = 0;
        if (markup.isEmpty()) {
            status = -1;
            redirectAttributes.addFlashAttribute("status", status);
            return "redirect:/supplier/update/".concat(String.valueOf(supplier.getSupplierAppId()));
        } else if (markup.matches("-?\\d+")) {
            int newMarkup = Integer.parseInt(markup);
            if (newMarkup >= -100 && newMarkup <= 100) {
                status = 1;
                supplierApp.setMarkup(newMarkup);
                supplierAppService.update(supplierApp);
                updateDataService.updatePrice(supplier.getSupplierAppId(),false);
            } else {
                status = -1;
            }
        }

        redirectAttributes.addFlashAttribute("status", status);
        return "redirect:/supplier/".concat(String.valueOf(supplier.getSupplierAppId()));

    }

}
