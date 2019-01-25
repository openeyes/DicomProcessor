/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.*;
import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import com.abehrdigital.dicomprocessor.utils.Status;

import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.script.*;

import org.hibernate.*;
import org.hibernate.query.NativeQuery;

/**
 * @author admin
 */
public class DicomEngine {

    private static final String testRequestQueue = "dicom_queue";
    private static final int SHUTDOWN_AFTER_MINUTES = 1;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ScriptException {
        long shutdownMsClock = System.currentTimeMillis() + 60 * 1000 * SHUTDOWN_AFTER_MINUTES;
        RequestQueueExecutor requestQueueExecutor = new RequestQueueExecutor(
                testRequestQueue,

                // CHANGE TO LOCK PROVIDER OR SOMETHING BUT AVOID USING THE SAME OBJECT TWICE
                DaoFactory.createDaoManager(),
                DaoFactory.createDaoManager()
        );

        //  Stability recovery loop
        while (System.currentTimeMillis() < shutdownMsClock) {
            try {
                // Main request handler iterator
                while (System.currentTimeMillis() < shutdownMsClock) {
                    requestQueueExecutor.execute();
                }
                System.out.println("RequestQueue: exiting cleanly after " + shutdownMsClock + " minutes");
                throw new OrderlyExitSuccessException();
            } catch (Exception exception) {
                if (exception.getClass() == OrderlyExitSuccessException.class) {
                    Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                            exception.toString() + " EVERYTHING WENT FINE");
                } else {

                    System.out.println(exception.getClass());
                    Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                            exception.toString() + " SOMETHING IS WRONG");
                }
            }
        }
    }


}
