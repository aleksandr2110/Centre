package orlov.home.centurapp.mapper.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import orlov.home.centurapp.entity.user.RoleApp;
import orlov.home.centurapp.entity.user.UserApp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UserAppExtractor implements ResultSetExtractor<List<UserApp>> {


    @Override
    public List<UserApp> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<UserApp> users = new ArrayList<>();
        while (rs.next()) {
            UserApp user = new UserApp();
            int userId = rs.getInt("u.user_id");
            String userName = rs.getString("user_name");
            String userLogin = rs.getString("user_login");
            String userPassword = rs.getString("user_password");
            user.setUserId(userId);
            user.setUserFirstName(userName);
            user.setUserLogin(userLogin);
            user.setUserPassword(userPassword);

            int roleId = rs.getInt("r.role_id");
            String roleName = rs.getString("role_name");
            RoleApp role = new RoleApp(roleId, roleName);

            if (users.contains(user)) {
                UserApp userApp = users.get(users.indexOf(user));
                userApp.getRoles().add(role);
            } else {
                user.getRoles().add(role);
                users.add(user);
            }
        }
        return users;
    }
}
