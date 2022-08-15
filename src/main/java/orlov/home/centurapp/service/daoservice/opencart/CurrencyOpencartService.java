package orlov.home.centurapp.service.daoservice.opencart;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.opencart.CurrencyOpencartDao;
import orlov.home.centurapp.entity.opencart.CurrencyOpencart;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class CurrencyOpencartService {

    private final CurrencyOpencartDao currencyOpencartDao;

    public List<CurrencyOpencart> getAllCurrency(){
        return currencyOpencartDao.getAll();
    }


}
