package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.entity.app.OrderProcessApp;
import orlov.home.centurapp.entity.app.ProductApp;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class OrderAppService {

    private final SupplierAppService supplierAppService;
    private final OrderProcessAppService orderProcessService;
    private final ProductAppService productAppService;

    public void saveOrderData(OrderProcessApp orderProcessApp) {
        OrderProcessApp order = orderProcessService.save(orderProcessApp);
        List<ProductApp> newPriceProduct = order.getNewPriceProduct();
        List<ProductApp> againProductApp = order.getAgainProductApp();
        List<ProductApp> newProduct = order.getNewProduct();
        List<ProductApp> olrProduct = order.getOlrProduct();

        newPriceProduct = newPriceProduct
                .stream()
                .peek(p ->   p.setOrderProcessId(order.getOrderProcessId()))
                .collect(Collectors.toList());

        productAppService.saveBatch(newPriceProduct);

        againProductApp = againProductApp.stream()
                .peek(p ->   p.setOrderProcessId(order.getOrderProcessId()))
                .collect(Collectors.toList());

        productAppService.saveBatch(againProductApp);

        newProduct= newProduct.stream()
                .peek(p ->   p.setOrderProcessId(order.getOrderProcessId()))
                .collect(Collectors.toList());
        productAppService.saveBatch(newProduct);

        olrProduct = olrProduct.stream()
                .peek(p ->   p.setOrderProcessId(order.getOrderProcessId()))
                .collect(Collectors.toList());
        productAppService.saveBatch(olrProduct);


    }





}
