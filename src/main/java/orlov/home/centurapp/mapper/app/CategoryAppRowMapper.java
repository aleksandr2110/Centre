package orlov.home.centurapp.mapper.app;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.app.CategoryApp;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CategoryAppRowMapper implements RowMapper<CategoryApp> {
    @Override
    public CategoryApp mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryApp.Builder()
                .withCategoryId(rs.getInt("category_id"))
                .withSupplierId(rs.getInt("supplier_id"))
                .withSupplierTitle(rs.getString("supplier_title"))
                .withOpencartTitle(rs.getString("opencart_title"))
                .withMarkup(rs.getInt("markup"))
                .build();
    }
}
