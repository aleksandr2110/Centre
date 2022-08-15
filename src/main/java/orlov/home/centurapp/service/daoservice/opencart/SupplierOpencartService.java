package orlov.home.centurapp.service.daoservice.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.opencart.CurrencyOpencartDao;
import orlov.home.centurapp.dao.opencart.SupplierOpencartDao;
import orlov.home.centurapp.entity.opencart.CurrencyOpencart;
import orlov.home.centurapp.entity.opencart.SupplierOpencart;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class SupplierOpencartService {

    private final SupplierOpencartDao supplierOpencartDao;
    private final CurrencyOpencartDao currencyOpencartDao;

    public List<SupplierOpencart> getAll(){
        return supplierOpencartDao.getAll();
    }

    public SupplierOpencart getBySupId(int supId){
        return supplierOpencartDao.getById(supId);
    }

    public SupplierOpencart update(SupplierOpencart supplierOpencart){
        return supplierOpencartDao.update(supplierOpencart);
    }
    public SupplierOpencart getBySubCode(String subCode) {
        return supplierOpencartDao.getBySubCode(subCode);
    }


}
