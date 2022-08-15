package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.ManufacturerDescriptionOpencart;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class ManufacturerDescriptionOpencartDao implements Dao<ManufacturerDescriptionOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(ManufacturerDescriptionOpencart manufacturerDescriptionOpencart) {
        String sql = "insert into oc_manufacturer_description (manufacturer_id, language_id, description, meta_description, meta_keyword, meta_title, meta_h1)" +
                "values (:manufacturerId, :languageId, :description, :metaDescription, :metaKeyword, :metaTitle, :metaH1)";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(manufacturerDescriptionOpencart));
        return 0;
    }

    @Override
    public ManufacturerDescriptionOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public ManufacturerDescriptionOpencart update(ManufacturerDescriptionOpencart manufacturerDescriptionOpencart) {
        return null;
    }

    @Override
    public List<ManufacturerDescriptionOpencart> getAll() {
        return null;
    }
}
