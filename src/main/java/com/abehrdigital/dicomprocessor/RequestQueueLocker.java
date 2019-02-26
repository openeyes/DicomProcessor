package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.dao.RequestQueueDao;
import com.abehrdigital.dicomprocessor.dao.RequestQueueLockDao;
import com.abehrdigital.dicomprocessor.exceptions.RequestQueueMissingException;
import com.abehrdigital.dicomprocessor.models.RequestQueueLock;
import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.abehrdigital.dicomprocessor.utils.StackTraceUtil.getStackTraceAsString;

public class RequestQueueLocker {
    private final static int FIVE_SECONDS = 5;

    private Session session;
    private RequestQueueLockDao requestQueueLockDao;
    private RequestQueueDao requestQueueDao;
    private String requestQueueName;

    public RequestQueueLocker(String requestQueueName) {
        this.requestQueueName = requestQueueName;
        session = HibernateUtil.getSessionFactory().openSession();
        requestQueueLockDao = new RequestQueueLockDao(session);
        requestQueueDao = new RequestQueueDao(session);
    }

    public void lockWithMaximumTryCount(int maximumTryCount) throws Exception {
        boolean queueLocked = false;

        if (requestQueueExists()) {
            for (int tryCount = 0; tryCount < maximumTryCount; tryCount++) {
                try {
                    if (!session.getTransaction().isActive()) {
                        session.beginTransaction();
                    }
                    session.clear();
                    RequestQueueLock queueLock = requestQueueLockDao.getWithLock(requestQueueName, LockMode.UPGRADE_NOWAIT);

                    if (queueLock == null) {
                        createRequestQueueLock();
                    } else {
                        queueLocked = true;
                        break;
                    }
                } catch (HibernateException exception) {
                    if (session.getTransaction() != null) {
                        session.getTransaction().rollback();
                    }
                    sleepFiveSeconds();
                }
            }
            if (!queueLocked) {
                lockingFailed();
            }
        } else {
            throw new RequestQueueMissingException("Request queue: " + requestQueueName + " doesn't exist in the database");
        }
    }

    private boolean requestQueueExists() {
        return requestQueueDao.get(requestQueueName) != null;
    }

    private void createRequestQueueLock() {
        RequestQueueLock queueLock = new RequestQueueLock(requestQueueName);
        requestQueueLockDao.save(queueLock);
        session.getTransaction().commit();
    }

    private void lockingFailed() throws Exception {
        Logger.getLogger(RequestQueueLocker.class.getName()).log(Level.SEVERE,
                null,
                "Queue Name " + requestQueueName
                        + " failed");
        throw new Exception("Lock failed for RequestQueue: " + requestQueueName);
    }

    private void sleepFiveSeconds() {
        try {
            TimeUnit.SECONDS.sleep(FIVE_SECONDS);
        } catch (InterruptedException exception) {
            Logger.getLogger(RequestQueueLocker.class.getName()).log(Level.SEVERE, getStackTraceAsString(exception));
        }
    }

    public void unlock() {
        Transaction transaction = session.getTransaction();
        if (transaction.isActive()) {
            transaction.commit();
        }
    }
}
