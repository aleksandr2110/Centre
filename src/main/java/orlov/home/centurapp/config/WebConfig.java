package orlov.home.centurapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import orlov.home.centurapp.service.handler.MVCResponseHandler;

@Component
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    MVCResponseHandler mvcResponseHandler;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mvcResponseHandler);
    }
}
