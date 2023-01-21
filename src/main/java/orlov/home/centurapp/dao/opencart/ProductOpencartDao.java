package orlov.home.centurapp.dao.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.dto.ManufacturerUpdateDto;
import orlov.home.centurapp.dto.ProductInfoDto;
import orlov.home.centurapp.dto.ProductToAttributeDto;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.*;
import orlov.home.centurapp.mapper.opencart.*;
import orlov.home.centurapp.util.OCConstant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@AllArgsConstructor
@Slf4j
public class ProductOpencartDao implements Dao<ProductOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(ProductOpencart product) {
        String sqlSaveProduct = "insert into oc_product (model, sku, upc, ean, jan, isbn, mpn, location, quantity, stock_status_id, image, " +
                "                        manufacturer_id, shipping, price, points, tax_class_id, date_available, weight, weight_class_id, " +
                "                        length, width, height, length_class_id, subtract, minimum, sort_order, status, viewed, " +
                "                        date_added, date_modified, noindex, " +
                "                        currency_id, obmen_id, itua_original_cur_id, itua_original_price, sticker_id, sticker2_id, uuid, af_values, af_tags) " +
                "                 values (:model,:sku,:upc,:ean,:jan,:isbn,:mpn,:location,:quantity,:stockStatusId,:image," +
                "                        :manufacturerId,:shipping,:price,:points,:taxClassId,:dataAvailable,:weight,:weightClassId," +
                "                        :length,:width,:height,:lengthClassId,:subtract,:minimum,:sortOrder,:status,:viewed," +
                "                        :dataAdded,:dataModified, :noindex, " +
                "                        :currencyId, :obmenId, :ituaOriginalCurId, :ituaOriginalPrice, :stickerId, :sticker2Id, :uuid, :afValues, :afTags)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplateOpencart.update(sqlSaveProduct, new BeanPropertySqlParameterSource(product), keyHolder, new String[]{"product_id"});
        }catch (Exception e){
            log.info("Exception during saving product : ",e);  // FIXME: 21.01.2023 sent exception to manager
        }
        int id = keyHolder.getKey().intValue();
        return id;
    }


    public void saveProductToStore(ProductOpencart product) {
        int storeId = OCConstant.STORE_ID;
        String sql = "insert into oc_product_to_store (product_id, store_id)" +
                "values (:productId, :storeId)";
        Map<String, Integer> data = new HashMap<>();
        data.put("productId", product.getId());
        data.put("storeId", storeId);
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource(data));
    }

    public void saveProductToCategory(ProductOpencart product) {
        String sqlToCategory = "insert into oc_product_to_category (product_id, category_id)" +
                "values (:productId, :categoryId)";
        List<CategoryOpencart> categoriesOpencart = product.getCategoriesOpencart();
        categoriesOpencart
                .forEach(c -> {
                    Map<String, Integer> dataToCategory = new HashMap<>();
                    dataToCategory.put("productId", product.getId());
                    dataToCategory.put("categoryId", c.getCategoryId());
                    jdbcTemplateOpencart.update(sqlToCategory, new MapSqlParameterSource(dataToCategory));
                });
    }

    public List<Integer> getProductsIdBySupplier(SupplierApp supplierApp) {
        String sql = "select * from oc_product p where p.jan = :jan";
        List<Integer> productsId = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("jan", supplierApp.getName()), new ProductIdMapper());
        return productsId;
    }

    public List<ProductInfoDto> getAllModelByAttributeId(int attrId) {
        String sql = "select p.model, pd.name\n" +
                "from oc_product_attribute pa\n" +
                "left join oc_product p ON p.product_id = pa.product_id\n" +
                "left join oc_product_description pd ON pd.product_id = p.product_id\n" +
                "where attribute_id = :attrId and p.product_id is not null";
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("attrId", attrId), (resultSet, i) -> new ProductInfoDto(resultSet.getString("model"), resultSet.getString("name")) );
    }

    public List<Integer> getProductsIdByAttributeOpencartId(int attributeOpencartId, String supplierName) {
        String sql = "select * from oc_product p " +
                "join oc_product_attribute pa on pa.product_id = p.product_id " +
                "join oc_attribute a on a.attribute_id = pa.attribute_id " +
                "join oc_attribute_description ad on ad.attribute_id = a.attribute_id " +
                "where a.attribute_id = :attributeOpencartId and p.jan = :supplierName and ad.language_id = :languageId";
        Map<String, Object> data = new HashMap<>();
        data.put("attributeOpencartId", attributeOpencartId);
        data.put("supplierName", supplierName);
        data.put("languageId", OCConstant.UA_LANGUAGE_ID);
        List<Integer> productsId = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new ProductIdMapper());
        return productsId;
    }


    public void deleteProductToStore(int productId) {
        String sql = "delete from oc_product_to_store where product_id = :productId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productId", productId));
    }

    public void deleteProductDescription(int productId) {
        String sql = "delete from oc_product_description where product_id = :productId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productId", productId));
    }

    public void deleteProductAttribute(int productId) {
        String sql = "delete from oc_product_attribute where product_id = :productId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productId", productId));
    }

    public void deleteProductToCategory(int productId) {
        String sql = "delete from oc_product_to_category where product_id = :productId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productId", productId));
    }

    public List<Integer> getCategoryIdByProductId(int productId) {
        String sql = "select * from oc_product_to_category where product_id = :productId";
        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("productId", productId), new CategoryIdMapper());
    }

    public void deleteProductToCategoryByProductId(int productId) {
        String sql = "delete from oc_product_to_category where product_id = :productId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productId", productId));
    }


    public void saveProductToAttribute(ProductOpencart productOpencart) {
        String sql = "insert into oc_product_attribute (product_id, attribute_id, language_id, text) " +
                "values (:productId, :attributeId, :languageId, :text)";

        productOpencart
                .getAttributesWrapper()
                .forEach(attr -> {
                    try {
                        Map<String, Object> data = new HashMap<>();
                        data.put("productId", productOpencart.getId());
                        data.put("attributeId", attr.getAttributeOpencart().getAttributeId());
                        data.put("languageId", OCConstant.UA_LANGUAGE_ID);
                        data.put("text", attr.getValueSite());
                        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource(data));
                    } catch (Exception e) {
                        log.warn("Attribute already set");
                    }
                });
    }


    @Override
    public ProductOpencart getById(int id) {
        String sql = "select * from oc_product where product_id = :productId";
        List<ProductOpencart> products = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("productId", id), new ProductOpencartRowMapper());
        return products.isEmpty() ? null : products.get(0);

    }


    public ProductOpencart getByModel(String model) {
        String sql = "select * from oc_product where model = :model";
        List<ProductOpencart> products = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("model", model), new ProductOpencartRowMapper());
        return products.isEmpty() ? null : products.get(0);

    }

    @Override
    public void deleteById(int id) {
        String sql = "delete from oc_product where product_id = :productId";
        jdbcTemplateOpencart.update(sql, new MapSqlParameterSource("productId", id));
    }

    public ProductOpencart updateImage(ProductOpencart product) {
        String sql = "update oc_product set image = :image where product_id = :id";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(product));
        return product;
    }

    @Override
    public ProductOpencart update(ProductOpencart product) {
        String sql = "update oc_product set price = :price where product_id = :id";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(product));
        return null;
    }

    public ProductOpencart updatePrice(ProductOpencart product) {
        try {
            String sql = "update oc_product " +
                    "set price = :price, itua_original_price = :ituaOriginalPrice " +
                    "where product_id = :id";
            jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(product));
        } catch (Exception ex) {
            log.warn("Exception ", ex);
        }
        return null;
    }

    public ProductOpencart updateStatus(ProductOpencart product) {
        String sql = "update oc_product set status = :status where product_id = :id";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(product));
        return null;
    }

    public ProductOpencart updateStockStatus(ProductOpencart product) {
        String sql = "update oc_product set stock_status_id = :stockStatusId where product_id = :id";
        jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(product));
        return null;
    }

    public ProductOpencart updateManufacturer(ProductOpencart product) {
        try {
            String sql = "update oc_product set manufacturer_id = :manufacturerId where product_id = :id";
            jdbcTemplateOpencart.update(sql, new BeanPropertySqlParameterSource(product));
        } catch (Exception ex) {
            log.warn("Exception ", ex);
        }
        return null;
    }

    @Override
    public List<ProductOpencart> getAll() {
        String sql = "select * from oc_product";
        List<ProductOpencart> products = jdbcTemplateOpencart.query(sql, new ProductOpencartRowMapper());
        return products;
    }

    public List<ProductOpencart> getAllBySupplier(String supplier) {
        String sql = "select * from oc_product where jan = :supplier";
        Map<String, String> data = new HashMap<>();
        data.put("supplier", supplier);
        List<ProductOpencart> products = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new ProductOpencartRowMapper());
        return products;
    }

    public List<ProductOpencart> getSupplierProducts(String supplierName) {
        String sql = "select * " +
                "from oc_product " +
                "left join oc_product_description opd on oc_product.product_id = opd.product_id " +
                "where jan = :supplierName";
        List<ProductOpencart> supplierProducts = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("supplierName", supplierName), new ProductDataOpencartExtractor());
        return supplierProducts;
    }

    public List<ProductOpencart> getProductsSameTitle(String title) {
        String sql = "select * " +
                "from oc_product " +
                "left join oc_product_description opd on oc_product.product_id = opd.product_id " +
                "where opd.name = :name";
        List<ProductOpencart> productsSameTitle = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("name", title), new ProductDataOpencartExtractor());
        return productsSameTitle;
    }

    public ProductOpencart getProductWithDescriptionById(long id) {
        String sql = "select * " +
                "from oc_product p " +
                "left join oc_product_description pd on p.product_id = pd.product_id " +
                "where p.product_id = :productId and language_id = 3 ";
        Map<String, Object> data = new HashMap<>();
        data.put("productId", id);
        ProductOpencart product = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new ProductOpencartWithDescriptionExtractor());
        return product;
    }


    public ProductOpencart getProductWithImageById(long id) {
        String sql = "select * " +
                "from oc_product p " +
                "left join oc_product_image i on p.product_id = i.product_id " +
                "where p.product_id = :productId ";
        Map<String, Object> data = new HashMap<>();
        data.put("productId", id);
        ProductOpencart product = jdbcTemplateOpencart.query(sql, new MapSqlParameterSource(data), new ProductOpencartWithImageExtractor());
        return product;
    }


    public int getLastProductModel() {
        String sql = "select * from oc_product order by product_id desc limit 1";
        int lastModel = 0;
        List<ProductOpencart> products = jdbcTemplateOpencart.query(sql, new ProductOpencartRowMapper());

        if (!products.isEmpty()) {
            String model = products.get(0).getModel().replaceAll("\\D", "");
            if (!model.isEmpty())
                lastModel = Integer.parseInt(model);
            return lastModel;
        }

        return lastModel;
    }

    public List<ProductToAttributeDto> getProductToAttributeBySupplierName(String supplierName) {

        String sql = "select p.product_id, sku, pa.attribute_id, text from oc_product p " +
                "         left join oc_product_attribute pa on p.product_id = pa.product_id " +
                "where jan = :supplierName";

        return jdbcTemplateOpencart.query(sql, new MapSqlParameterSource("supplierName", supplierName), new ProductToAttributeExtractor());
    }


    public void updateProductToAttribute(ProductToAttributeDto product) {
        try {
            String sql = "update oc_product_attribute " +
                    "set attribute_id = :newAttributeId " +
                    "where product_id = :productId and attribute_id = :attributeId and language_id = :languageId";
            Map<String, Object> data = new HashMap<>();
            data.put("newAttributeId", product.getNewAttributeId());
            data.put("productId", product.getProductId());
            data.put("attributeId", product.getAttributeId());
            data.put("languageId", OCConstant.UA_LANGUAGE_ID);
            jdbcTemplateOpencart.update(sql, new MapSqlParameterSource(data));

        } catch (Exception ex) {
            log.warn("Exception ", ex);
        }
    }

    public void updateAttributeOpencartValue(ProductToAttributeDto product) {
        try {
            String sql = "update oc_product_attribute " +
                    "set text = :text " +
                    "where product_id = :productId and attribute_id = :attributeId and language_id = :languageId";
            Map<String, Object> data = new HashMap<>();
            data.put("text", product.getText());
            data.put("productId", product.getProductId());
            data.put("attributeId", product.getAttributeId());
            data.put("languageId", OCConstant.UA_LANGUAGE_ID);
            jdbcTemplateOpencart.update(sql, new MapSqlParameterSource(data));

        } catch (Exception ex) {
            log.warn("Exception ", ex);
        }
    }


    public void updateModel(ProductOpencart product) {

        String SQL = "update oc_product " +
                "set model  = :model " +
                "where sku = :sku " +
                "and jan = :jan";

        jdbcTemplateOpencart.update(SQL, new BeanPropertySqlParameterSource(product));
    }


    public void updateProductManufacturer(ManufacturerUpdateDto manufacturerUpdateDto) {

        String SQL = "update oc_product " +
                "set manufacturer_id  = :manufacturerOpencartId " +
                "where sku = :sku " +
                "and jan = :supplierName";

        Map<String, Object> data = new HashMap<>();
        data.put("manufacturerOpencartId", manufacturerUpdateDto.getManufacturerOpencartId());
        data.put("sku", manufacturerUpdateDto.getSku());
        data.put("supplierName", manufacturerUpdateDto.getSupplierName());

        jdbcTemplateOpencart.update(SQL, data);
    }


}
