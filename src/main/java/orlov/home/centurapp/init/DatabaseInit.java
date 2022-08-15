package orlov.home.centurapp.init;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import orlov.home.centurapp.entity.user.RoleApp;
import orlov.home.centurapp.entity.user.UserApp;
import orlov.home.centurapp.service.daoservice.app.UserAppService;

import java.util.*;

@Component
@AllArgsConstructor
@Slf4j
public class DatabaseInit implements ApplicationListener<ContextRefreshedEvent> {

    private final JdbcTemplate jdbcTemplateWorker;
    private final UserAppService userAppService;


    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        String sqlCreateTableProductAttributeApp = "create table if not exists product_attribute_app\n" +
                "(\n" +
                "    product_profile_id int          not null,\n" +
                "    attribute_id       int          not null,\n" +
                "    attribute_value    text not null,\n" +
                "    primary key (product_profile_id, attribute_id),\n" +
                "    foreign key (product_profile_id) references product_profile_app (product_profile_id),\n" +
                "    foreign key (attribute_id) references attribute_app (attribute_id)\n" +
                ");";
        jdbcTemplateWorker.execute(sqlCreateTableProductAttributeApp);
        log.info("Table product_attribute_app is created");

        String sqlCreateTableOptionApp = "create table if not exists option_app (\n" +
                "    option_id          int auto_increment primary key,\n" +
                "    product_profile_id int            NOT NULL,\n" +
                "    value_id        int   NOT NULL,\n" +
                "    option_value       VARCHAR(255)   NOT NULL,\n" +
                "    option_price       decimal(15, 4) NOT NULL,\n" +
                "    constraint option_product_profile foreign key (product_profile_id) references product_profile_app (product_profile_id));";
        jdbcTemplateWorker.execute(sqlCreateTableOptionApp);
        log.info("Table option is created");

        String sqlCreateUserAppTable = "create table if not exists user_app " +
                "(\n" +
                "    user_id       int auto_increment primary key,\n" +
                "    user_name     varchar(255) not null,\n" +
                "    user_login    varchar(255) not null,\n" +
                "    user_password varchar(255) not null\n" +
                ")";
        jdbcTemplateWorker.execute(sqlCreateUserAppTable);

        String sqlCreateRoleAppTable = "create table if not exists role_app\n" +
                "(\n" +
                "    role_id   int auto_increment primary key,\n" +
                "    role_name varchar(255) not null\n" +
                ");";
        jdbcTemplateWorker.execute(sqlCreateRoleAppTable);

        String sqlCreateUserRoleTable = "create table if not exists user_role_app\n" +
                "(\n" +
                "    user_id int not null,\n" +
                "    role_id int not null,\n" +
                "    foreign key (user_id) references user_app (user_id) on delete restrict on update cascade,\n" +
                "    foreign key (role_id) references role_app (role_id) on delete restrict on update cascade,\n" +
                "    primary key (user_id, role_id)\n" +
                ");";

        jdbcTemplateWorker.execute(sqlCreateUserRoleTable);

        RoleApp roleAppUser = new RoleApp();
        roleAppUser.setRoleName("ROLE_USER");
        RoleApp roleAppAdmin = new RoleApp();
        roleAppAdmin.setRoleName("ROLE_ADMIN");
        RoleApp userRole = userAppService.getRoleByName(roleAppUser.getRoleName());
        RoleApp adminRole = userAppService.getRoleByName(roleAppAdmin.getRoleName());

        if (Objects.isNull(userRole)) {
            userRole = userAppService.saveRole(roleAppUser);
        }
        if (Objects.isNull(adminRole))
            adminRole = userAppService.saveRole(roleAppAdmin);

        UserApp mainUser = new UserApp();
        mainUser.setUserLogin("centur");
        mainUser.setUserPassword("centur_409759814");
        mainUser.setUserFirstName("Сергій");

        UserApp byLogin = userAppService.getByLogin(mainUser.getUserLogin());

        if (Objects.isNull(byLogin)) {
            mainUser.setRoles(new HashSet<>(Arrays.asList(userRole, adminRole)));
            userAppService.save(mainUser);
        } else {
            Set<RoleApp> roles = byLogin.getRoles();

           if (!roles.contains(userRole)){
               userAppService.saveUserRole(mainUser.getUserId(), userRole.getRoleId());
           }

            if (!roles.contains(adminRole)){
                userAppService.saveUserRole(mainUser.getUserId(), adminRole.getRoleId());
            }
        }


    }
}
