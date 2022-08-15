package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.app.OptionAppDao;
import orlov.home.centurapp.entity.app.OptionApp;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class OptionAppService {

    private final OptionAppDao optionAppDao;

    public OptionApp saveOptionApp(OptionApp optionApp) {
        int optionId = optionAppDao.save(optionApp);
        optionApp.setOptionId(optionId);
        return optionApp;
    }

    public OptionApp updateOptionApp(OptionApp optionApp) {
        optionAppDao.update(optionApp);
        return optionApp;
    }

    public List<OptionApp> getAllOptionApp() {
        return optionAppDao.getAll();
    }

    public List<OptionApp> getOptionsByProductId(int productId) {
        return optionAppDao.getOptionsByProductId(productId);
    }

}
