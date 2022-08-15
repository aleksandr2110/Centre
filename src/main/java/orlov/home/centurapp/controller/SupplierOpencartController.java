package orlov.home.centurapp.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orlov.home.centurapp.entity.app.ProductProfileApp;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.app.SupplierAppService;
import orlov.home.centurapp.service.daoservice.opencart.CurrencyOpencartService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;
import orlov.home.centurapp.service.daoservice.opencart.ProductOpencartService;
import orlov.home.centurapp.service.daoservice.opencart.SupplierOpencartService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/supplieroc")
@AllArgsConstructor
@Slf4j
public class SupplierOpencartController {

    private final SupplierOpencartService supplierOpencartService;
    private final SupplierAppService supplierAppService;
    private final ProductOpencartService productOpencartService;
    private final CurrencyOpencartService currencyOpencartService;
    private final OpencartDaoService opencartDaoService;
    private final AppDaoService appDaoService;


    @GetMapping
    public String getSuppliers(Model model) {
        List<SupplierOpencart> suppliersoc = supplierOpencartService.getAll()
                .stream()
                .sorted((a, b) -> {
                    boolean matchesA = a.getSupCode().replaceAll("\\D", "").matches("\\d+");
                    boolean matchesB = b.getSupCode().replaceAll("\\D", "").matches("\\d+");
                    int numberA = matchesA ? Integer.parseInt(a.getSupCode().replaceAll("\\D", "")) : -1;
                    int numberB = matchesB ? Integer.parseInt(b.getSupCode().replaceAll("\\D", "")) : -1;
                    return Integer.compare(numberA, numberB);
                })
                .collect(Collectors.toList());
        model.addAttribute("suppliersoc", suppliersoc);
        model.addAttribute("supoc", new SupplierOpencart());
        return "t_supplier_oc";
    }

    @PostMapping("product/add")
    public String addProduct(Model model, @RequestParam("productModel") String productModel,
                             @RequestParam("supCode") String supCode,
                             RedirectAttributes redirectAttributes) {
        SupplierOpencart supplier = opencartDaoService.getSupplierOpencartBySubCode(supCode);
        log.info("Product model: {}", productModel);
        log.info("Sup code: {}", supCode);
        ProductOpencart product = opencartDaoService.getProductOpencartByModel(productModel);
        String message = "";


        if (Objects.isNull(product)) {
            log.info("Model {} not found", productModel);
            message = productModel + " - модель не знайдена";
        } else {
            ProductSupplierOpencart productSupplier = opencartDaoService.getAllProductSupplierBySupCodeProductId(product.getId(), supCode);
            if (Objects.nonNull(productSupplier)) {
                log.info("Model {} already exist", productModel);
                message = productModel + " - вже э у постачальника";
            } else {
                log.info("Model {} new product_supplier", productModel);
                BigDecimal price = new BigDecimal("0");

                SupplierApp supplierApp = appDaoService.getSupplierAppByDisplayName(supCode);
                if (Objects.nonNull(supplierApp)) {
                    String sku = product.getSku();
                    ProductProfileApp productProfile = appDaoService.getProductProfileBySkyJan(sku, supplierApp.getSupplierAppId());
                    if (Objects.nonNull(productProfile)) {
                        price = productProfile.getPrice();
                    }
                }

                ProductSupplierOpencart newProductSupplier = new ProductSupplierOpencart();
                newProductSupplier.setProductId(product.getId());
                newProductSupplier.setSupCode(supCode);
                newProductSupplier.setPrice(price);
                newProductSupplier.setIsPdv(supplier.getIsPdv());
                newProductSupplier.setCurrency(supplier.getCurrency());
                newProductSupplier.setAvailability("");
                log.info("New Product Supplier {} ", newProductSupplier);
                opencartDaoService.saveProductSupplierOpencart(newProductSupplier);
                message = productModel + " - товар доданий";
            }
        }

        redirectAttributes.addFlashAttribute("mess", message);
        return "redirect:/supplieroc/products/" + supplier.getSupId();
    }

    @GetMapping("/product/remove/{supId}/{id}")
    public String removeProductOpencart(Model model, @PathVariable("id") int productId, @PathVariable("supId") int supId) {
        SupplierOpencart supplier = opencartDaoService.getSupplierOpencartBySupId(supId);
        opencartDaoService.deleteByProductSupplier(productId, supplier.getSupCode());

        return "redirect:/supplieroc/products/" + supplier.getSupId();
    }


    @GetMapping("/product/update/{supId}/{id}")
    public String updateProductOpencart(Model model, @PathVariable("id") int productId, @PathVariable("supId") int supId) {
        SupplierOpencart supplier = opencartDaoService.getSupplierOpencartBySupId(supId);
        ProductSupplierOpencart product = opencartDaoService.getAllProductSupplierBySupCodeProductId(productId, supplier.getSupCode());
        ProductOpencart productOpencartById = opencartDaoService.getProductOpencartById(product.getProductId());
        if (Objects.nonNull(productOpencartById)) {
            product.setModel(productOpencartById.getModel());
        }
        log.info("Product: {}", product);
        model.addAttribute("product", product);
        return "t_supplier_oc_product_update";
    }

    @PostMapping("/product/update")
    public String updateProductOpencartPost(@ModelAttribute("product") ProductSupplierOpencart product) {
        log.info("Product sup update: {}", product);
        SupplierOpencart supplier = opencartDaoService.getSupplierOpencartBySubCode(product.getSupCode());
        opencartDaoService.updateProductSupplierOpencart(product);
        return "redirect:/supplieroc/products/" + supplier.getSupId();
    }

    @GetMapping("/products/{id}")
    public String supplierProductsOpencart(Model model, @PathVariable("id") int supId) {
        SupplierOpencart supplier = opencartDaoService.getSupplierOpencartBySupId(supId);
        List<ProductSupplierOpencart> allProductSupplierOpencartBySupCode = opencartDaoService.getAllProductSupplierOpencartBySupCode(supplier.getSupCode());
        allProductSupplierOpencartBySupCode.forEach(p -> {
            int productId = p.getProductId();
            ProductOpencart productOpencartById = opencartDaoService.getProductOpencartById(productId);
            if (Objects.nonNull(productOpencartById)) {
                p.setModel(productOpencartById.getModel());
            }
        });

        model.addAttribute("products", allProductSupplierOpencartBySupCode);
        model.addAttribute("supId", supplier.getSupId());
        model.addAttribute("supCode", supplier.getSupCode());
        return "t_supplier_oc_product";
    }

    @PostMapping
    public String updateProductStatus(@ModelAttribute("supoc") SupplierOpencart supplierOpencart) {
        String statusResult = supplierOpencart.getStatus();
        SupplierApp supplierApp = supplierAppService.getByDisplayName(supplierOpencart.getSupCode());
        List<Integer> scrapedProductsId = new ArrayList<>();
        if (Objects.nonNull(supplierApp)) {
            List<Integer> ids = productOpencartService.getAllBySupplier(supplierApp.getName())
                    .stream()
                    .map(ProductOpencart::getId)
                    .collect(Collectors.toList());
            scrapedProductsId.addAll(ids);
        }

        if (statusResult.equals("0") || statusResult.equals("1")) {
            List<ProductOpencart> products = productOpencartService.getAllProductSupplierBySupCode(supplierOpencart.getSupCode())
                    .stream()
                    .filter(ps -> !scrapedProductsId.contains(ps.getProductId()))
                    .map(s -> {
                        log.info("s: {}", s);
                        boolean status = statusResult.equals("1");
                        return new ProductOpencart.Builder()
                                .withProductId(s.getProductId())
                                .withStatus(status)
                                .build();
                    })
                    .collect(Collectors.toList());

            products.forEach(productOpencartService::updateStatus);
        }

        return "redirect:/";
    }

    @GetMapping("/update/{supId}")
    public String getSupplier(@PathVariable int supId,
                              Model model) {
        log.info("Sup code: {}", supId);
        List<CurrencyOpencart> currencies = currencyOpencartService.getAllCurrency();
        SupplierOpencart supplierOpencart = supplierOpencartService.getBySupId(supId);
        supplierOpencart.setCurrencies(currencies);
        log.info("Supplier: {}", supplierOpencart);
        log.info("currencies: {}", currencies);
        model.addAttribute("supupd", supplierOpencart);
        return "t_supplier_data_update";
    }

    @PostMapping("/update")
    public String updateSupplier(@ModelAttribute("supupd") SupplierOpencart supplierOpencart,
                                 RedirectAttributes redirectAttributes) {
        log.info("Suppler new : {}", supplierOpencart);

        SupplierOpencart update = supplierOpencartService.update(supplierOpencart);
        productOpencartService.updatePDVProductSupplier(supplierOpencart);
        redirectAttributes.addFlashAttribute("supupd", update);
        return "redirect:/supplieroc";
    }


}
