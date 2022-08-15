package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.app.CategoryAppDao;
import orlov.home.centurapp.entity.app.CategoryApp;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class CategoryAppService {

    private final CategoryAppDao categoryAppDao;

    public CategoryApp save(CategoryApp categoryApp) {
        int id = categoryAppDao.save(categoryApp);
        categoryApp.setCategoryId(id);
        return categoryApp;
    }

    public CategoryApp update(CategoryApp categoryApp) {
        return categoryAppDao.update(categoryApp);
    }

    public List<CategoryApp> getAll() {
        return categoryAppDao.getAll();
    }

    public List<CategoryApp> getAllCategoryAppBySupplierAppId(int supplierId) {
        return categoryAppDao.getAllCategoryAppBySupplierAppId(supplierId);
    }

    public void deleteAll(){
        categoryAppDao.deleteAll();
    }


}
