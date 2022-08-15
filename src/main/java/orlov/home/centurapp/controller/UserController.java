package orlov.home.centurapp.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import orlov.home.centurapp.entity.user.RoleApp;
import orlov.home.centurapp.entity.user.UserApp;
import orlov.home.centurapp.service.daoservice.app.UserAppService;
import orlov.home.centurapp.service.daoservice.validator.UserUpdateValidator;
import orlov.home.centurapp.service.daoservice.validator.UserValidator;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/user")
@Slf4j
@AllArgsConstructor
public class UserController {

    private final UserAppService userAppService;
    private final UserValidator userValidator;
    private final UserUpdateValidator userUpdateValidator;


    @GetMapping
    public String users(@ModelAttribute("userApp") UserApp userApp, Model model) {
        List<UserApp> users = userAppService.getAll();
        model.addAttribute("users", users);
        return "t_user";
    }


    @GetMapping("/update/{userId}")
    public String update(@ModelAttribute("userUpdate") UserApp userApp, @PathVariable("userId") int userId, Model model) {
        UserApp byId = userAppService.getById(userId);
        if (Objects.nonNull(byId)) {
            String role = byId.getRoles().size() == 1 ? "U" : "A";
            byId.setRole(role);
            byId.setUserPasswordConfirm(byId.getUserPassword());
            log.info("User for update: {}", byId);
            model.addAttribute("userUpdate", byId);
            return "t_user_update";
        } else
            return "t_user";
    }

    @PostMapping("/update")
    public String updatePost(@ModelAttribute("userUpdate") UserApp userApp, BindingResult bindingResult, Model model) {

        log.info("Update user: {}", userApp);
        userUpdateValidator.validate(userApp, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("userReg", userApp);
            return "t_user_update";
        }

        String newRole = userApp.getRole();
        UserApp oldUser = userAppService.getById(userApp.getUserId());

        userApp.setUserId(oldUser.getUserId());
        userAppService.update(userApp);
        if (newRole.equals("A") && oldUser.getRoles().size() == 1) {
            RoleApp roleAdmin = userAppService.getRoleByName("ROLE_ADMIN");
            userAppService.saveUserRole(userApp.getUserId(), roleAdmin.getRoleId());
        } else if (newRole.equals("U") && oldUser.getRoles().size() == 2) {
            RoleApp roleAdmin = userAppService.getRoleByName("ROLE_ADMIN");
            userAppService.deleteUserRoleByUserRoleId(userApp.getUserId(), roleAdmin.getRoleId());
        }


        return "redirect:/user";
    }

    @GetMapping("/remove/{userId}")
    public String remove(@ModelAttribute("userApp") UserApp userApp, Model model, @PathVariable("userId") int userId) {
        userAppService.deleteById(userId);
        return "redirect:/user";
    }

    @GetMapping("/create")
    public String create(Model model) {
        UserApp userApp = new UserApp();
        log.info("User reg: {}", userApp);
        model.addAttribute("userReg", userApp);
        return "t_registration";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute("userReg") UserApp userApp, BindingResult bindingResult, Model model) {
        log.info("Reg user: {}", userApp);

        userValidator.validate(userApp, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("userReg", userApp);
            return "t_registration";
        }

        String role = userApp.getRole();

        if (role.equals("U")) {
            RoleApp roleUser = userAppService.getRoleByName("ROLE_USER");
            userApp.setRoles(new HashSet<>(Arrays.asList(roleUser)));
        } else {
            RoleApp roleUser = userAppService.getRoleByName("ROLE_USER");
            RoleApp roleAdmin = userAppService.getRoleByName("ROLE_ADMIN");
            userApp.setRoles(new HashSet<>(Arrays.asList(roleUser, roleAdmin)));
        }

        userAppService.save(userApp);

        log.info("User reg: {}", userApp);
        return "redirect:/user";
    }


}
