package orlov.home.centurapp.config;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.CharacterEncodingFilter;
import orlov.home.centurapp.entity.user.UserApp;
import orlov.home.centurapp.service.daoservice.app.UserAppService;
import orlov.home.centurapp.service.security.AuthenticationProviderImpl;

import javax.servlet.Filter;

@Configuration
@EnableWebSecurity
@ComponentScan("orlov.home.centurapp")
public class SecurityConfigurer extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthenticationProviderImpl authenticationProviderImpl;
    @Autowired
    private UserAppService userAppService;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**", "/images/**", "/css/**", "/js/**");
    }


//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.authenticationProvider(authenticationProviderImpl);
//    }

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authenticationProvider(authenticationProviderImpl);
        http
                .authorizeRequests()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/**").access("hasRole('USER')")
                .and()
                .formLogin() // (5)
                .loginPage("/login_t") // (5)
                .usernameParameter("userLogin")
                .passwordParameter("userPassword")
                .defaultSuccessUrl("/")
                .failureUrl("/login_t?errorLog=true")
                .permitAll()
                .and()
                .logout() // (6)
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login_t")
                .clearAuthentication(true)
                .and()
                .rememberMe()
                .userDetailsService(userAppService)
                .and()
                .httpBasic();
    }

    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        registrationBean.setFilter(characterEncodingFilter);
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        registrationBean.setOrder(Integer.MIN_VALUE);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }


}
