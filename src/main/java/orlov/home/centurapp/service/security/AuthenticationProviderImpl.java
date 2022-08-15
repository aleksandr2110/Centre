package orlov.home.centurapp.service.security;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import orlov.home.centurapp.entity.user.UserApp;
import orlov.home.centurapp.service.daoservice.app.UserAppService;

import java.util.Objects;

@Component
@Slf4j
@AllArgsConstructor
public class AuthenticationProviderImpl implements AuthenticationProvider {

    private final UserAppService userAppService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String login = authentication.getName();
        String password = authentication.getCredentials().toString();

        UserApp userApp = userAppService.loadUserByUsername(login);
        log.info("Login: {}", login);
        log.info("Password: {}", password);

        if (Objects.isNull(userApp) || !userApp.getUsername().equalsIgnoreCase(login)) {
            log.info("BadCredentialsException Username not found");
            throw new BadCredentialsException("Username not found.");
        }

        boolean matchesPassword = password.equals(userApp.getPassword());

        if (!matchesPassword) {
            log.info("BadCredentialsException Wrong password");
            throw new BadCredentialsException("Wrong password.");
        }

//        return new UsernamePasswordAuthenticationToken(userApp, userApp.getPassword(), userApp.getAuthorities());
        return new UsernamePasswordAuthenticationToken(userApp, userApp.getPassword(), userApp.getAuthorities());

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
