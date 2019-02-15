package com.abehrdigital.dicomprocessor.dao;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

public class PatientDao {
    private Session session;

    public PatientDao(Session session) {
        this.session = session;
    }

    public int getIdByNameAndDobAndGender(String firstName, String lastName, int dateOfBirth, String gender) {
        NativeQuery query = session.createSQLQuery("" +
                "SELECT p.id FROM patient p " +
                "JOIN contact c ON c.id = p.contact_id " +
                "WHERE p.dob = :date_of_birth " +
                "AND p.gender = :gender " +
                "AND c.first_name = :first_name " +
                "AND c.last_name = :last_name ")
                .setParameter("date_of_birth", dateOfBirth)
                .setParameter("gender", gender)
                .setParameter("first_name", firstName)
                .setParameter("last_name", lastName);

        return (int) query.getResultList().get(0);
    }

    public int getIdByHosNumAndDobAndGender(int hospitalNumber, int dateOfBirth, String gender) {
        NativeQuery query = session.createSQLQuery("" +
                "SELECT p.id FROM patient p " +
                "JOIN contact c ON c.id = p.contact_id " +
                "WHERE p.dob = :date_of_birth " +
                "AND p.gender = :gender " +
                "AND p.hos_num = :hospital_number ")
                .setParameter("date_of_birth", dateOfBirth)
                .setParameter("gender", gender)
                .setParameter("hospital_number", hospitalNumber);
        return (int) query.getResultList().get(0);
    }
}