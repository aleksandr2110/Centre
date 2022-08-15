package orlov.home.centurapp.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import orlov.home.centurapp.dto.AttributeFrontDto;
import orlov.home.centurapp.dto.ProductInfoDto;
import orlov.home.centurapp.entity.app.AttributeApp;
import orlov.home.centurapp.entity.opencart.AttributeOpencart;
import orlov.home.centurapp.service.daoservice.opencart.OpencartDaoService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Controller
@RequestMapping("/opencart")
public class OpencartController {

    private final OpencartDaoService opencartDaoService;

    @GetMapping
    public String opencartData() {
        return "t_oc_data";
    }

    @GetMapping(value = {"/attr"})
    public String opencartDataAttr(Model model, @RequestParam(required = false, defaultValue = "") String search) {
        List<AttributeFrontDto> attrs;
        if (search.isEmpty()) {
           attrs = opencartDaoService.getAllAttributeOpencartWithDesc()
                    .stream()
                    .map(a -> new AttributeFrontDto(a.getAttributeId(), a.getDescriptions().get(0).getName(), null))
                    .sorted(Comparator.comparing(AttributeFrontDto::getName))
                    .collect(Collectors.toList());
        } else {
            attrs = opencartDaoService.getAllAttributeBySearchWithDesc(search)
                    .stream()
                    .map(a -> new AttributeFrontDto(a.getAttributeId(), a.getDescriptions().get(0).getName(), null))
                    .sorted(Comparator.comparing(AttributeFrontDto::getName))
                    .collect(Collectors.toList());
        }
        log.info("Attr size: {}", attrs.size());
        model.addAttribute("attr", attrs);
        return "t_oc_data_attr";
    }

    @GetMapping("/attr/{attrid}")
    public String opencartDataAttr(Model model, @PathVariable("attrid") int attrId) {
        List<ProductInfoDto> models = opencartDaoService.getAllModelByAttributeId(attrId)
                .stream()
//                .filter(p -> Objects.nonNull(p.getModel()) && Objects.nonNull(p.))
                .sorted(Comparator.comparing(ProductInfoDto::getModel))
                .collect(Collectors.toList());
        log.info("Attr size: {}", models.size());
        model.addAttribute("models", models);
        return "t_oc_data_attr_prod";
    }

}
