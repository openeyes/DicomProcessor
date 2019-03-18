package com.abehrdigital.dicomprocessor.utils;

import org.hibernate.query.Query;

import java.util.List;

public class QueryResultUtils {
    public static Object getFirstResultOrNull(Query query) {
        List results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }
}
