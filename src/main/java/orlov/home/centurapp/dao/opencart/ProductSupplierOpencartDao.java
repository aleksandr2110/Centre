package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.ProductSupplierOpencart;
import orlov.home.centurapp.entity.opencart.SupplierOpencart;
import orlov.home.centurapp.mapper.opencart.ProductSupplierOpencartRowMapper;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class ProductSupplierOpencartDao implements Dao<ProductSupplierOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(ProductSupplierOpencart productSupplierOpencart) {
        String sql = "insert into oc_product_supplier (product_id, sup_code, is_pdv, price, currency, availability) " +
                "values (:productId, :supCode, :isPdv, :price, :currency, :availability)";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(productSupplierOpencart));
        return 0;
    }

    public List<ProductSupplierOpencart> getAllProductSupplierBySupCode(String supCode) {
        String sql = "select * from oc_product_supplier " +
                "where sup_code = :supCode";
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("supCode", supCode), new ProductSupplierOpencartRowMapper());
    }

    public ProductSupplierOpencart getAllProductSupplierBySupCodeProductId(int productId, String supCode) {
        String sql = "select * from oc_product_supplier " +
                "where sup_code = :supCode and product_id = :productId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("supCode", supCode);
        params.addValue("productId", productId);
        List<ProductSupplierOpencart> products = jdbcTemplateOpencart.query(sql, params, new ProductSupplierOpencartRowMapper());
        return products.isEmpty() ? null : products.get(0);
    }

    public void updatePDVProductSupplier(SupplierOpencart supplierOpencart) {
        String sql = "update oc_product_supplier " +
                "set is_pdv = :isPdv " +
                "where sup_code = :supCode ";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(supplierOpencart));
    }

    public void updatePriceProductSupplier(ProductSupplierOpencart productSupplierOpencart) {
        String sql = "update oc_product_supplier " +
                "set price = :price " +
                "where product_id = :productId ";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(productSupplierOpencart));
    }

    @Override
    public ProductSupplierOpencart getById(int id) {
        return null;
    }

    //  TODO add to service
    public void deleteByProductSupplier(int productId, String supCode) {
        String sql = "delete\n" +
                "from oc_product_supplier\n" +
                "where product_id = :productId and sup_code = :supCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("productId", productId);
        params.addValue("supCode", supCode);
        jdbcTemplateOpencart.update(sql, params);
    }

    public void deleteProductSupplierByProductId(int productId) {
        String sql = "delete\n" +
                "from oc_product_supplier\n" +
                "where product_id = :productId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("productId", productId);
        jdbcTemplateOpencart.update(sql, params);
    }


    @Override
    public void deleteById(int productId) {

    }


    @Override
    public ProductSupplierOpencart update(ProductSupplierOpencart productSupplierOpencart) {
        String sql = "update oc_product_supplier\n" +
                "set sup_code = :supCode,\n" +
                "    price = :price,\n" +
                "    is_pdv = :isPdv,\n" +
                "    currency = :currency,\n" +
                "    availability = :availability\n" +
                "where product_id = :productId and sup_code = :supCode";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(productSupplierOpencart));
        return productSupplierOpencart;
    }


    @Override
    public List<ProductSupplierOpencart> getAll() {
        return null;
    }
}
