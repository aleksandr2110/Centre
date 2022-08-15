package orlov.home.centurapp.dao;

import java.util.List;

public interface Dao<T> {
    int save(T t);
    T getById(int id);
    void deleteById(int id);
    T update(T t);
    List<T> getAll();
}
