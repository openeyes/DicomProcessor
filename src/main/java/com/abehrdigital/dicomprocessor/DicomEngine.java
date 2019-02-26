/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.exceptions.OrderlyExitSuccessException;
import com.abehrdigital.dicomprocessor.exceptions.RequestQueueMissingException;
import com.abehrdigital.dicomprocessor.utils.DatabaseConfiguration;
import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import org.hibernate.cfg.Configuration;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.abehrdigital.dicomprocessor.utils.StackTraceUtil.getStackTraceAsString;

/**
 * @author admin
 */
public class DicomEngine {

    private static final String testRequestQueue = "dicom_queue";
    private static final int SHUTDOWN_AFTER_MINUTES = 3;

    /**
     * @param args the command line arguments
     */
    //TODO SHUT DOWN AFER MINUTES SHOULD COME THROUGH ARGS
    public static void main(String[] args) {
        buildSessionFactory();
        long shutdownMsClock = System.currentTimeMillis() + 60 * 1000 * SHUTDOWN_AFTER_MINUTES;
        RequestQueueExecutor requestQueueExecutor = new RequestQueueExecutor(testRequestQueue);

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
                        queueMissingException.toString());
                break;
            } catch (OrderlyExitSuccessException successException) {
                Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                        successException.toString());
            } catch (Exception exception) {
                System.out.println(exception.getClass());
                Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                        getStackTraceAsString(exception));
            }
        }
        requestQueueExecutor.shutDown();
    }

    private static void buildSessionFactory() {
        DatabaseConfiguration.init();
        Configuration hibernateConfig = DatabaseConfiguration.getHibernateConfiguration();
        HibernateUtil.buildSessionFactory(hibernateConfig);
    }
}
