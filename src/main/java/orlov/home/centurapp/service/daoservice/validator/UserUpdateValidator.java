package orlov.home.centurapp.service.daoservice.validator;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import orlov.home.centurapp.entity.user.UserApp;
import orlov.home.centurapp.service.daoservice.app.UserAppService;

@Component
@Slf4j
@AllArgsConstructor
public class UserUpdateValidator implements Validator {

    private final UserAppService userAppService;


    @Override
    public boolean supports(Class<?> clazz) {
        return UserApp.class.equals(clazz);
    }


    @Override
    public void validate(Object target, Errors errors) {
        UserApp user = (UserApp) target;

        if (user.getUserLogin().length() < 4 || user.getUserLogin().length() > 32) {
            errors.rejectValue("userLogin", "", "Логін повинен буде 4 - 32 символи");
        }

        if (user.getUserPassword().length() < 4 || user.getUserPassword().length() > 32) {
            errors.rejectValue("userPassword", "", "Пароль маэ буди 4 - 32 символи");
        }

        if (!user.getUserPasswordConfirm().equals(user.getUserPassword())) {
            errors.rejectValue("userPasswordConfirm", "", "Паролі не співпадаюсь");
        }

    }

}


