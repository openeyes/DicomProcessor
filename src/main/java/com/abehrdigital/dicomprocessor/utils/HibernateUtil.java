/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.utils;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static SessionFactory sessionFactory = null;

    public static void buildSessionFactory(Configuration hibernateConfig) {
        try {
            // Use hibernate.cfg.xml to get a SessionFactory
            sessionFactory = hibernateConfig.buildSessionFactory();
        } catch (Exception ex) {
            System.err.println("SessionFactory creation failed." + ex);
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
