package orlov.home.centurapp.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import orlov.home.centurapp.entity.user.UserApp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Component
@Slf4j
public class MVCResponseHandler implements HandlerInterceptor {
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        try {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (Objects.nonNull(authentication) && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserApp) {
                UserApp userApp = (UserApp) authentication.getPrincipal();
                modelAndView.addObject("userApp", userApp);
                log.info("userApp: {}", userApp);
            }

        } catch (Exception ex){
            log.error("ERROR", ex);
        }


    }
}
