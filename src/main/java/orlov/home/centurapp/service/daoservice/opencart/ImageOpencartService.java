package orlov.home.centurapp.service.daoservice.opencart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.opencart.ImageOpencartDao;
import orlov.home.centurapp.entity.opencart.ImageOpencart;
import orlov.home.centurapp.mapper.opencart.ImageOpencartRowMapper;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
@Getter
@Setter
public class ImageOpencartService {

    private final ImageOpencartDao imageOpencartDao;

    public void deleteById(int id) {
        imageOpencartDao.deleteById(id);
    }

    public void deleteByName(String imageName) {
        imageOpencartDao.deleteByName(imageName);
    }

    public void deleteByProductId(int productId) {
        imageOpencartDao.deleteByProductId(productId);
    }

    public int save(ImageOpencart imageOpencart) {
        return imageOpencartDao.save(imageOpencart);
    }

    public List<ImageOpencart> getImageByProductId(int productId) {
        return imageOpencartDao.getImageByProductId(productId);
    }

    public void updateSubImage(ImageOpencart imageOpencart) {
        imageOpencartDao.update(imageOpencart);
    }

    public ImageOpencart getByImage(String image) {
        return imageOpencartDao.getByImage(image);
    }

}
