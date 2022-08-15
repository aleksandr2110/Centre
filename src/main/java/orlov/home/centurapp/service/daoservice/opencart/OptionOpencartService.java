package orlov.home.centurapp.service.daoservice.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.opencart.OptionDescriptionOpencartDao;
import orlov.home.centurapp.dao.opencart.OptionOpencartDao;
import orlov.home.centurapp.dao.opencart.OptionValueDescriptionOpencartDao;
import orlov.home.centurapp.dao.opencart.OptionValueOpencartDao;
import orlov.home.centurapp.entity.opencart.OptionDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.OptionOpencart;
import orlov.home.centurapp.entity.opencart.OptionValueDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.OptionValueOpencart;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class OptionOpencartService {

    private final OptionOpencartDao optionOpencartDao;
    private final OptionDescriptionOpencartDao optionDescriptionOpencartDao;
    private final OptionValueOpencartDao optionValueOpencartDao;
    private final OptionValueDescriptionOpencartDao optionValueDescriptionOpencartDao;

    public OptionOpencart saveOption(OptionOpencart option) {
        int id = optionOpencartDao.save(option);
        option.setOptionId(id);
        return option;
    }

    public OptionValueOpencart updateOptionValueOpencart(OptionValueOpencart optionValueOpencart){
        return optionValueOpencartDao.update(optionValueOpencart);
    }

    public List<OptionOpencart> getAllOptionOpencartFullData(){
        return optionOpencartDao.getAll();
    }

    public OptionDescriptionOpencart saveOptionDescription(OptionDescriptionOpencart optionDescription) {
        int id = optionDescriptionOpencartDao.save(optionDescription);
        return optionDescription;
    }

    public OptionValueOpencart saveOptionValueOpencart(OptionValueOpencart optionValue) {
        int id = optionValueOpencartDao.save(optionValue);
        optionValue.setOptionValueId(id);
        return optionValue;
    }

    public OptionValueDescriptionOpencart saveOptionValueDescription(OptionValueDescriptionOpencart optionValueDescription) {
        optionValueDescriptionOpencartDao.save(optionValueDescription);
        return optionValueDescription;
    }



}
