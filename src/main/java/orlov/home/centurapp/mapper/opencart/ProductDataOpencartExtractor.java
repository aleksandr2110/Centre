package orlov.home.centurapp.mapper.opencart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ProductDataOpencartExtractor implements ResultSetExtractor<List<ProductOpencart>> {
    @Override
    public List<ProductOpencart> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<ProductOpencart> products = new ArrayList<>();
        while (rs.next()) {
            try {


            ProductOpencart product = new ProductOpencart.Builder()
                    .withProductId(rs.getInt("product_id"))
                    .withModel(rs.getString("model"))
                    .withSku(rs.getString("sku"))
                    .withUpc(rs.getString("upc"))
                    .withEan(rs.getString("ean"))
                    .withJan(rs.getString("jan"))
                    .withIsbn(rs.getString("isbn"))
                    .withMpn(rs.getString("mpn"))
                    .withLocation(rs.getString("location"))
                    .withQuantity(rs.getInt("quantity"))
                    .withStockStatusId(rs.getInt("stock_status_id"))
                    .withImage(rs.getString("image"))
                    .withManufacturerId(rs.getInt("manufacturer_id"))
                    .withShipping(rs.getBoolean("shipping"))
                    .withPrice(rs.getBigDecimal("price"))
                    .withPoints(rs.getInt("points"))
                    .withTaxClassId(rs.getInt("tax_class_id"))
                    .withDataAvailable(rs.getTimestamp("date_available"))
                    .withWeight(rs.getBigDecimal("weight"))
                    .withWeightClassId(rs.getInt("weight_class_id"))
                    .withLength(rs.getBigDecimal("length"))
                    .withWidth(rs.getBigDecimal("width"))
                    .withHeight(rs.getBigDecimal("height"))
                    .withLengthClassId(rs.getInt("length_class_id"))
                    .withSubtract(rs.getBoolean("subtract"))
                    .withMinimum(rs.getInt("minimum"))
                    .withSortOrder(rs.getInt("sort_order"))
                    .withStatus(rs.getBoolean("status"))
                    .withViewed(rs.getInt("viewed"))
                    .withDataAdded(rs.getTimestamp("date_added"))
                    .withDataModified(rs.getTimestamp("date_modified"))
                    .withNoindex(rs.getBoolean("noindex"))
                    .withCurrencyId(rs.getInt("currency_id"))
                    .withObmenId(rs.getInt("obmen_id"))
                    .withItuaOriginalCurId(rs.getInt("itua_original_cur_id"))
                    .withItuaOriginalPrice(rs.getBigDecimal("itua_original_price"))
                    .withStickerId(rs.getInt("sticker_id"))
                    .withSticker2Id(rs.getInt("sticker2_id"))
                    .withUuid(rs.getString("uuid"))
                    .withAfValues(rs.getString("af_values"))
                    .withAfTags(rs.getString("af_tags"))
                    .build();

            ProductDescriptionOpencart description = new ProductDescriptionOpencart.Builder()
                    .withProductId(rs.getInt("product_id"))
                    .withLanguageId(rs.getInt("language_id"))
                    .withName(rs.getString("name"))
                    .withDescription(rs.getString("description"))
                    .withTag(rs.getString("tag"))
                    .withMetaTitle(rs.getString("meta_title"))
                    .withMetaDescription(rs.getString("meta_description"))
                    .withMetaKeyword(rs.getString("meta_keyword"))
                    .withMetaH1(rs.getString("meta_h1"))
                    .withDescription1(rs.getString("description1"))
                    .build();


            if (products.contains(product)) {
                ProductOpencart availableProduct = products.get(products.indexOf(product));
                List<ProductDescriptionOpencart> descriptions = availableProduct.getProductsDescriptionOpencart();
                if (!descriptions.contains(description)){
                    descriptions.add(description);
                }
            } else {
                product.getProductsDescriptionOpencart().add(description);
                products.add(product);
            }
            } catch (Exception ex){
                log.error("bag gat product from result set.", ex);
            }
        }

        return products;
    }
}
