package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.app.SupplierAppDao;
import orlov.home.centurapp.entity.app.SupplierApp;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class SupplierAppService {

    private final SupplierAppDao supplierAppDao;

    public SupplierApp save(SupplierApp supplierApp) {
        int id = supplierAppDao.save(supplierApp);
        supplierApp.setSupplierAppId(id);
        return supplierApp;
    }

    public SupplierApp getByName(String name) {
        return supplierAppDao.getByName(name);
    }

    public SupplierApp getByDisplayName(String displayName) {
        return supplierAppDao.getByDisplayName(displayName);
    }

    public List<SupplierApp> getAll() {
        return supplierAppDao.getAll();
    }

    public SupplierApp getById(int id) {
        return supplierAppDao.getById(id);
    }

    public SupplierApp update(SupplierApp supplierApp) {
        return supplierAppDao.update(supplierApp);
    }


}
