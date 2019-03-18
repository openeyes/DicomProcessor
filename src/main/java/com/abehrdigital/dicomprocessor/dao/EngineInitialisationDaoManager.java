package com.abehrdigital.dicomprocessor.dao;

public class EngineInitialisationDaoManager extends BaseDaoManager {
    private RoutineLibraryDao routineLibraryDao;

    public EngineInitialisationDaoManager() {
    }

    public RoutineLibraryDao getRoutineLibraryDao() {
        if (this.routineLibraryDao == null) {
            this.routineLibraryDao = new RoutineLibraryDao(getConnection());
        }

        return this.routineLibraryDao;
    }
}
