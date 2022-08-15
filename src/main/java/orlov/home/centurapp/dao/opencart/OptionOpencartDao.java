package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.OptionOpencart;
import orlov.home.centurapp.mapper.opencart.OptionOpencartExtractor;
import orlov.home.centurapp.util.OCConstant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@AllArgsConstructor
@Slf4j
public class OptionOpencartDao implements Dao<OptionOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(OptionOpencart optionOpencart) {
        String sql = "insert into oc_option (type, sort_order, uuid) " +
                "values (:type, :sortOrder, :uuid);";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(optionOpencart), keyHolder);
        int optionsId = keyHolder.getKey().intValue();
        return optionsId;
    }

    @Override
    public OptionOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int optionId) {
        String sql = "delete from oc_option where option_id = :optionId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("optionId", optionId));
    }




    @Override
    public OptionOpencart update(OptionOpencart optionOpencart) {
        return null;
    }

    @Override
    public List<OptionOpencart> getAll() {
        String sql = "select o.option_id, o.type, o.sort_order, o.uuid, od.option_id, od.language_id, od.name, v.option_value_id, v.option_id, v.image, v.sort_order, v.uuid, vd.option_value_id, vd.language_id, vd.option_id, vd.name " +
                "from oc_option o " +
                "left join oc_option_description od on od.option_id = o.option_id " +
                "left join oc_option_value v on v.option_id = o.option_id " +
                "left join oc_option_value_description vd on v.option_value_id = vd.option_value_id " +
                "where od.language_id = :languageId and vd.language_id = :languageId;";
        Map<String, Object> data = new HashMap<>();
        data.put("languageId", OCConstant.UA_LANGUAGE_ID);
        List<OptionOpencart> query = jdbcTemplateOpencart.query(sql, data, new OptionOpencartExtractor());
        return query;
    }
}
