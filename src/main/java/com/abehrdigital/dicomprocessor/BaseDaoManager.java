package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public abstract class BaseDaoManager {
    private Session session;

    protected Session getConnection() {
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
}
