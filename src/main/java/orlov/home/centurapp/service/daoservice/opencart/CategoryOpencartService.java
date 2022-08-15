package orlov.home.centurapp.service.daoservice.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.opencart.CategoryDescriptionOpencartDao;
import orlov.home.centurapp.dao.opencart.CategoryOpencartDao;
import orlov.home.centurapp.entity.app.SupplierApp;
import orlov.home.centurapp.entity.opencart.CategoryOpencart;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class CategoryOpencartService {
    private final CategoryOpencartDao categoryOpencartDao;
    private final CategoryDescriptionOpencartDao categoryDescriptionOpencartDao;

    public List<CategoryOpencart> getSupplierCategoryOpencart(SupplierApp supplierApp) {
        return categoryOpencartDao.getAllSupplierCategoryOpencart(supplierApp);
    }

    public CategoryOpencart save(CategoryOpencart categoryOpencart) {
        int categoryId = categoryOpencartDao.save(categoryOpencart);
        categoryOpencart.setCategoryId(categoryId);
        categoryOpencart.getDescriptions()
                .stream()
                .forEach(cd -> {
                    cd.setCategoryId(categoryId);
                    categoryDescriptionOpencartDao.save(cd);
                });
        categoryOpencartDao.saveCategoryToPath(categoryOpencart);
        return categoryOpencart;
    }

    public CategoryOpencart getMainSupplierCategoryOpencart(SupplierApp supplierApp){
        return categoryOpencartDao.getMainSupplierCategoryOpencart(supplierApp);
    }

    public CategoryOpencart getCategoryByNameAndDescription(String categoryName, String categoryDescription){
       return categoryOpencartDao.getCategoryByNameAndDescription(categoryName,categoryDescription);
    }

}
