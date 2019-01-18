package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.RoutineLibrary;
import org.hibernate.Session;

public class RoutineLibraryDao implements BaseDao<RoutineLibrary, String> {
    private Session session;

    public RoutineLibraryDao(Session session) {
        this.session = session;
    }

    @Override
    public RoutineLibrary get(String primaryKey) {
        return session.get(RoutineLibrary.class , primaryKey);
    }

    @Override
    public void save(RoutineLibrary entity) {
        session.save(entity);
    }

    @Override
    public void update(RoutineLibrary entity) {
        session.update(entity);
    }

    @Override
    public void delete(RoutineLibrary entity) {
        session.delete(entity);
    }
}
