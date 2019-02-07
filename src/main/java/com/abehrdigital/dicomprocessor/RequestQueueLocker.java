package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.dao.RequestQueueDao;
import com.abehrdigital.dicomprocessor.dao.RequestQueueLockDao;
import com.abehrdigital.dicomprocessor.models.RequestQueueLock;
import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestQueueLocker {
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
                    if (establishQueueLock()) {
                        queueLocked = true;
                        break;
                    }
                } catch (HibernateException exception) {
                    if(session.getTransaction() != null){
                        session.getTransaction().rollback();
                    }
                    sleepFiveSeconds();
                }
            }

            if (!queueLocked) {
                lockingFailed();
            }
        } else {
            throw new Exception("Request queue doesn't exist");
        }
    }

    private boolean requestQueueExists() {
        return requestQueueDao.get(requestQueueName) != null;
    }

    private boolean establishQueueLock() {
        if (!session.getTransaction().isActive()) {
            session.beginTransaction();
        }
        session.clear();
        RequestQueueLock queueLock = requestQueueLockDao.getWithLock(requestQueueName, LockMode.UPGRADE_NOWAIT);

        if (queueLock == null) {
            createRequestQueueLock();
            establishQueueLock();
        } else {
            return true;
        }
        return false;
    }

    private void createRequestQueueLock() {
        RequestQueueLock queueLock = new RequestQueueLock(requestQueueName);
        requestQueueLockDao.save(queueLock);
        session.getTransaction().commit();
    }

    private void lockingFailed() throws Exception {
        Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                null,
                "Queue Name " + requestQueueName
                        + " failed");
        throw new Exception("Lock failed for RequestQueue: " + requestQueueName);
    }

    private void sleepFiveSeconds() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException ex) {
            Logger.getLogger(RequestQueueExecutor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void unlock() {
        Transaction transaction = session.getTransaction();
        if (transaction.isActive()) {
            transaction.commit();
        }
    }
}
