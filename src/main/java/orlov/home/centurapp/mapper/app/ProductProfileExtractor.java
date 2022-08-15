package orlov.home.centurapp.mapper.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import orlov.home.centurapp.entity.app.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ProductProfileExtractor implements ResultSetExtractor<List<ProductProfileApp>> {
    @Override
    public List<ProductProfileApp> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<ProductProfileApp> profilesApp = new ArrayList<>();

        while (rs.next()) {
            SupplierApp supplierApp = new SupplierApp.Builder()
                    .withSupplierAppId(rs.getInt("supplier_app_id"))
                    .withUrl(rs.getString("sa.url"))
                    .withName(rs.getString("name"))
                    .withDisplayName(rs.getString("display_name"))
                    .withMarkup(rs.getInt("sa.markup"))
                    .build();


            CategoryApp categoryApp = new CategoryApp.Builder()
                    .withCategoryId(rs.getInt("ca.category_id"))
                    .withSupplierId(rs.getInt("ca.supplier_id"))
                    .withSupplierTitle(rs.getString("ca.supplier_title"))
                    .withOpencartTitle(rs.getString("ca.opencart_title"))
                    .withMarkup(rs.getInt("ca.markup"))
                    .build();

            ManufacturerApp manufacturerApp = new ManufacturerApp.Builder()
                    .withManufacturerId(rs.getInt("ma.manufacturer_id"))
                    .withSupplierId(rs.getInt("ma.supplier_id"))
                    .withSupplierTitle(rs.getString("ma.supplier_title"))
                    .withOpencartTitle(rs.getString("ma.opencart_title"))
                    .withMarkup(rs.getInt("ma.markup"))
                    .build();

            ProductProfileApp productProfileApp = new ProductProfileApp.Builder()
                    .withProductProfileId(rs.getInt("product_profile_id"))
                    .withUrl(rs.getString("pp.url"))
                    .withSku(rs.getString("sku"))
                    .withTitle(rs.getString("title"))
                    .withSupplierId(rs.getInt("pp.supplier_id"))
                    .withManufacturerId(rs.getInt("pp.manufacturer_id"))
                    .withCategoryId(rs.getInt("pp.category_id"))
                    .withPrice(rs.getBigDecimal("price"))
                    .build();

            productProfileApp.setSupplierApp(supplierApp);
            productProfileApp.setCategoryApp(categoryApp);
            productProfileApp.setManufacturerApp(manufacturerApp);

            profilesApp.add(productProfileApp);

        }

        return profilesApp;
    }
}
