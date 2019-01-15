/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.*;
import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import com.abehrdigital.dicomprocessor.utils.Status;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.script.*;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author admin
 */
public class DicomEngine {

    private static final String testRequestQueue = "dicom_queue";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ScriptException {
        try {

//            RequestQueue requestQueue = new RequestQueue.Builder("high_priority", 0, 0, 0, 0).build();
            RequestQueueExecutor requestQueueExecutor = new RequestQueueExecutor(
                    testRequestQueue,
                    DaoFactory.createDaoManager()
            );
            requestQueueExecutor.execute();

//            RequestQueue requestQueue = requestQueueDao.getRequestQueueByPk(testRequestQueue);
//            requestQueueDao.establishQueueLock(requestQueue);
            executeEngine();
        } catch (Exception ex) {
            System.out.println(ex.toString());
            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void executeEngine() throws SQLException, ClassNotFoundException {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<RequestRoutine> requestRoutines = getRequestRoutineListByStatus(Status.NEW, session);

        for (RequestRoutine requestRoutine : requestRoutines) {
            Status status;
            Request request = getRequestById(session, requestRoutine.getRequestId());
            request.setSession(session);
            String logMessage = "";
            try {
                ScriptEngineManager factory = new ScriptEngineManager();
                ScriptEngine engine = factory.getEngineByName("JavaScript");
                engine.put("request", request);

                RoutineLibrary routineLibrary = getRoutineLibrary(
                        session,
                        requestRoutine.getRoutineName()
                );
                Transaction transaction = session.beginTransaction();
                engine.eval(routineLibrary.getRoutineBody());
                transaction.commit();
                status = Status.SUCCESS;
            } catch (HibernateException exception) {
                if (session.getTransaction() != null) {
                    session.getTransaction().rollback();
                }
                Logger.getLogger(DicomEngine.class.getName(), exception.toString());
                status = Status.FAILED;
            } catch (Exception e) {
                if (session.getTransaction() != null) {
                    session.getTransaction().rollback();
                }
                System.out.println(e.toString());
                logMessage = e.toString();
                status = Status.FAILED;
            }

            requestRoutine.evaluateStatus(status);
            try {
                insertRequestRoutineExecution(requestRoutine, session, logMessage);
            } catch (HibernateException exception) {
                session.getTransaction().rollback();
                System.err.println("Got an exception123!");
                System.err.println(exception.getMessage());
            }

            try {
                updateRequestRoutine(requestRoutine, session);
            } catch (HibernateException exception) {
                session.getTransaction().rollback();
                System.err.println("Got an exception!");
                System.err.println(exception.getMessage());
            }
        }
    }

//    private static Connection createDatabaseConnection() throws SQLException, ClassNotFoundException {
//        Class.forName("com.mysql.jdbc.Driver");
//        Connection connection = DriverManager.getConnection(
//                "jdbc:mysql://openeyes.vm", "openeyes", "openeyes");
//        connection.setCatalog("openeyes");
//        return connection;
//    }

    private static RoutineLibrary getRoutineLibrary(Session session, String routineName) throws HibernateException {
        return session.get(RoutineLibrary.class, routineName);
    }

    private static Request getRequestById(Session session, int requestId) throws HibernateException {
        return session.get(Request.class, requestId);
    }

    private static List<RequestRoutine> getRequestRoutineListByStatus(Status status, Session session) throws HibernateException {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery(RequestRoutine.class);
        Root<RequestRoutine> root = criteriaQuery.from(RequestRoutine.class);
        criteriaQuery.where(criteriaBuilder.equal(root.get("status"), status));



        return session.createQuery(criteriaQuery).getResultList();
    }

    private static void insertRequestRoutineExecution(
            RequestRoutine requestRoutine,
            Session session,
            String log) throws HibernateException {
        RequestRoutineExecution routineExecution = new RequestRoutineExecution(
                log,
                requestRoutine.getId(),
                new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()),
                requestRoutine.getStatus().getExecutionStatus(),
                requestRoutine.getTryCount()
        );
        session.beginTransaction();
        session.save(routineExecution);
        session.getTransaction().commit();
    }

    private static void updateRequestRoutine(
            RequestRoutine requestRoutine,
            Session session
    ) throws HibernateException {
        Transaction transaction = session.beginTransaction();
        session.save(requestRoutine);
        transaction.commit();
    }

}
