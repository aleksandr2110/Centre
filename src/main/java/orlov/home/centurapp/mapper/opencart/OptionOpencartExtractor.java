package orlov.home.centurapp.mapper.opencart;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import orlov.home.centurapp.entity.opencart.OptionDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.OptionOpencart;
import orlov.home.centurapp.entity.opencart.OptionValueDescriptionOpencart;
import orlov.home.centurapp.entity.opencart.OptionValueOpencart;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OptionOpencartExtractor implements ResultSetExtractor<List<OptionOpencart>> {


    @Override
    public List<OptionOpencart> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<OptionOpencart> optionList = new ArrayList<>();
        while (rs.next()) {
            OptionOpencart option = new OptionOpencart();
            option.setOptionId(rs.getInt("o.option_id"));
            option.setType(rs.getString("o.type"));
            option.setSortOrder(rs.getInt("o.sort_order"));
            option.setUuid(rs.getString("o.uuid"));

            OptionDescriptionOpencart optionDescription = new OptionDescriptionOpencart();
            optionDescription.setOptionId(rs.getInt("od.option_id"));
            optionDescription.setLanguageId(rs.getInt("od.language_id"));
            optionDescription.setName(rs.getString("od.name"));

            OptionValueOpencart optionValue = new OptionValueOpencart();
            optionValue.setOptionValueId(rs.getInt("v.option_value_id"));
            optionValue.setOptionId(rs.getInt("v.option_id"));
            optionValue.setImage(rs.getString("v.image"));
            optionValue.setSortOrder(rs.getInt("v.sort_order"));
            optionValue.setUuid(rs.getString("uuid"));

            OptionValueDescriptionOpencart optionValueDescription = new OptionValueDescriptionOpencart();
            optionValueDescription.setOptionValueId(rs.getInt("vd.option_value_id"));
            optionValueDescription.setLanguageId(rs.getInt("vd.language_id"));
            optionValueDescription.setOptionId(rs.getInt("vd.option_id"));
            optionValueDescription.setName(rs.getString("vd.name"));

            boolean containsOption = optionList.contains(option);

            if (containsOption) {
                OptionOpencart optionOpencart = optionList.get(optionList.indexOf(option));

                List<OptionDescriptionOpencart> descriptionList = optionOpencart.getDescriptions();
                boolean containsDescription = descriptionList.contains(optionDescription);

                if (!containsDescription) {
                    descriptionList.add(optionDescription);
                }

                List<OptionValueOpencart> valueList = optionOpencart.getValues();

                boolean containsValue = valueList.contains(optionValue);
                if (!containsValue) {
                    optionValue.getDescriptionValue().add(optionValueDescription);
                    valueList.add(optionValue);
                } else {

                    OptionValueOpencart optionValueOpencart = valueList.get(valueList.indexOf(optionValue));
                    List<OptionValueDescriptionOpencart> descriptionsValue = optionValueOpencart.getDescriptionValue();

                    boolean containsValueDescription = descriptionsValue.contains(optionValueDescription);
                    if (!containsValueDescription) {
                        optionValueOpencart.getDescriptionValue().add(optionValueDescription);
                    }

                }


            } else {
                optionValue.getDescriptionValue().add(optionValueDescription);
                option.getValues().add(optionValue);
                option.getDescriptions().add(optionDescription);
                optionList.add(option);
            }

        }
        return optionList;
    }
}
