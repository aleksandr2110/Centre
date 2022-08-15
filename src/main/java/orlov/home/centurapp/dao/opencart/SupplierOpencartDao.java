package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.SupplierOpencart;
import orlov.home.centurapp.mapper.opencart.SupplierOpencartRowMapper;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class SupplierOpencartDao implements Dao<SupplierOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;


    @Override
    public int save(SupplierOpencart supplierOpencart) {
        return 0;
    }

    @Override
    public SupplierOpencart getById(int id) {
        String sql = "select * from oc_supplier where sup_id = :id";
        List<SupplierOpencart> suppliers = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("id", id), new SupplierOpencartRowMapper());
        return suppliers.size() > 0 ? suppliers.get(0) : null;
    }

    public SupplierOpencart getBySubCode(String subCode) {
        String sql = "select * from oc_supplier where sup_code = :subCode";
        List<SupplierOpencart> suppliers = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("subCode", subCode), new SupplierOpencartRowMapper());
        return suppliers.size() > 0 ? suppliers.get(0) : null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public SupplierOpencart update(SupplierOpencart supplierOpencart) {
        String sql = "update oc_supplier " +
                "set sup_code = :supCode, " +
                "    name = :name, " +
                "    is_pdv = :isPdv, " +
                "    currency = :currency, " +
                "    contacts = :contacts " +
                "where sup_id = :supId";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(supplierOpencart));
        return supplierOpencart;
    }

    @Override
    public List<SupplierOpencart> getAll() {
        String sql = "select * from oc_supplier";
        return jdbcTemplateOpencart.query(sql, new SupplierOpencartRowMapper());
    }


}
