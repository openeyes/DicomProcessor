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
    public TreeMap<String, String> foreignKeys;

    public Query() {}
    public Query(String dataSet, String CRUD, TreeMap<String, String> knownFields, TreeMap<String, String> unknownFields, TreeMap<String, String> foreignKeys) {
        this.dataSet = dataSet;
        this.CRUD = CRUD;
        this.knownFields = knownFields;
        this.unknownFields = unknownFields;
        this.foreignKeys = foreignKeys;
    }

    @Override
    public String toString() {
        return "Query [dataSet=" + dataSet + ", CRUD=" + CRUD + ", \n\tunknownFields=" + unknownFields + ", \n\tknownFields="
                + knownFields + "]\n";
    }

    // construct sql query
    public String constructQuery(HashMap<String, XID> map) {
        switch (this.CRUD) {
            case "create":
                return constructInsertQuery(map);
            case "retrieve":
                return constructSelectQuery();
            case "update":
                return constructUpdateQuery();
            case "merge":
                return constructMergeQuery();
            default:
                break;
        }
        return null;
    }

    /**
     * Search if the unknown fields were assigned by a previous query
     * @param map
     * @return
     */
    private boolean validateFieldsInsert(HashMap<String, XID> map) {
//		System.out.println(unknownFields.entrySet());
        for (Map.Entry<String,String> entry : unknownFields.entrySet()) {
            // skip the id field (it cannot exist in the database)
            if (!entry.getKey().equals("id")) {
//				System.out.println("1: " + entry.getKey());
                // if the value is still unknown in the global map (still null)
                if (map.containsKey(entry.getValue())) {
//					System.out.println("2: " + entry.getValue());
                    if (map.get(entry.getValue()) == null || map.get(entry.getValue()).value == null) {
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


    public String constructInsertQuery(HashMap<String, XID> map) {

        /* construct the INSERT query */

        // if there are fields still unknown, return error
        if (!validateFieldsInsert(map)) {
            System.err.println("validation failed");
            return null;
        }

        // all the unknown fields were found, append together the known & unknown maps to form the insert query
        StringBuilder keys = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (String s : unknownFields.keySet()) {
            keys.append(s + ", ");
            values.append("'" + map.get(unknownFields.get(s)) + "', ");
        }
        for (String s : knownFields.keySet()) {
            keys.append(s + ", ");
            values.append("'" + knownFields.get(s) + "', ");
        }
        // remove last ", "
        keys.delete(keys.length() - 2, keys.length());
        values.delete(values.length() - 2, values.length());

        this.query = "INSERT INTO " + this.dataSet + " (" + keys + ") VALUES (" + values + ");";





        /*
         * after the insert, we want to retrieve the id and all information inserted and put it back in the MAP
         * construct the SELECT query
         */

        // SELECT	-> *
        String select = "*";
        // FROM 	-> dataset
        String from = this.dataSet;
        // WHERE	-> this.knownFields
        String condition = setToStringWithDelimiter(getEquals(knownFields), "AND");
        // condition might be null -> there is no WHERE clause
        String where = condition != null ? (" WHERE " + condition) : "";

        return "SELECT " + select + " FROM " + from + where;
    }

    public String constructSelectQuery() {
        // SELECT	-> unknownFields
        String select = unknownFields.keySet().size() <= 0 ? "*" : setToStringWithDelimiter(unknownFields.keySet().toArray(), ",");

        // FROM 	-> dataset
        String from = this.dataSet;

        // WHERE	-> this.knownFields
        String condition = setToStringWithDelimiter(getEquals(unknownFields), "AND");
        // condition might be null -> there is no WHERE clause
        String where = condition != null ? (" WHERE " + condition + ";") : "";

        return "SELECT " + select + " FROM " + from + where;
    }

    public String constructUpdateQuery() {
//		UPDATE table_name
//		SET column1 = value1, column2 = value2,
//		WHERE condition;
        /*
        $$_ROW_$$": [
    {
        "$$_CRUD_$": "update",
        "id": "$$_event_type[1]_$$",
        "episode_id": "$$_episode[1][1]_$$",
        "event_type_id": "$$_event_type[1][1]_$$"
    }, {
        "$$_CRUD_$": "update",
        "id": "$$_event_type[2][1]_$$",         // gives the row in the table to be updated
        "episode_id": "$$_episode[1][1]_$$",
        "event_type_id": "$$_event_type[1][1]_$$"
    }
]

ok UPDATE first... parsing and executing TOP TO BOTTOM,
UPDATE owner.table_name a
SET a.id = 32329, a.episode_id = 23, a.event_type_id = 23 /* simple substitution looking up values in memory mapping table .
        WHERE a.id = 32329 /* condition derived from PK of metadata about table obtained from database data dictionary /
         */
        return "Update";
    }

    public String constructMergeQuery() {
/*
A MERGE will translates to two operations on the database .... as follows:

UPDATE owner.table_name a
SET a.id = 32329, a.episode_id = 23, a.event_type_id = 23 /* simple substitution looking up values in memory mapping table *
        WHERE a.id = 32329 /* condition derived from PK of metadata about table obtained from database data dictionary *

        IF one rows updated THEN move on to next parse row in file
        IF two or more rows updated THEN rollback and fail
        IF no rows updated THEN ....
        INSERT INTO owner.table_name a
        ( id, episode_id, event_type_id)
        VALUES (NULL, 23, 23 ) /* simple substitution of lookup values less the PK auto_number field
Then the function to get the autoallocated number needs to run and update the id map memory for ID
To be honest I am not sure if ID and NULL should be explicitly excluded for statement for auto allocate work. It may be.
The simple rule for MERGE is, if the UPDATE updates no rows then create an INSERT statement. When doing the insert, if the substitution lookup values are in the map table, then substitute, OTHERWISE if it is auto allocate column, then exclude from statement without that column (or NULL as above) and then read off the allocated number immediately following the  successful insert and add this back to memory map. an end of all processing make sure the map lookup memory table can be serialsed back to JSON for next time around.
THE OVERALL NET EFFECT.
MERGE OPERATIONS can be run multiple times, and first insert, then subsequently UPDATE from that point forward. Initial autoallocated numbers carry forward to the UPDATE (so duplicate erroneous records are not created)
 */
        return "Delete";
    }



    /**
     * Get a list of conditions from the known fields in the class
     *
     * @return
     */
    private Object[] getEquals(TreeMap<String, String> map) {
        Object[] o = new Object[map.size()];

        if (map.size() <= 0) {
            return null;
        }

        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
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
    public HashMap<String, String> executeQuery(String secondary_query_insert) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        NativeQuery SQLQuery = session.createSQLQuery(query);
        if (CRUD.equals("create")) {
            session.beginTransaction();
            int rows_affected = SQLQuery.executeUpdate();
            System.err.println("rows_affected: " + rows_affected);
            session.getTransaction().commit();

            // do the secondary query: for insert/update, it should be "SELECT * FROM TABLE WHERE (...)"
            SQLQuery = session.createSQLQuery(secondary_query_insert);
        } else {

        }

        // get all the fields from the sql query
        List<Object[]> requestRoutines = SQLQuery.getResultList();
        SQLQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        List<HashMap<String, String>> aliasToValueMapList = SQLQuery.list();

        System.err.println("==============" + aliasToValueMapList + "================");

        // append to the known fields of the XID object all the rows returned by the SQL
        TreeMap<String, String> knownFields = new TreeMap<>();
        for (HashMap<String, String> result : aliasToValueMapList) {
            knownFields.putAll(result);
        }

        System.err.println("knownFields: === " + knownFields);

        // TODO
        Test.XID_map.put("??????", new XID("?????", "??????", this.dataSet, knownFields));

            //TODO
            //TODO: CONTINUE FROM HERE
            //TODO
//            for (Object person : requestRoutines) {
//                if (person instanceof Object[]) {
//                    Object[] list = (Object[]) person;
//                    int i = 0;
//                    for (Map.Entry<String, String> entry : unknownFields.entrySet()) {
//
////                        String value = person.toString();
////                        String XID = unknownFields.firstEntry().getValue();
////                        Test.XID_map.put(XID, new XID(XID, value, this.dataSet, knownFields));
////
////                        Test.XID_map.put(entry.getValue(), list[i++].toString());
//                        System.out.print(list[i++].toString() + "   ");
//                        System.out.print(entry.getKey() + "   " + entry.getValue());
//                    }
//                    System.out.println("=================1==");
//                } else {
//                    String value = person.toString();
//                    String XID = unknownFields.firstEntry().getValue();
//                    Test.XID_map.put(XID, new XID(XID, value, this.dataSet, knownFields));
//                }
//            }
//
        System.out.println("Test.map: "+ Test.XID_map);

        // TODO: the insert should return the new information (id of the new record)

        return null;
    }
}
