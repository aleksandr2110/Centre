package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.app.ProductAttributeAppDao;
import orlov.home.centurapp.entity.app.ProductAttributeApp;
import orlov.home.centurapp.mapper.app.ProductAttributeAppRowMapper;

import java.util.List;

@Repository
@Slf4j
@AllArgsConstructor
public class ProductAttributeAppService {
    private final ProductAttributeAppDao productAttributeAppDao;


    public int save(ProductAttributeApp productAttributeApp) {
        productAttributeAppDao.save(productAttributeApp);
        return 0;
    }

    public void deleteByProfileId(int id) {
        productAttributeAppDao.deleteByProfileId(id);
    }



    public ProductAttributeApp getProductAttributeId(int productProfileAppId, int attributeAppId) {
        return productAttributeAppDao.getProductAttributeId(productProfileAppId, attributeAppId);
    }


}
