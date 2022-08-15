package orlov.home.centurapp.mapper.app;

import org.springframework.jdbc.core.RowMapper;
import orlov.home.centurapp.entity.app.OrderProcessApp;
import orlov.home.centurapp.entity.app.SupplierApp;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderProcessAppRowMapper implements RowMapper<OrderProcessApp> {

    @Override
    public OrderProcessApp mapRow(ResultSet rs, int rowNum) throws SQLException {
        SupplierApp supplierApp = new SupplierApp();
        supplierApp.setSupplierAppId(rs.getInt("supplier_app_id"));
        supplierApp.setName(rs.getString("name"));
        supplierApp.setUrl(rs.getString("url"));
        supplierApp.setDisplayName(rs.getString("display_name"));
        supplierApp.setMarkup(rs.getInt("markup"));

        OrderProcessApp order = new OrderProcessApp.Builder()
                .withOrderProcessId(rs.getInt("order_process_id"))
                .withSupplierApp(supplierApp)
                .withSupplierAppId(supplierApp.getSupplierAppId())
                .withStartProcess(rs.getTimestamp("start_process"))
                .withEndProcess(rs.getTimestamp("end_process"))
                .build();
        return order;
    }
}
