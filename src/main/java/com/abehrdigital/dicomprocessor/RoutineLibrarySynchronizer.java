package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.dao.EngineInitialisationDaoManager;
import com.abehrdigital.dicomprocessor.models.RoutineLibrary;
import com.abehrdigital.dicomprocessor.utils.DirectoryFileNamesReader;
import com.abehrdigital.dicomprocessor.utils.RoutineScriptAccessor;

import java.io.IOException;
import java.util.List;

public class RoutineLibrarySynchronizer {
    private RoutineScriptAccessor scriptAccessor;
    private DirectoryFileNamesReader fileNamesReader;
    private EngineInitialisationDaoManager daoManager;
    private Long lastTimeWhenRoutineLibraryWasPopulated;
    private long cooldownForNextCheckInMiliseconds;

    public RoutineLibrarySynchronizer(RoutineScriptAccessor scriptAccessor,
                                      DirectoryFileNamesReader fileNamesReader,
                                      EngineInitialisationDaoManager initialisationDaoManager, long delayInMs) {
        this.scriptAccessor = scriptAccessor;
        this.fileNamesReader = fileNamesReader;
        this.daoManager = initialisationDaoManager;
        this.cooldownForNextCheckInMiliseconds = delayInMs;
        lastTimeWhenRoutineLibraryWasPopulated = null;
    }

    public void sync() throws IOException {
        if(lastTimeWhenRoutineLibraryWasPopulated == null ||
                System.currentTimeMillis() >= lastTimeWhenRoutineLibraryWasPopulated + cooldownForNextCheckInMiliseconds) {
            List<String> fileNames = fileNamesReader.read(DicomEngine.SCRIPT_FILE_LOCATION);
            for (String fileName : fileNames) {
                if (daoManager.getRoutineLibraryDao().get(fileName) == null) {
                    createAndSaveNewRoutineLibrary(fileName);
                }
            }
            lastTimeWhenRoutineLibraryWasPopulated = System.currentTimeMillis();
        }
    }

    private void createAndSaveNewRoutineLibrary(String fileName) throws IOException {
        String trimmedFileName = fileName.trim();
        RoutineLibrary routineLibrary = new RoutineLibrary(trimmedFileName,
                scriptAccessor.getRoutineScriptHashCode(trimmedFileName)
        );
        daoManager.transactionStart();
        daoManager.getRoutineLibraryDao().save(routineLibrary);
        daoManager.commit();
    }
}
