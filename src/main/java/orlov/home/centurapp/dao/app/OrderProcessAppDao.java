package orlov.home.centurapp.dao.app;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.app.OrderProcessApp;
import orlov.home.centurapp.mapper.app.OrderProcessAppRowMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@Repository
public class OrderProcessAppDao implements Dao<OrderProcessApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;

    @Override
    public int save(OrderProcessApp orderProcess) {
        String sql = "insert into order_process_app (supplier_id, start_process, end_process) " +
                "values (:supplierAppId, :startProcess, :endProcess)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(orderProcess), keyHolder, new String[]{"order_process_id"});
        int id = keyHolder.getKey().intValue();
        return id;
    }

    @Override
    public OrderProcessApp getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    public void deleteAll() {
        String sql = "delete from order_process_app";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource());
    }

    @Override
    public OrderProcessApp update(OrderProcessApp orderProcess) {
        return null;
    }

    @Override
    public List<OrderProcessApp> getAll() {
        return null;
    }

    public List<OrderProcessApp> getAllLimited(int begin, int limit) {
        String sql = "select * from order_process_app " +
                "join supplier_app on supplier_app.supplier_app_id = order_process_app.supplier_id " +
                "order by end_process desc " +
                "limit :begin, :limit";
        Map<String, Object> data = new HashMap<>();
        data.put("begin", begin);
        data.put("limit", limit);
        List<OrderProcessApp> orders = jdbcTemplateApp.query(sql, new MapSqlParameterSource(data), new OrderProcessAppRowMapper());
        return orders;
    }
}
