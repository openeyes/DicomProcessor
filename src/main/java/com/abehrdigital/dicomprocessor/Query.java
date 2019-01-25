package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Query {

    public String query;
    public String dataSet; 							// table name
    public String CRUD; 							// command: retrieve, create..
    public TreeMap<String, String> unknownFields;	// name, date, parent_id
    public TreeMap<String, String> knownFields;		// TODO: LITERAL {id=>5, class_name="Examination", episode_id=245}

    public Query() {}
    public Query(String dataSet, String CRUD, TreeMap<String, String> knownFields, TreeMap<String, String> unknownFields) {
        this.dataSet = dataSet;
        this.CRUD = CRUD;
        this.knownFields = knownFields;
        this.unknownFields = unknownFields;
    }

    @Override
    public String toString() {
        return "Query [dataSet=" + dataSet + ", CRUD=" + CRUD + ", \n\tunknownFields=" + unknownFields + ", \n\tknownFields="
                + knownFields + "]\n";
    }

    // construct sql query
    public void constructQuery(HashMap<String, String> map) {
        // depending on the CRUD: CREATE(INSERT), READ(SELECT), UPDATE, DELETE
        switch (this.CRUD) {
            case "create":
                System.out.println("Create: " + map);
                query = constructInsertQuery(map);
                break;
            case "retrieve":
                System.out.println("Retrieve: ");
                query = constructSelectQuery();
                break;
            //TODO: MERGE ????
            case "update":
                query = constructUpdateQuery();
                break;
            case "delete":
                query = constructDeleteQuery();
                break;
            default:
                break;
        }
    }


    public String constructInsertQuery(HashMap<String, String> map) {
//		INSERT
//		INTO table_name
//		(column1, column2, column3, ...)
//		VALUES (value1, value2, value3, ...);
//
        // if there are fields still unknown, return error
        if (!validateFields(map)) {
            System.err.println("validation failed");
            return null;
        }

        StringBuilder keys = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (String s : unknownFields.keySet()) {
            keys.append(s + ", ");
            values.append("'" + map.get(unknownFields.get(s)) + "', ");
            System.out.println("== " + unknownFields.get(s) + "==" + map.get(unknownFields.get(s)) );
        }
        for (String s : knownFields.keySet()) {
            System.out.println("======" + s);
            keys.append(s + ", ");
            values.append("'" + knownFields.get(s) + "', ");
        }
        // remove last ", "
        keys.delete(keys.length() - 2, keys.length());
        values.delete(values.length() - 2, values.length());

        System.out.println(keys);
        System.out.println(values);

        // SELECT	-> unknownFields
        String select = setToStringWithDelimiter(unknownFields.keySet().toArray(), ",");

        // WHERE	-> this.knownFields
        String condition = setToStringWithDelimiter(getEquals(), ",");
        // condition might be null -> there is no WHERE clause
        String where = condition != null ? (" WHERE " + condition) : "";

        return "INSERT INTO " + this.dataSet + " (" + keys + ") VALUES (" + values + ");";
    }

    private boolean validateFields(HashMap<String, String> map) {
//		System.out.println(unknownFields.entrySet());
        for (Map.Entry<String,String> entry : unknownFields.entrySet()) {
            // skip the id field (it cannot exist in the database)
            if (!entry.getKey().equals("id")) {
//				System.out.println("1: " + entry.getKey());
                // if the value is still unknown in the global map (still null)
                if (map.containsKey(entry.getValue())) {
//					System.out.println("2: " + entry.getValue());
                    if (map.get(entry.getValue()) == null) {
//						System.out.println("3: Not valid====================== " + entry.getValue());
                        return false;
                    }
                    else {
//						System.out.println("4: " + entry.getValue());
                    }
                }
            } else {
//				System.out.println("====id");
            }
        }
//		System.out.println("ret");
        return true;
    }



    public String constructSelectQuery() {
        // SELECT	-> unknownFields
        String select = unknownFields.keySet().size() <= 0 ? "*" : setToStringWithDelimiter(unknownFields.keySet().toArray(), ",");

        // FROM 	-> dataset
        String from = this.dataSet;

        // WHERE	-> this.knownFields
        String condition = setToStringWithDelimiter(getEquals(), "AND");
        // condition might be null -> there is no WHERE clause
        String where = condition != null ? (" WHERE " + condition) : "";

        return "SELECT " + select + " FROM " + from + where;
    }

    public String constructUpdateQuery() {
//		UPDATE table_name
//		SET column1 = value1, column2 = value2,
//		WHERE condition;
        return "Update";
    }

    public String constructDeleteQuery() {
//		DELETE
//		FROM table_name
//		WHERE condition;
        return "Delete";
    }



    /**
     * Get a list of conditions from the known fields in the class
     *
     * @return
     */
    private Object[] getEquals() {
        Object[] o = new Object[knownFields.size()];

        if (knownFields.size() <= 0) {
            return null;
        }

        int i = 0;
        for (Map.Entry<String, String> entry : knownFields.entrySet()) {
            o[i++] = entry.getKey() + "='" + entry.getValue() + "'";
        }

        return o;
    }

    /**
     * Get a string containing the objects in the array, delimited by given delimiter
     *
     * @param arr
     * @param delimiter
     * @return
     */
    public String setToStringWithDelimiter(Object[] arr, String delimiter) {
        StringBuilder sb = new StringBuilder();

        if (arr == null || arr.length <= 0) {
            return null;
        }

        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i] + " " + delimiter + " ");
        }

        // remove last ", "
        sb.delete(sb.length() - 2 - delimiter.length(), sb.length());

        return sb.toString();
    }


    /**
     * Execute SQL query
     * <p>
     * TODO: Assume there is only one record returned
     *
     * @return array containing the columns returned by the sql query
     */
    @org.springframework.transaction.annotation.Transactional
    public HashMap<String, String> executeQuery() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        NativeQuery SQLQuery = session.createSQLQuery(query);
        if (CRUD.equals("create")) {
            session.beginTransaction();
            System.out.println(SQLQuery.executeUpdate());
            session.getTransaction().commit();
        } else {
            List<Object[]> requestRoutines = SQLQuery.getResultList();
//            SQLQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
//            List<Map<String,Object>> aliasToValueMapList=query.list();

            SQLQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
            List<Map<String,Object>> aliasToValueMapList=SQLQuery.list();

            System.out.println(requestRoutines);
            System.out.println("==============" +requestRoutines+"================");
            System.out.println("==============" +aliasToValueMapList+"================");

            for (Object person : requestRoutines) {
                if (person instanceof Object[]) {
                    Object[] list = (Object[]) person;
                    int i = 0;
                    for (Map.Entry<String, String> entry : unknownFields.entrySet()) {
                        Test.map.put(entry.getValue(), list[i++].toString());
                        System.out.print(list[i++].toString() + "   ");
                        System.out.print(entry.getKey() + "   " + entry.getValue());
                    }
                    System.out.println("=================1==");
                } else {
                    Test.map.put(unknownFields.firstEntry().getValue(), person.toString());
                    System.out.print(person.toString() + "   ");
                    System.out.print(unknownFields.firstEntry().getKey() + "   " + unknownFields.firstEntry().getValue());
                    System.out.println("=================2==");
                }
            }
        }
        System.out.println("Test.map: "+ Test.map);

        // TODO: the insert should return the new information (id of the new record)

        return null;
    }
}
