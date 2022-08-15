package orlov.home.centurapp.service.daoservice.app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import orlov.home.centurapp.dao.app.OrderProcessAppDao;
import orlov.home.centurapp.entity.app.OrderProcessApp;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class OrderProcessAppService {

    private final OrderProcessAppDao orderProcessDao;

    public OrderProcessApp save(OrderProcessApp orderProcess){
        int id = orderProcessDao.save(orderProcess);
        orderProcess.setOrderProcessId(id);
        return orderProcess;
    }

    public List<OrderProcessApp> getAllLimited(int begin, int limit){
        return orderProcessDao.getAllLimited(begin, limit);
    }

    public void deleteAll(){
        orderProcessDao.deleteAll();
    }

}
