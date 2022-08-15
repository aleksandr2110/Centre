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
import orlov.home.centurapp.entity.opencart.ManufacturerOpencart;
import orlov.home.centurapp.mapper.opencart.ManufacturerOpencartRpwMapper;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class ManufacturerOpencartDao implements Dao<ManufacturerOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;


    @Override
    public int save(ManufacturerOpencart manufacturerOpencart) {
        String sql = "insert into oc_manufacturer (name, image, sort_order, noindex, uuid)" +
                "values (:name, :image, :sortOrder, :noindex ,:uuid)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(manufacturerOpencart), keyHolder);
        int id = keyHolder.getKey().intValue();
        return id;
    }

    @Override
    public ManufacturerOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    public ManufacturerOpencart getByName(String name) {
        String sql = "select * from oc_manufacturer where name = :name";
        List<ManufacturerOpencart> manufacturersOpencart = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("name", name), new ManufacturerOpencartRpwMapper());
        if (manufacturersOpencart.size() > 0) {
            ManufacturerOpencart manufacturerOpencart = manufacturersOpencart.get(0);
            return manufacturerOpencart;
        }
        return null;
    }

    @Override
    public ManufacturerOpencart update(ManufacturerOpencart manufacturerOpencart) {
        return null;
    }

    @Override
    public List<ManufacturerOpencart> getAll() {
        String sql = "select  * from oc_manufacturer";
        return jdbcTemplateOpencart.query(sql, new ManufacturerOpencartRpwMapper());
    }
}
