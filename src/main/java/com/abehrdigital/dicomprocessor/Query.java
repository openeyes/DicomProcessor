package com.abehrdigital.dicomprocessor;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;

import java.util.*;

public class Query {

    private String query;                           // resulting query
    private String dataSet; 						// table name
    private String XID;                             // encoding for current id, stored as key in Test.XID_map
    private String CRUD; 							// e.g. retrieve, create, merge
    private TreeMap<String, String> unknownFields;	// e.g. {name=>$$_tableXID.fieldName_$$, date=>$$_tableXID.fieldDate_$$, }
    private TreeMap<String, String> knownFields;	// e.g. {id=>5, class_name="Examination", episode_id=245, }
    private TreeMap<String, ForeignKey> foreignKeys;// e.g. {referencing_XID=>(referenced_column,referencing_column), }

    Query(String dataSet, String XID, String CRUD, TreeMap<String, String> knownFields, TreeMap<String, String> unknownFields, TreeMap<String, ForeignKey> foreignKeys) {
        this.dataSet = dataSet;
        this.XID = XID;
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
    int constructAndRunQuery() throws Exception {

        // before constructing the query, update the list of known and unknown fields
        updateKnownUnknown();

        String secondary_query_insert;
        int rows_affected = 0;

        switch (this.CRUD) {
            case "create":
                secondary_query_insert = constructInsertQuery();
                // execute query and the secondary query
                rows_affected = executeQuery(Test.getSession(), secondary_query_insert);
                System.out.println("ins rows: " + rows_affected);
                break;
            case "retrieve":
                secondary_query_insert = constructSelectQuery();
                // execute select query
                rows_affected = executeQuery(Test.getSession(), secondary_query_insert);
                System.out.println("sel rows: " + rows_affected);
                break;
            case "merge":
                // if id is known, try to update fields
                if (Test.XID_map.containsKey(XID) && Test.XID_map.get(XID) !=  null) {
                    System.out.println("Value is here");

                    secondary_query_insert = constructUpdateQuery();

                    // when the ID is not known -> do queryByExample to retrieve it
                    // when the ID is known (present in the XID_map) -> update the rest of the columns
                    rows_affected = executeQuery(Test.getSession(), secondary_query_insert);
                    if (rows_affected != 1) {
                        throw new Exception("Could not update.");
                    }
                } else {
                    // no id, then try Retrieve.
                    //  if no rows are returned, then insert and get the newly introduced id
                    System.out.println("Value is not here");

                    this.CRUD = "retrieve";
                    secondary_query_insert = constructSelectQuery();

                    // execute select query
                    rows_affected = executeQuery(Test.getSession(), secondary_query_insert);

                    System.out.println("merge rows: " + rows_affected);

                    if (rows_affected == 0) {
                        // no rows returned: insert
                        this.CRUD = "create";

                        System.out.println("insert");
                        secondary_query_insert = constructInsertQuery();
                        System.out.println("Second: " + secondary_query_insert);
                        // execute query and the secondary query
                        rows_affected = executeQuery(Test.getSession(), secondary_query_insert);
                    } else if (rows_affected == 1) {
                        // records found
                        System.out.println("ONE Record found");
                    } else {
                        System.out.println("MORE THAN ONE record found");
                    }
                }

                if (Test.XID_map.containsKey(XID) && Test.XID_map.get(XID) !=  null) {
                    Test.printMap("Value is here", Test.XID_map);
                } else {
                    Test.printMap("Value is not here", Test.XID_map);
                }

                break;
            default:
                break;
        }
        return rows_affected;
    }

    /**
     * add to the list of KnownFields those that have a value in XID_map and remove them from UnknownFields
     * add to the list of KnownFields the foreign keys that have a value in XID_map
     */
    private void updateKnownUnknown() {
        Iterator itr = unknownFields.entrySet().iterator();

        while(itr.hasNext()) {
            Map.Entry<String, String> entryUnknown = (Map.Entry<String, String>) itr.next();
            String XID = entryUnknown.getValue();
            String id = entryUnknown.getKey();

            // if the value of the XID was previously computed, remove it from the unknown fields
            // and move it to the known fields
            if (Test.XID_map.containsKey(XID)) {
                XID XID_object = Test.XID_map.get(XID);

                if (XID_object != null && XID_object.knownFields != null && XID_object.knownFields.containsKey(id)) {
                    String prevFoundValue = XID_object.knownFields.get(id);
                    knownFields.put(id, prevFoundValue);
                    itr.remove();
                }
            }
        }

        /* Add to the known fields the foreign keys with a value received previously */
        for (Map.Entry<String, ForeignKey> foreignKeyEntry : foreignKeys.entrySet()) {
            String XID = foreignKeyEntry.getKey();
            ForeignKey foreignKey = foreignKeyEntry.getValue();

            if (Test.XID_map.containsKey(XID)) {
                XID XID_object = Test.XID_map.get(XID);
                if (XID_object != null && XID_object.knownFields != null && XID_object.knownFields.containsKey(foreignKey.referencing_column)) {
                    String prevFoundValue = XID_object.knownFields.get(foreignKey.referencing_column);
                    // save the value of foreign key in known fields
                    knownFields.put(foreignKey.referenced_column, prevFoundValue);
                }
            }
        }
    }

    /**
     * Construct the insert SQL query; construct a select query to determine the ID of the newly inserted row
     * @return query for selecting the id of the inserted row
     * @throws Exception Validation error
     */
    private String constructInsertQuery() throws Exception {
        // if there are fields still unknown, return error
        if (!validateFieldsInsert(Test.XID_map)) {
            System.err.println("validation failed");
            throw new Exception("Validation failed. Some information is not available.");
        }
        System.out.println("Validation correct:\n"+this);

        // all the unknown fields were found, append together the known & unknown maps to form the insert query
        StringBuilder keys = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (String s : unknownFields.keySet()) {
            // if not id (normal field)
            // or
            // is id and there is a known value for it in the XID_map
            if (!s.equals("id") || (Test.XID_map.get(unknownFields.get(s)) != null && Test.XID_map.get(unknownFields.get(s)).knownFields != null &&
                    Test.XID_map.get(unknownFields.get(s)).knownFields.containsKey(s))) {
                keys.append(s);
                keys.append(", ");
                String val = Test.XID_map.get(unknownFields.get(s)).knownFields.get(s);
                if (val == null) {
                    System.err.println("1+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*");
                    values.append("null, ");
                } else {
                    System.err.println("2+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*+*");
                    values.append("'");
                    values.append(val);
                    values.append("', ");
                }
            }
        }
        for (String s : knownFields.keySet()) {
            keys.append(s);
            keys.append(", ");
            if (knownFields.get(s) == null) {
                values.append(knownFields.get(s));
                values.append(", ");
            } else {
                values.append("'");
                values.append(knownFields.get(s));
                values.append("', ");
            }
        }
        // remove last ", "
        keys.delete(keys.length() - 2, keys.length());
        values.delete(values.length() - 2, values.length());

        this.query = "INSERT INTO " + this.dataSet + " (" + keys + ") VALUES (" + values + ");";

        /*
         * after the insert, we want to retrieve the id and all information inserted and put it back in the MAP
         * construct the SELECT query
         */

        // SELECT	-> the newly inserted id
        String select = "id";
        // WHERE	-> this.knownFields
        String condition = setToStringWithDelimiter(getEquals(knownFields, " is "), "AND");
        // condition might be null -> there is no WHERE clause
        String where = condition != null ? (" WHERE " + condition) : "";

        // return as second sql query "SELECT id FROM ..."
        return "SELECT " + select + " FROM " + this.dataSet + where;
    }

    /**
     * Construct the select SQL query to be executed
     * @return second query to be executed
     * @throws Exception Invalid request: there must be at least a field unknown
     */
    private String constructSelectQuery() throws Exception {
        if (unknownFields.keySet().size() <= 0) {
            throw new Exception("Invalid request: there are no fields specified for the retrieve operation!");
        }

        // SELECT	-> unknownFields
        String select = setToStringWithDelimiter(unknownFields.keySet().toArray(), ",");

        // WHERE	-> this.knownFields
        String condition = setToStringWithDelimiter(getEquals(knownFields, " is "), "AND");
        // condition might be null -> there is no WHERE clause
        String where = condition != null ? (" WHERE " + condition + ";") : "";

        // select the SQL query
        this.query =  "SELECT " + select + " FROM " + this.dataSet + where;

        // there is no secondary query to be executed after select
        return null;
    }

    /**
     * Construct the update SQL query to be executed
     * @return second query to be executed
     */
    private String constructUpdateQuery() {
        // select the SQL query
        this.query =  "UPDATE " + this.dataSet +
                " SET " + setToStringWithDelimiter(getEquals(knownFields, "="), ",") +
                " WHERE id='" + Test.XID_map.get(XID).knownFields.get("id") + "'";

        // there is no secondary query to be executed after select
        return null;
    }

    /**
     * Search if the unknown fields were assigned by a previous query
     * @param map hashmap to balidate
     * @return true if all unknownFields are found in the Test.XID_map; false if one unknown field was not found
     */
    private boolean validateFieldsInsert(HashMap<String, XID> map) {
        for (Map.Entry<String,String> entry : unknownFields.entrySet()) {
            String XID = entry.getValue();
            String id = entry.getKey();
            // skip the id field (it cannot exist in the database)
            if (!id.equals("id")) {
                // if the value is still unknown in the global map (still null)
                if (map.containsKey(XID)) {
                    if (map.get(XID) == null || map.get(XID).knownFields.get(id) == null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Get a list of conditions from the given map fields;
     * use "=" as delimiter for normal values
     * use "is" for NULLs
     *
     * @return array of Objects (strings)
     */
    private Object[] getEquals(TreeMap<String, String> map, String nullDelimiter) {
        Object[] o = new Object[map.size()];

        /* nothing to work with */
        if (map.size() <= 0) {
            return null;
        }

        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            // if value is null, use "key is null" syntax
            if (entry.getValue() == null) {
                o[i++] = entry.getKey() + nullDelimiter + entry.getValue();
            } else {
                // else, use "key = value"
                o[i++] = entry.getKey() + "='" + entry.getValue() + "'";
            }
        }

        return o;
    }

    /**
     * Get a string containing the objects in the array, delimited by given delimiter
     *
     * @param arr array of Objects to be concatenated into a string
     * @param delimiter String to be placed between elements of array
     * @return string concatenation of all elements in array
     */
    private String setToStringWithDelimiter(Object[] arr, String delimiter) {
        StringBuilder sb = new StringBuilder();

        if (arr == null || arr.length <= 0) {
            return null;
        }

        for (Object o : arr) {
            sb.append(o);
            sb.append(" ");
            sb.append(delimiter);
            sb.append(" ");
        }

        // remove last delimiter and space
        sb.delete(sb.length() - 2 - delimiter.length(), sb.length());

        return sb.toString();
    }

    /**
     * Execute SQL query
     *
     * TODO: Assume there is only one record returned
     *
     * @return array containing the columns returned by the sql query
     */
    @org.springframework.transaction.annotation.Transactional
    private int executeQuery(Session session, String secondary_query_insert) {
        NativeQuery SQLQuery = session.createSQLQuery(query);
        int rows_affected = -1;

        /* only if the crud is "create", execute the secondary query */
        if (CRUD.equals("create")) {
            session.beginTransaction();
            rows_affected = SQLQuery.executeUpdate();
            session.getTransaction().commit();

            // do the secondary query: for insert/update, it should be "SELECT id FROM TABLE WHERE (...)"
            SQLQuery = session.createSQLQuery(secondary_query_insert);
        }

        // get all the fields from the sql query
        List<Object[]> requestRoutines = SQLQuery.getResultList();
        SQLQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

        // !! Returned fields can be Integers, so the value in the HashMap must be a parent of all types:
        // Strings, Integer, etc. => Object must be used
        List<HashMap<String, Object>> aliasToValueMapList = SQLQuery.list();

        System.err.println("=====SQL response:=========" + aliasToValueMapList + "================");


        // append to the known fields of the XID object all the rows returned by the SQL
        TreeMap<String, String> knownFields = new TreeMap<>();
        for (HashMap<String, Object> result : aliasToValueMapList) {
            for (String key : result.keySet()) {
                knownFields.put(key, result.get(key).toString());
            }
        }

        // return the number of rows returned by the sql query
        if (rows_affected == -1) {
            rows_affected = aliasToValueMapList.size();
        }

        /*
            // iterate through the response
            for (Object person : requestRoutines) {
                if (person instanceof Object[]) {
                    System.err.println("THERE ARE MULTIPLE COLUMNS RETURNED");

                    //Object[] list = (Object[]) person;
                    //int i = 0;
                    //for (Map.Entry<String, String> entry : unknownFields.entrySet()) {
                    //    String value = person.toString();
                    //    String XID = unknownFields.firstEntry().getValue();
                    //    Test.XID_map.put(XID, new XID(XID, value, this.dataSet, knownFields));
                    //}

                } else {
                    System.out.println("=One column returned=");
                }
            }
        */

        // if unknownFields has no entries, it means there is no information required:
        // ex: insert with a known id
        if (unknownFields.size() > 0) {
            String XID = unknownFields.firstEntry().getValue();

            // if the XID is already in the MAP,
            // update known fields in the map with ones returned as a result of running the current sql query
            if (Test.XID_map.containsKey(XID) && Test.XID_map.get(XID) != null) {
                // knownFields is null; init treemap
                if (Test.XID_map.get(XID).knownFields == null) {
                    Test.XID_map.get(XID).knownFields = new TreeMap<>();
                }
                // put new fields in XID_map
                Test.XID_map.get(XID).knownFields.putAll(knownFields);
            } else {
                // XID is not present in the map; create and insert a new instance of XID object into the map
                Test.XID_map.put(XID, new XID(XID, this.dataSet, knownFields));
            }
        }

        Test.printMap("Test.map: ", Test.XID_map);

        return rows_affected;
    }

    /**
     * Get SQL style date time
     * @param session Sql session
     * @return string containing current system date time
     * @throws Exception Cannot get time from SQL
     */
    static String getTime(Session session) throws Exception {
        NativeQuery SQLQuery = session.createSQLQuery("SELECT NOW();");
        List<Object> requestRoutines = SQLQuery.getResultList();

        if (requestRoutines.size() != 1)
            throw new Exception("Could not get current datetime");

        return requestRoutines.get(0).toString();
    }
}
