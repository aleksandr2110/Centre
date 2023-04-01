package orlov.home.centurapp.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import orlov.home.centurapp.service.handler.DaoExceptionHandlerInit;

import javax.sql.DataSource;

@Configuration
public class DaoConfig {

    @Value("${db.driver}")
    private String driver;
    @Value("${db.url.oc}")
    private String urlOc;
    @Value("${db.url.app}")
    private String urlApp;
    @Value("${db.username}")
    private String username;
    @Value("${db.password}")
    private String password;


    @Bean
    public DataSource dataSourceApp() {
        HikariDataSource ds = new HikariDataSource();
        ds.setMaximumPoolSize(20);
        ds.setConnectionTimeout(600_000 * 2);
        ds.setMaxLifetime(580_000 * 2);
        ds.setDriverClassName(driver);
        ds.setJdbcUrl(urlApp);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }

    @Bean
    public DataSource dataSourceWorker() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(urlApp);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    public DataSource dataSourceOpencart() {
        HikariDataSource ds = new HikariDataSource();
        ds.setMaximumPoolSize(20);
        ds.setConnectionTimeout(600_000 * 2);
        ds.setMaxLifetime(580_000 * 2);
        ds.setDriverClassName(driver);
        ds.setJdbcUrl(urlOc);
        ds.setUsername(username);
        ds.setPassword(password);

        return ds;
    }

    @Bean("jdbcTemplateWorker")
    public JdbcTemplate jdbcTemplateWorker() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSourceWorker());

        return jdbcTemplate;
    }


    @Bean("jdbcTemplateOpencart")
    public NamedParameterJdbcTemplate jdbcTemplateOpencart() {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSourceOpencart());

        return namedParameterJdbcTemplate;
    }

    @Bean("jdbcTemplateApp")
    public NamedParameterJdbcTemplate jdbcTemplateApp() {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSourceApp());

        return namedParameterJdbcTemplate;
    }


}
