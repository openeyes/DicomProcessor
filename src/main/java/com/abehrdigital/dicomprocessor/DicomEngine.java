/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

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
    public static void main(String[] args) {
        long shutdownMsClock = System.currentTimeMillis() + 60 * 1000 * SHUTDOWN_AFTER_MINUTES;
        RequestQueueExecutor requestQueueExecutor = new RequestQueueExecutor(testRequestQueue);

        //  Stability recovery loop
        while (System.currentTimeMillis() < shutdownMsClock) {
            try {
                // Main request handler iterator
                while (System.currentTimeMillis() < shutdownMsClock) {
                    requestQueueExecutor.execute();
                }
                System.out.println("RequestQueue: exiting cleanly after " + shutdownMsClock + " milliseconds");
                throw new OrderlyExitSuccessException();
            } catch (Exception exception) {
                if (exception.getClass() == OrderlyExitSuccessException.class) {
                    Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                            exception.toString() + " EVERYTHING WENT FINE");
                } else {
                    System.out.println(exception.getClass());
                    Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                            getStackTraceAsString(exception));
                }
            }
        }
        requestQueueExecutor.shutDown();
    }
}
