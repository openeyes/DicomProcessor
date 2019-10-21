/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.payloadprocessor.utils;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static SessionFactory sessionFactory = null;

    public static boolean buildSessionFactory(Configuration hibernateConfig) {
        try {
            // Use hibernate.cfg.xml to get a SessionFactory
            sessionFactory = hibernateConfig.buildSessionFactory();
            return true;
        } catch (Exception ex) {
            System.err.println("SessionFactory creation failed.");
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            buildDefaultSessionFactory();
        }
        return sessionFactory;
    }

    private static void buildDefaultSessionFactory() {
        buildSessionFactory(new Configuration().configure());
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
