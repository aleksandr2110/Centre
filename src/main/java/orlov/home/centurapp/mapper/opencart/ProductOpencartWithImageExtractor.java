package orlov.home.centurapp.mapper.opencart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import orlov.home.centurapp.entity.opencart.ImageOpencart;
import orlov.home.centurapp.entity.opencart.ProductDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ProductOpencartWithImageExtractor implements ResultSetExtractor<ProductOpencart> {
    @Override
    public ProductOpencart extractData(ResultSet rs) throws SQLException, DataAccessException {
        ProductOpencart product = null;

        while (rs.next()) {

            if (Objects.isNull(product)) {
                product = new ProductOpencart.Builder()
                        .withProductId(rs.getInt("p.product_id"))
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
                        .withImage(rs.getString("p.image"))
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
                        .withSortOrder(rs.getInt("p.sort_order"))
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
                        .withUuid(rs.getString("p.uuid"))
                        .withAfValues(rs.getString("af_values"))
                        .withAfTags(rs.getString("af_tags"))
                        .build();
            }

            ImageOpencart imageOpencart = new ImageOpencart.Builder()
                    .withProductImageId(rs.getInt("i.product_image_id"))
                    .withProductId(rs.getInt("i.product_id"))
                    .withImage(rs.getString("i.image"))
                    .withSortOrder(rs.getInt("i.sort_order"))
                    .withUuid(rs.getString("i.uuid"))
                    .build();

            List<ImageOpencart> imagesOpencart = product.getImagesOpencart();
            if (!imagesOpencart.contains(imageOpencart) && imageOpencart.getProductImageId() != 0)
                imagesOpencart.add(imageOpencart);

        }

        return product;

    }
}
