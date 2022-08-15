package orlov.home.centurapp.service.daoservice.opencart;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.opencart.AttributeDescriptionOpencartDao;
import orlov.home.centurapp.dao.opencart.AttributeGroupDescriptionOpencartDao;
import orlov.home.centurapp.dao.opencart.AttributeGroupOpencartDao;
import orlov.home.centurapp.dao.opencart.AttributeOpencartDao;
import orlov.home.centurapp.dto.AttributeWrapper;
import orlov.home.centurapp.dto.ProductToAttributeDto;
import orlov.home.centurapp.entity.opencart.AttributeDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.AttributeGroupOpencart;
import orlov.home.centurapp.entity.opencart.AttributeOpencart;
import orlov.home.centurapp.entity.opencart.ProductOpencart;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class AttributeOpencartService {
    private final AttributeGroupOpencartDao attributeGroupOpencartDao;
    private final AttributeGroupDescriptionOpencartDao attributeGroupDescriptionOpencartDao;
    private final AttributeOpencartDao attributeOpencartDao;
    private final AttributeDescriptionOpencartDao attributeDescriptionOpencartDao;

    public AttributeGroupOpencart getDefaultGlobalAttributeGroupByName(String name) {
        return attributeGroupOpencartDao.getDefaultGlobalAttributeGroupByName(name);
    }

    public List<AttributeWrapper> getAttributesWrapperByProduct(ProductOpencart product) {
        return attributeOpencartDao.getAttributesByProduct(product);
    }

    public AttributeOpencart getByName(String name){
        return attributeOpencartDao.getByName(name);
    }

    public void batchUpdateAttribute(List<ProductToAttributeDto> attributesDto){
        attributeOpencartDao.batchUpdateAttribute(attributesDto);
    }

    public AttributeOpencart saveAttribute(AttributeOpencart attributeOpencart) {
        int id = attributeOpencartDao.save(attributeOpencart);
        attributeOpencart.setAttributeId(id);
        return attributeOpencart;
    }

    public AttributeDescriptionOpencart saveAttributeDescription(AttributeDescriptionOpencart attributeDescriptionOpencart) {
        attributeDescriptionOpencartDao.save(attributeDescriptionOpencart);
        return attributeDescriptionOpencart;
    }

    public List<AttributeOpencart> getAllWithDesc() {
        return attributeOpencartDao.getAllWithDesc();
    }

    public List<AttributeOpencart> getAllBySearchWithDesc(String likeName) {
        return attributeOpencartDao.getAllBySearchWithDesc(likeName);
    }

    public ProductToAttributeDto getProductToAttributeById(int productId, int attributeId){
        return attributeOpencartDao.getProductToAttributeById(productId, attributeId);
    }

}
