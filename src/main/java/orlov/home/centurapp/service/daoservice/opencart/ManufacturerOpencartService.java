package orlov.home.centurapp.service.daoservice.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.opencart.ManufacturerOpencartDao;
import orlov.home.centurapp.dao.opencart.ManufacturerDescriptionOpencartDao;
import orlov.home.centurapp.entity.opencart.ManufacturerOpencart;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ManufacturerOpencartService {

    private final ManufacturerOpencartDao manufacturerOpencartDao;
    private final ManufacturerDescriptionOpencartDao manufacturerDescriptionOpencartDao;

    public ManufacturerOpencart save(ManufacturerOpencart manufacturerOpencart) {
        int id = manufacturerOpencartDao.save(manufacturerOpencart);
        manufacturerOpencart.setManufacturerId(id);
        return manufacturerOpencart;
    }

    public ManufacturerOpencart getByName(String name){
        return manufacturerOpencartDao.getByName(name);
    }

    public List<ManufacturerOpencart> getAll(){
        return manufacturerOpencartDao.getAll();
    }


}
