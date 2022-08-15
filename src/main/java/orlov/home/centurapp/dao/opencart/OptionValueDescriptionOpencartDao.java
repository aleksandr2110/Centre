package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.OptionValueDescriptionOpencart;

import java.util.List;

@Repository
@Slf4j
@AllArgsConstructor
public class OptionValueDescriptionOpencartDao implements Dao<OptionValueDescriptionOpencart> {
    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(OptionValueDescriptionOpencart optionValueDescriptionOpencart) {
        String sql = "insert into oc_option_value_description (option_value_id, language_id, option_id, name)\n" +
                "values (:optionValueId, :languageId, :optionId, :name);";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(optionValueDescriptionOpencart));
        return 0;
    }

    @Override
    public OptionValueDescriptionOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public OptionValueDescriptionOpencart update(OptionValueDescriptionOpencart optionValueDescriptionOpencart) {
        return null;
    }

    @Override
    public List<OptionValueDescriptionOpencart> getAll() {
        return null;
    }
}
