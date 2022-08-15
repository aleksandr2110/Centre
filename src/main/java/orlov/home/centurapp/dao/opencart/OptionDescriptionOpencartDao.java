package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.OptionDescriptionOpencart;

import java.util.List;

@Repository
@Slf4j
@AllArgsConstructor
public class OptionDescriptionOpencartDao implements Dao<OptionDescriptionOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(OptionDescriptionOpencart optionDescriptionOpencart) {
        String sql = "insert into oc_option_description (option_id, language_id, name) " +
                "values (:optionId, :languageId, :name);";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(optionDescriptionOpencart));
        return 0;
    }

    @Override
    public OptionDescriptionOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public OptionDescriptionOpencart update(OptionDescriptionOpencart optionDescriptionOpencart) {
        return null;
    }

    @Override
    public List<OptionDescriptionOpencart> getAll() {
        return null;
    }
}
