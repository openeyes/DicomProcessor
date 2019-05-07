/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.exceptions.OrderlyExitSuccessException;
import com.abehrdigital.dicomprocessor.exceptions.RequestQueueMissingException;
import com.abehrdigital.dicomprocessor.utils.*;
import org.apache.commons.cli.*;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author admin
 */
public class DicomEngine {

    private static String requestQueue;
    private static int SHUTDOWN_AFTER_MINUTES = 3;
    private static int SYNCHRONIZE_ROUTINE_LIBRARY_AFTER_MINUTES;
    private static int RETRY_DATABASE_CONNECTION_FOR_MINUTES;
    public static String SCRIPT_FILE_LOCATION;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        initOptions(args);
        buildSessionFactory();

        long shutdownMsClock = System.currentTimeMillis() + 60 * 1000 * SHUTDOWN_AFTER_MINUTES;
        long synchronizeRoutineLibraryDelay = 60 * 1000 * SYNCHRONIZE_ROUTINE_LIBRARY_AFTER_MINUTES;
        RoutineLibrarySynchronizer routineLibrarySynchronizer = new RoutineLibrarySynchronizer(
                new RoutineScriptAccessor(),
                new DirectoryFileNamesReader(),
                DaoFactory.createEngineInitialisationDaoManager(),
                synchronizeRoutineLibraryDelay
        );

        try {
            routineLibrarySynchronizer.sync();
        } catch (IOException e) {
            e.printStackTrace();
        }

        RequestQueueExecutor requestQueueExecutor = new RequestQueueExecutor(
                requestQueue,
                routineLibrarySynchronizer,
                shutdownMsClock);

        // Stability recovery loop
        while (System.currentTimeMillis() < shutdownMsClock) {
            try {
                // Main request handler iterator
                while (System.currentTimeMillis() < shutdownMsClock) {
                    requestQueueExecutor.execute();
                }
                throw new OrderlyExitSuccessException("Engine run was successful");
            } catch (RequestQueueMissingException queueMissingException) {
                Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                        StackTraceUtil.getStackTraceAsString(queueMissingException));
                break;
            } catch (OrderlyExitSuccessException successException) {
                Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                        successException.toString());
            } catch (Exception exception) {
                requestQueueExecutor.shutDown();
                System.out.println(exception.getClass());
                Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                        StackTraceUtil.getStackTraceAsString(exception));
            }
        }
        requestQueueExecutor.shutDown();
    }

    private static void initOptions(String[] args) {
        Options options = new Options();
        Option optionScriptFileLocation = new Option("sf", "scriptFileLocation",
                true,
                "Specify the location of routine library scripts");
        Option optionRequestQueue = new Option("rq", "requestQueue",
                true,
                "Specify the request_queue to process");
        Option optionShutDownAfterMinutes = new Option("sa", "shutdownAfterMinutes",
                true,
                "Specify the amount of minutes after which the engine should shut down");
        Option optionRoutineLibraryAfterMinutes = new Option("sy", "synchronizeRoutine",
                true,
                "Specify the delay in minutes that the routine library in the database should be synchronized after");
        Option optionRetryDatabaseConnectionForMinutes = new Option("rd", "retryDatabaseConnectionForMinutes",
                true,
                "Specify the amount of minutes to try connecting to the database");
        options.addOption(optionRequestQueue);
        options.addOption(optionScriptFileLocation);
        options.addOption(optionShutDownAfterMinutes);
        options.addOption(optionRoutineLibraryAfterMinutes);
        options.addOption(optionRetryDatabaseConnectionForMinutes);

        CommandLineParser parser;
        parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.hasOption("sf") || commandLine.hasOption("scriptFileLocation")) {
                SCRIPT_FILE_LOCATION = commandLine.getOptionValue("scriptFileLocation");
            }
            if (commandLine.hasOption("rq") || commandLine.hasOption("requestQueue")) {
                requestQueue = commandLine.getOptionValue("requestQueue");
            }
            if (commandLine.hasOption("sa") || commandLine.hasOption("shutdownAfterMinutes")) {
                SHUTDOWN_AFTER_MINUTES = Integer.parseInt(commandLine.getOptionValue("shutdownAfterMinutes"));
            }
            if (commandLine.hasOption("sy") || commandLine.hasOption("synchronizeRoutine")) {
                SYNCHRONIZE_ROUTINE_LIBRARY_AFTER_MINUTES = Integer.parseInt(commandLine.getOptionValue("synchronizeRoutine"));
            }
            if (commandLine.hasOption("rd") || commandLine.hasOption("retryDatabaseConnectionForMinutes")) {
                RETRY_DATABASE_CONNECTION_FOR_MINUTES = Integer.parseInt(commandLine.getOptionValue("retryDatabaseConnectionForMinutes"));
            }
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            // exit code 1: unable to parse command line arguments
            System.exit(1);
        }
    }

    private static void buildSessionFactory() throws InterruptedException {
        DatabaseConfiguration.init();
        Configuration hibernateConfig = DatabaseConfiguration.getHibernateConfiguration();
        ExceptionInInitializerError lastException = new ExceptionInInitializerError("Failed connecting to the database");
        long shutdownRetryDatabaseConnectionClock = System.currentTimeMillis() * 60 * 1000 * RETRY_DATABASE_CONNECTION_FOR_MINUTES;
        boolean connectionAcquired = false;
        while (System.currentTimeMillis() < shutdownRetryDatabaseConnectionClock && !connectionAcquired) {
            try {
                connectionAcquired = HibernateUtil.buildSessionFactory(hibernateConfig);
            } catch (ExceptionInInitializerError exception) {
                lastException = exception;
                TimeUnit.SECONDS.sleep(5);
            }
        }

        if (!connectionAcquired) {
            throw lastException;
        }
    }
}
