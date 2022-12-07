package orlov.home.centurapp.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import orlov.home.centurapp.dao.app.OrderProcessAppDao;
import orlov.home.centurapp.dao.app.ProductAppDao;
import orlov.home.centurapp.entity.app.OrderProcessApp;
import orlov.home.centurapp.entity.app.ProductApp;
import orlov.home.centurapp.service.daoservice.app.AppDaoService;
import orlov.home.centurapp.service.daoservice.app.OrderProcessAppService;
import orlov.home.centurapp.service.daoservice.app.ProductAppService;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Controller
@RequestMapping("/order")
public class OrderController {

    private final AppDaoService appDaoService;


    @GetMapping
    public String parserOrder(Model model) {
        List<OrderProcessApp> orders = appDaoService.getAllOrderProcessAppLimited(0, 100);
        orders
                .forEach(o -> {
//                    List<ProductApp> aNew = productAppService.getByOrderAndStatus(o.getOrderProcessId(), "new");
                    List<ProductApp> aNew = appDaoService.getProductAppByOrderAndStatus(o.getOrderProcessId(), "new");
                    o.getNewProduct().addAll(aNew);
                    List<ProductApp> price = appDaoService.getProductAppByOrderAndStatus(o.getOrderProcessId(), "price");
                    o.getNewPriceProduct().addAll(price);
                });

        model.addAttribute("orders", orders);
        return "t_order";
    }

    @GetMapping("/product/new/{orderId}")
    public String parserOrderNewProduct(@PathVariable("orderId") int orderId, Model model) {
        List<ProductApp> newProducts = appDaoService.getProductAppByOrderAndStatus(orderId, "new");
        model.addAttribute("products", newProducts);
        return "t_nproduct";
    }

    @GetMapping("/product/price/{orderId}")
    public String parserProductNewProductPrice(@PathVariable("orderId") int orderId, Model model) {
        List<ProductApp> newPriceProducts = appDaoService.getProductAppByOrderAndStatus(orderId, "price");
        model.addAttribute("products", newPriceProducts);
        return "t_nprice";
    }


}
