package orlov.home.centurapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import orlov.home.centurapp.entity.user.UserApp;

import java.util.Objects;

@Controller
@RequestMapping("/")
@Slf4j
public class MainController {

    @GetMapping
    public String home() {
        return "t_index";
    }
}
