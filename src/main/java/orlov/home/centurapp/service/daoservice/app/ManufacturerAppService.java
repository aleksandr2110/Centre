package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.app.ManufacturerAppDao;
import orlov.home.centurapp.entity.app.ManufacturerApp;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class ManufacturerAppService {

    private final ManufacturerAppDao manufacturerAppDao;


    public ManufacturerApp save(ManufacturerApp manufacturerApp) {
        int id = manufacturerAppDao.save(manufacturerApp);
        manufacturerApp.setManufacturerId(id);
        return manufacturerApp;
    }

    public ManufacturerApp update(ManufacturerApp manufacturerApp) {
        return manufacturerAppDao.update(manufacturerApp);
    }

    public List<ManufacturerApp> getAll() {
        return manufacturerAppDao.getAll();
    }

    public List<ManufacturerApp> getAllBySupplierId(int supplierId) {
        return manufacturerAppDao.getAllBySupplierId(supplierId);
    }

    public ManufacturerApp getManufacturerAppById(int manufacturerAppId) {
        return manufacturerAppDao.getById(manufacturerAppId);
    }

    public void deleteAll(){
        manufacturerAppDao.deleteAll();
    }

}
