package orlov.home.centurapp.dao.opencart;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import orlov.home.centurapp.dao.Dao;
import orlov.home.centurapp.entity.opencart.CurrencyOpencart;
import orlov.home.centurapp.mapper.opencart.CurrencyOpencartRowMapper;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class CurrencyOpencartDao implements Dao<CurrencyOpencart> {

    private final NamedParameterJdbcTemplate jdbcTemplateOpencart;

    @Override
    public int save(CurrencyOpencart currencyOpencart) {
        return 0;
    }

    @Override
    public CurrencyOpencart getById(int id) {
        return null;
    }

    @Override
    public void deleteById(int id) {

    }

    @Override
    public CurrencyOpencart update(CurrencyOpencart currencyOpencart) {
        return null;
    }

    @Override
    public List<CurrencyOpencart> getAll() {
        String sql = "select * from oc_currency";
        return jdbcTemplateOpencart.query(sql, new CurrencyOpencartRowMapper());
    }


}
