package orlov.home.centurapp.service.appservice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class AnalystService {

    private final AppDaoService appDaoService;
    private final OpencartDaoService opencartDaoService;

    public List<ProductOpencart> productOpencartSameTitle() {

        List<SupplierApp> suppliers = appDaoService.getAllSupplierApp();

        suppliers
                .forEach(s -> {

                    List<ProductOpencart> supplierProducts = opencartDaoService.getSupplierProducts(s.getName());
                    log.info("Supplier: {} has {} products", s.getName(), supplierProducts.size());

                    AtomicInteger countMainProduct = new AtomicInteger();
                    supplierProducts
                            .stream()
                            .peek(p -> {


                                StringBuilder mainDescString = new StringBuilder();
                                p.getProductsDescriptionOpencart()
                                        .forEach(d -> mainDescString
                                                .append("[lang id: ")
                                                .append(d.getLanguageId())
                                                .append(" title: ")
                                                .append(d.getName())
                                                .append("]"));
                                ProductDescriptionOpencart descMain = p.getProductsDescriptionOpencart()
                                        .stream()
                                        .filter(d -> d.getLanguageId() == 3)
                                        .findFirst()
                                        .orElse(null);

                                if (Objects.nonNull(descMain)) {

                                    List<ProductOpencart> productsSameTitle = opencartDaoService.getProductsSameTitle(descMain.getName());

                                    AtomicInteger count = new AtomicInteger();
                                    List<ProductOpencart> collectedProducts = productsSameTitle
                                            .stream()
                                            .filter(pst -> !p.equals(pst))
                                            .collect(Collectors.toList());


                                    if (!collectedProducts.isEmpty()) {
                                        log.info("");
                                        log.info("[{}. main product]\tid: [{}]\tdesc: [{}]\tmodel: [{}]\tsku: [{}]\tjan: [{}]", countMainProduct.addAndGet(1), p.getId(), mainDescString, p.getModel(), p.getSku(), p.getJan());
                                        collectedProducts.forEach(sub -> {
                                            StringBuilder descString = new StringBuilder();
                                            sub.getProductsDescriptionOpencart()
                                                    .forEach(d -> descString
                                                            .append("[lang id: ")
                                                            .append(d.getLanguageId())
                                                            .append(" title: ")
                                                            .append(d.getName())
                                                            .append("]"));
                                            log.info("\t\t\t[{}. same product]\tid: [{}]\tdesc: [{}]\tmodel: [{}]\tsku: [{}]\tjan: [{}]", count.addAndGet(1), sub.getId(), descString, sub.getModel(), sub.getSku(), sub.getJan());
                                        });
                                    }
                                    p.setSameTitleProductsOpencart(collectedProducts);
                                }
                            })
                            .collect(Collectors.toList());


                });

        return null;
    }

}
