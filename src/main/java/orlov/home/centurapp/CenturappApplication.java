package orlov.home.centurapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@SpringBootApplication(
        exclude = {SecurityAutoConfiguration.class})
public class CenturappApplication {

    public static void main(String[] args) {
        SpringApplication.run(CenturappApplication.class, args);
    }

}
