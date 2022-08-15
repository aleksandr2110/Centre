package orlov.home.centurapp.mapper.opencart;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CategoryIdMapper implements RowMapper<Integer> {

    @Override
    public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
        int categoryId = rs.getInt("category_id");
        return categoryId;
    }
}
