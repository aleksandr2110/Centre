package orlov.home.centurapp.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import orlov.home.centurapp.entity.user.UserApp;
import orlov.home.centurapp.service.daoservice.app.UserAppService;
import orlov.home.centurapp.service.daoservice.validator.UserValidator;
import orlov.home.centurapp.service.security.AuthenticationProviderImpl;

@Slf4j
@AllArgsConstructor
@Controller
@RequestMapping("/login_t")
public class LoginController {

    private final UserValidator userValidator;



    @GetMapping
    public String login(Model model) {
        model.addAttribute("user", new UserApp());
        return "t_login";
    }

//    @PostMapping
//    public String loginPost(@ModelAttribute("userApp") UserApp userApp,
//                            BindingResult bindingResult,
//                            Model model) {
//        userValidator.validate(userApp, bindingResult);
//        log.info("Errors count: {}", bindingResult.getErrorCount());
//        if (bindingResult.hasErrors()) {
//            log.info("Errors count: {}", bindingResult.getErrorCount());
//            model.addAttribute("userLogin", userApp);
//            return "t_login";
//        }
//
//        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userApp.getUserLogin(), userApp.getPassword());
//        Authentication authenticate = authenticationProviderImpl.authenticate(token);
//        SecurityContext context = SecurityContextHolder.getContext();
//        context.setAuthentication(authenticate);
//        return "t_index";
//    }


}
