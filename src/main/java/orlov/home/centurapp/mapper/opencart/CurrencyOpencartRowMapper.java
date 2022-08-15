package orlov.home.centurapp.mapper.opencart;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.opencart.CurrencyOpencart;
import orlov.home.centurapp.entity.opencart.SupplierOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CurrencyOpencartRowMapper implements RowMapper<CurrencyOpencart> {
    @Override
    public CurrencyOpencart mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CurrencyOpencart(
                rs.getInt("currency_id"),
                rs.getString("title"),
                rs.getString("code"),
                rs.getString("symbol_left"),
                rs.getString("symbol_right"),
                rs.getString("decimal_place"),
                rs.getBigDecimal("value"),
                rs.getBoolean("status"),
                rs.getTimestamp("date_modified"),
                rs.getString("uuid"));
    }
}
