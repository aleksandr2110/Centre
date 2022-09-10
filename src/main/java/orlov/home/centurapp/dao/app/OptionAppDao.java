package orlov.home.centurapp.dao.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.app.OptionApp;
import orlov.home.centurapp.mapper.app.OptionAppMapper;

import java.util.List;

@Repository
@Slf4j
@AllArgsConstructor
public class OptionAppDao implements Dao<OptionApp> {

    private final NamedParameterJdbcTemplate jdbcTemplateApp;

    @Override
    public int save(OptionApp optionApp) {
        String sql = "insert into option_app (product_profile_id, value_id, option_value, option_price)\n" +
                "values (:productProfileId, :valueId, :optionValue, :optionPrice);";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(optionApp), keyHolder);
        int id = keyHolder.getKey().intValue();
        return id;
    }

    @Override
    public OptionApp getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    public void deleteByProfileId(int id) {
        String sql = "delete from option_app where product_profile_id = :productProfileId";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource("productProfileId", id));
    }


//    TODO
    public void deleteOptionValue(int optionId) {
        String sql = "delete from option_app where option_id = :optionId;";
        jdbcTemplateApp.update(sql, new MapSqlParameterSource("optionId", optionId));
    }

    public List<OptionApp> getOptionsByProductId(int productId) {
        String sql = "select * from option_app where product_profile_id = :productProfileId;";
        return jdbcTemplateApp.query(sql, new MapSqlParameterSource("productProfileId", productId), new OptionAppMapper());

    }

    @Override
    public OptionApp update(OptionApp optionApp) {
        String sql = "update option_app\n" +
                "set option_price = :optionPrice\n" +
                "where option_id = :optionId;";
        jdbcTemplateApp.update(sql, new BeanPropertySqlParameterSource(optionApp));
        return optionApp;
    }

    @Override
    public List<OptionApp> getAll() {
        String sql = "select * from option_app;";
        return jdbcTemplateApp.query(sql, new OptionAppMapper());
    }
}
