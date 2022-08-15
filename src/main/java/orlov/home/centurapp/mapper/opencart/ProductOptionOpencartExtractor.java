package orlov.home.centurapp.mapper.opencart;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import orlov.home.centurapp.entity.opencart.ProductOptionOpencart;
import orlov.home.centurapp.entity.opencart.ProductOptionValueOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductOptionOpencartExtractor implements ResultSetExtractor<List<ProductOptionOpencart>> {
    @Override
    public List<ProductOptionOpencart> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<ProductOptionOpencart> productOptions = new ArrayList<>();

        while (rs.next()){
            ProductOptionOpencart option = new ProductOptionOpencart();
            option.setProductOptionId(rs.getInt("o.product_option_id"));
            option.setProductId(rs.getInt("o.product_id"));
            option.setOptionId(rs.getInt("o.option_id"));
            option.setValue(rs.getString("value"));
            option.setRequired(rs.getBoolean("required"));

            ProductOptionValueOpencart optionValue = new ProductOptionValueOpencart();
            optionValue.setProductOptionValueId(rs.getInt("product_option_value_id"));
            optionValue.setProductOptionId(rs.getInt("ov.product_option_id"));
            optionValue.setProductId(rs.getInt("ov.product_id"));
            optionValue.setOptionId(rs.getInt("ov.option_id"));
            optionValue.setOptionValueId(rs.getInt("option_value_id"));
            optionValue.setQuantity(rs.getInt("quantity"));
            optionValue.setSubtract(rs.getBoolean("subtract"));
            optionValue.setPrice(rs.getBigDecimal("price"));
            optionValue.setPricePrefix(rs.getString("price_prefix"));
            optionValue.setPoints(rs.getInt("points"));
            optionValue.setPointsPrefix(rs.getString("points_prefix"));
            optionValue.setWeight(rs.getBigDecimal("weight"));
            optionValue.setWeightPrefix(rs.getString("weight_prefix"));
            optionValue.setReward(rs.getBigDecimal("reward"));
            optionValue.setRewardPrefix(rs.getString("reward_prefix"));
            optionValue.setOptsku(rs.getString("optsku"));

            boolean containsOption = productOptions.contains(option);
            if (containsOption){
                ProductOptionOpencart productOptionOpencart = productOptions.get(productOptions.indexOf(option));
                List<ProductOptionValueOpencart> optionValues = productOptionOpencart.getOptionValues();
                boolean containsValue = optionValues.contains(optionValue);
                if (!containsValue){
                    optionValues.add(optionValue);
                }
            } else {
                option.getOptionValues().add(optionValue);
                productOptions.add(option);
            }
        }

        return productOptions;
    }
}
