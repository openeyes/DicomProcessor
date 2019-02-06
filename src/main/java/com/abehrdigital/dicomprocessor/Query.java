package com.abehrdigital.dicomprocessor;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;

import java.util.*;

public class Query {

    public String query;
    public String dataSet; 							// table name
    public String CRUD; 							// command: retrieve, create..
    public TreeMap<String, String> unknownFields;	// name, date, parent_id
    public TreeMap<String, String> knownFields;		// TODO: LITERAL {id=>5, class_name="Examination", episode_id=245}
    public TreeMap<String, ForeignKey> foreignKeys;

    public Query() {}
    public Query(String dataSet, String CRUD, TreeMap<String, String> knownFields, TreeMap<String, String> unknownFields, TreeMap<String, ForeignKey> foreignKeys) {
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
    public int constructAndRunQuery() throws Exception {

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
                constructSelectQuery();
                // execute select query
                rows_affected = executeQuery(Test.getSession(), null);
                System.out.println("sel rows: " + rows_affected);
                break;
            case "merge":
                this.CRUD = "retrieve";
                constructSelectQuery();
                // execute select query
                rows_affected = executeQuery(Test.getSession(), null);

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
                    // only one row returned: update
                    System.out.println("update");

                    // TODO: need this here?
                    // this.CRUD = "update";

                    //TODO
                    // Construct UPDATE
                    // ....
                    constructUpdateQuery();
                    System.out.println("Query: " + this.query);

                    // when the ID is not known -> do queryByExample to retrieve it
                    // when the ID is known (present in the XID_map) -> update the rest of the columns
                    rows_affected = executeQuery(Test.getSession(), null);
                    if (rows_affected != 1) {
                        throw new Exception("Could not update.");
                    }

                } else {
                    // many rows returned: do nothing
                }

                this.CRUD = "merge";
                System.out.println("===");
                System.out.println(unknownFields);
                System.out.println(knownFields);
                System.out.println("===");
                break;
            default:
                break;
        }
        return rows_affected;
    }

    /**
     * Search if the unknown fields were assigned by a previous query
     * @param map
     * @return
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


    public String constructInsertQuery() throws Exception {

        /* construct the INSERT query */

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
            /*
            if (map.get(unknownFields.get(s)) != null) {
                if (map.get(unknownFields.get(s)).knownFields != null) {
                    if (map.get(unknownFields.get(s)).knownFields.containsKey(s)) {
                        System.out.println("1: " + s + "==" + !s.equals("id") + ", " + (map.get(unknownFields.get(s)) != null) + ", " + (map.get(unknownFields.get(s)).knownFields != null) + ", " + map.get(unknownFields.get(s)).knownFields.containsKey(s));
                    } else {
                        System.out.println("2: " + s + "==" + !s.equals("id") + ", " + (map.get(unknownFields.get(s)) != null) + ", " + (map.get(unknownFields.get(s)).knownFields != null));
                    }
                } else {
                    System.out.println("3: " + s + "==" + !s.equals("id") + ", " + (map.get(unknownFields.get(s)) != null));
                }
            } else {
                System.out.println("4: " + s + "==" + !s.equals("id"));
            }
            */

            // if not id (normal field)
            // or
            // is id and there is a known value for it in the XID_map
            if (!s.equals("id") || (Test.XID_map.get(unknownFields.get(s)) != null && Test.XID_map.get(unknownFields.get(s)).knownFields != null &&
                    Test.XID_map.get(unknownFields.get(s)).knownFields.containsKey(s))) {
                keys.append(s + ", ");
                values.append("'" + Test.XID_map.get(unknownFields.get(s)).knownFields.get(s) + "', ");
            }
        }
        for (String s : knownFields.keySet()) {
            keys.append(s + ", ");
            values.append("'" + knownFields.get(s) + "', ");
        }
        // remove last ", "
        keys.delete(keys.length() - 2, keys.length());
        values.delete(values.length() - 2, values.length());

        this.query = "INSERT INTO " + this.dataSet + " (" + keys + ") VALUES (" + values + ");";

        System.out.println("+++++++++++++++++++");
        System.out.println(this.query);



        /*
         * after the insert, we want to retrieve the id and all information inserted and put it back in the MAP
         * construct the SELECT query
         */

        // SELECT	-> the newly inserted id
        String select = "id";
        // FROM 	-> dataset
        String from = this.dataSet;
        // WHERE	-> this.knownFields
        String condition = setToStringWithDelimiter(getEquals(knownFields), "AND");
        // condition might be null -> there is no WHERE clause
        String where = condition != null ? (" WHERE " + condition) : "";

        return "SELECT " + select + " FROM " + from + where;
    }

    public String constructSelectQuery() throws Exception {
        if (unknownFields.keySet().size() <= 0) {
            throw new Exception("Invalid request: there are no fields specified for the retrieve operation!");
        }

        // SELECT	-> unknownFields
        String select = setToStringWithDelimiter(unknownFields.keySet().toArray(), ",");

        // FROM 	-> dataset
        String from = this.dataSet;

        // WHERE	-> this.knownFields
        String condition = setToStringWithDelimiter(getEquals(knownFields), "AND");
        // condition might be null -> there is no WHERE clause
        String where = condition != null ? (" WHERE " + condition + ";") : "";

        // select the SQL query
        this.query =  "SELECT " + select + " FROM " + from + where;

        // there is no secondary query to be executed after select
        return null;
    }

    /**
     * add to the list of KnownFields those that have a value in XID_map
     * remove from UnknownFields those that have a value in XID_map
     */
    public void updateKnownUnknown() {
        Iterator itr = unknownFields.entrySet().iterator();

        while(itr.hasNext()) {
            Map.Entry<String, String> entryUnknown = (Map.Entry<String, String>)itr.next();
            String XID = entryUnknown.getValue();
            String id = entryUnknown.getKey();

            // if the value of the XID was previously computed, remove it from the unknown fields
            // and move it to the known fields
            if (Test.XID_map.containsKey(XID)) {
                XID XID_object = Test.XID_map.get(XID);
                //TODO 3.1: check if correct
                System.out.println("SS: " + id + "   " + XID + "   " + XID_object);

//                if (XID_object != null) {
//                    if (XID_object.knownFields != null) {
//                        if (XID_object.knownFields.containsKey(id)) {
//                            System.out.println("1+++" + id + "++++++++: " + (XID_object != null) + "==" + (XID_object.knownFields != null) + "--" + (XID_object.knownFields.containsKey(id)));
//                        } else {
//                            System.out.println("2+++" + id + "++++++++: " + (XID_object != null) + "==" + (XID_object.knownFields != null));
//                        }
//                    } else {
//                        System.out.println("3++" + id + "+++++++++: " + (XID_object != null) + "==" + (XID_object.knownFields != null));
//                    }
//                } else {
//                    System.out.println("4++" + id + "+++++++++: " + (XID_object != null));
//                }
//                System.out.println(foreignKeys);

                if (XID_object != null && XID_object.knownFields != null && XID_object.knownFields.containsKey(id)) {
                    String prevFoundValue = XID_object.knownFields.get(id);
                    // TODO 3.1: check if correct
                    knownFields.put(id, prevFoundValue);
                    itr.remove();
                    System.out.println("UPDATE AND REMOVE");
                }
            } else {
                System.out.println("SS else: " + id + "   " + XID);
            }
        }

        for (Map.Entry<String, ForeignKey> foreignKeyEntry : foreignKeys.entrySet()) {
            String XID = foreignKeyEntry.getKey();
            ForeignKey foreignKey = foreignKeyEntry.getValue();
            System.out.println("FK: " + XID + "   " + foreignKey.referenced_column + "   " + foreignKey.referencing_column);
            if (Test.XID_map.containsKey(XID)) {
                XID XID_object = Test.XID_map.get(XID);
                if (XID_object != null && XID_object.knownFields != null && XID_object.knownFields.containsKey(foreignKey.referencing_column)) {
                    String prevFoundValue = XID_object.knownFields.get(foreignKey.referencing_column);
                    // TODO: what is the id for this foreign key???
                    /*
                    foreignkey: {"$$_service[1]_$$" => {referenced_column = "id", referencing_column = "service_id"}}
                     */
                    knownFields.put(foreignKey.referenced_column, prevFoundValue);

                    System.out.println("FOREIGN KEY found: " + XID_object.knownFields.get(foreignKey.referencing_column));
                }
            }
        }

        System.out.println("AFTER UPDATE: " + unknownFields);
        System.out.println("AFTER UPDATE: " + knownFields);
    }


    public String constructUpdateQuery() {
        /*
            UPDATE owner.table_name a
            SET a.id = 32329, a.episode_id = 23, a.event_type_id = 23
            WHERE a.id = 32329
         */
//        if (unknownFields.keySet().size() <= 0) {
//            throw new Exception("Invalid request: there are no fields specified for the retrieve operation!");
//        }

        // FROM 	-> dataset
        String table = this.dataSet;

        // WHERE	-> this.knownFields
        String fields = setToStringWithDelimiter(getEquals(knownFields), ",");

        // select the SQL query
        this.query =  "UPDATE " + table + " SET " + fields + " WHERE id='" + Test.XID_map.get(unknownFields.get("id")).knownFields.get("id") + "'";

        // there is no secondary query to be executed after select
        return null;
    }

    /**
     * update if one row is affected,
     * insert if no rows are affected
     * no changes if multiple rows are affected
     * @return
     */
    public String constructMergeQuery() throws Exception {
        //TODO: if there is no "created_date", insert it as NOW
        System.out.println("=========================================================================================");
        System.out.println(unknownFields);
        System.out.println(knownFields);
        System.out.println("=========================================================================================");

        String secondQuery = constructSelectQuery();
        if (this.query == null) {
            return null;
        }
        System.out.println("query: " + query);

//        int row_affected = executeQuery(Test.getSession(), secondQuery);
//        System.out.println("result: " + row_affected);

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
        return null;
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
    public int executeQuery(Session session, String secondary_query_insert) {
        NativeQuery SQLQuery = session.createSQLQuery(query);
        int rows_affected = -1;

        /* only if the crud is "create", execute the secondary query */
        if (CRUD.equals("create")) {
            session.beginTransaction();
            rows_affected = SQLQuery.executeUpdate();
            System.err.println("rows_affected: " + rows_affected);
            session.getTransaction().commit();

            // do the secondary query: for insert/update, it should be "SELECT * FROM TABLE WHERE (...)"
            SQLQuery = session.createSQLQuery(secondary_query_insert);
        }

        // get all the fields from the sql query
        List<Object[]> requestRoutines = SQLQuery.getResultList();
        SQLQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

        // !! Returned fields can be Integers, so the value in the HashMap must be a parent of all types:
        // Strings, Integer, etc. => Object must be used
        List<HashMap<String, Object>> aliasToValueMapList = SQLQuery.list();

        System.err.println("==============" + aliasToValueMapList + "================");


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

        for (Object person : requestRoutines) {
            if (person instanceof Object[]) {
                System.err.println("THERE ARE MULTIPLE COLUMNS RETURNED");
                /*
                    Object[] list = (Object[]) person;
                    int i = 0;
                    for (Map.Entry<String, String> entry : unknownFields.entrySet()) {
                        String value = person.toString();
                        String XID = unknownFields.firstEntry().getValue();
                        Test.XID_map.put(XID, new XID(XID, value, this.dataSet, knownFields));
                    }
                */
            } else {
//                System.out.println("=================2==: " + knownFields);

                // if unknownFields has no entries, it means there is no information required:
                // ex: insert with a known id
                if (unknownFields.size() > 0) {
                    String XID = unknownFields.firstEntry().getValue();
//                    System.out.println("Bef: " + this.knownFields);
//                    this.knownFields.putAll(knownFields);
//                    System.out.println("Aft: " + this.knownFields);

                    //TODO: what if the XID is already in the MAP
                    // ??????????????????????????????????????
                    // will be overwriten
                    Test.XID_map.put(XID, new XID(XID, this.dataSet, knownFields));
                }
            }
        }

        Test.printMap("Test.map: ", Test.XID_map);

        return rows_affected;
    }

    public static String getTime(Session session) throws Exception {
        NativeQuery SQLQuery = session.createSQLQuery("SELECT NOW();");
        List<Object> requestRoutines = SQLQuery.getResultList();

        if (requestRoutines.size() != 1)
            throw new Exception("Could not get current datetime");

        return requestRoutines.get(0).toString();
    }
}
