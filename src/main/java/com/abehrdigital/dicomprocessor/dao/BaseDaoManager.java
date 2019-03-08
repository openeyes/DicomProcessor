package com.abehrdigital.dicomprocessor.dao;

import com.abehrdigital.dicomprocessor.models.RoutineLibrary;
import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public abstract class BaseDaoManager {
    private Session session;

    public Session getConnection() {
        if (this.session == null) {
            this.session = HibernateUtil.getSessionFactory().openSession();
        }
        return this.session;
    }

    public Object executeAndClose(DaoCommand command) {
        try {
            return command.execute(this);
        } finally {
            getConnection().close();
        }
    }

    public Object transaction(DaoCommand command) {
        Transaction transaction = null;
        try {
            transaction = getConnection().beginTransaction();
            Object returnValue = command.execute(this);
            transaction.commit();
            return returnValue;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e; //or wrap it before rethrowing it
        }
    }

    public Object transactionAndClose(DaoCommand command) {
        return executeAndClose(new DaoCommand() {
            public Object execute(BaseDaoManager manager) {
                return manager.transaction(command);
            }
        });
    }

    public void flushSession() {
        getConnection().flush();
    }

    public void clearSession() {
        getConnection().clear();
    }

    public void shutDown() {
        if(session != null && session.isConnected()) {
            getConnection().disconnect();
        }
    }

    public void refresh(Object object) {
        getConnection().refresh(object);
    }

    public void commit() {
        getConnection().getTransaction().commit();
    }

    public void rollback() {
        if (getConnection().getTransaction() != null) {
            getConnection().getTransaction().rollback();
        }
    }

    public void transactionStart() {
        if (!getConnection().getTransaction().isActive())
            getConnection().beginTransaction();
    }

}
