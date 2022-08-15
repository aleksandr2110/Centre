package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.OptionValueOpencart;

import java.util.List;

@Repository
@Slf4j
@AllArgsConstructor
public class OptionValueOpencartDao implements Dao<OptionValueOpencart> {
    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(OptionValueOpencart optionValueOpencart) {
        log.info("Save option value: {}", optionValueOpencart);
        String sql = "insert into oc_option_value (option_id, image, sort_order, uuid)\n" +
                "values (:optionId, :image, :sortOrder, :uuid);";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(optionValueOpencart), keyHolder);
        int id = keyHolder.getKey().intValue();
        return id;
    }

    @Override
    public OptionValueOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public OptionValueOpencart update(OptionValueOpencart optionValueOpencart) {
        String sql = "update oc_option_value\n" +
                "set image = :image\n" +
                "where option_value_id = :optionValueId;";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(optionValueOpencart));
        return optionValueOpencart;
    }

    @Override
    public List<OptionValueOpencart> getAll() {
        return null;
    }
}
