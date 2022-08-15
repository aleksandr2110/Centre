package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.app.AttributeAppDao;
import orlov.home.centurapp.entity.app.AttributeApp;
import orlov.home.centurapp.mapper.app.AttributeAppRowMapper;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class AttributeAppService {

    private final AttributeAppDao attributeAppDao;

    public AttributeApp save(AttributeApp attributeApp) {
        int id = attributeAppDao.save(attributeApp);
        attributeApp.setAttributeId(id);
        return attributeApp;
    }

    public List<AttributeApp> getAll() {
        return attributeAppDao.getAll();
    }

    public List<AttributeApp> getAllByLikeName(String likeName) {
        return attributeAppDao.getAllByLikeName(likeName);
    }

    public List<AttributeApp> getAllBySupplierId(int supplierId) {
        return attributeAppDao.getAllBySupplierId(supplierId);
    }

    public AttributeApp getById(int id) {
        return attributeAppDao.getById(id);
    }

    public AttributeApp update(AttributeApp attributeApp) {
        return attributeAppDao.update(attributeApp);
    }

    public void deleteAll() {
        attributeAppDao.deleteAll();
    }

}
