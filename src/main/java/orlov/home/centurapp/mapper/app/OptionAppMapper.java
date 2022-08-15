package orlov.home.centurapp.mapper.app;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.app.OptionApp;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OptionAppMapper implements RowMapper<OptionApp> {
    @Override
    public OptionApp mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OptionApp(rs.getInt("option_id"), rs.getInt("product_profile_id"),rs.getInt("value_id"), rs.getString("option_value"), rs.getBigDecimal("option_price"));
    }
}
