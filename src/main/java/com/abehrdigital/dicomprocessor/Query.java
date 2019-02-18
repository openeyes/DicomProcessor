package com.abehrdigital.dicomprocessor;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

public class Query {

    private String query;
    private String dataSet;
    private String XID;
    private CRUD crud;
    private TreeMap<String, String> unknownFields;
    private TreeMap<String, String> knownFields;
    private TreeMap<String, ForeignKey> foreignKeys;
    private final String SPACE = " ";
    private final String COMA_SPACE = ", ";
    private final String SINGLE_QUOTE  = "'";

    enum CRUD {
        retrieve, merge, create
    }

    Query(String dataSet, String XID, CRUD crud, TreeMap<String, String> knownFields, TreeMap<String, String> unknownFields,
          TreeMap<String, ForeignKey> foreignKeys ) {
        this.dataSet = dataSet;
        this.XID = XID;
        this.crud = crud;
        this.knownFields = knownFields;
        this.unknownFields = unknownFields;
        this.foreignKeys = foreignKeys;
    }

    @Override
    public String toString() {
        return "Query [dataSet=" + dataSet + ", crud=" + crud +
                ", \n\tunknownFields=" + unknownFields +
                ", \n\tknownFields=" + knownFields +
                ", \n\tforeignKeys=" + foreignKeys +
                "]\n";
    }

    // construct sql query
    int constructAndRunQuery() throws Exception {
        // before constructing the query, update the list of known fields
        updateKnownFields();
        updateKnownFieldsWithForeignKeys();

        String secondaryQueryInsert;
        int rowsAffected = 0;

        switch (this.crud) {
            case retrieve:
                constructSelectQuery();

                // execute select query
                // there is no secondary query to be executed after select
                rowsAffected = executeQuery(DataAPI.getSession(), null);
                break;
            case merge:
                // if id is known, try to update fields
                String primaryKey = DataAPI.keyIndex.get(dataSet).pk;

                // no id, then try Retrieve.
                //  if no rows are returned, then insert and get the newly introduced id
                this.crud = CRUD.retrieve;
                constructSelectQuery();

                // execute select query
                // there is no secondary query to be executed after select
                rowsAffected = executeQuery(DataAPI.getSession(), null);

                if (rowsAffected == 0) {
                    // no rows returned: insert
                    this.crud = CRUD.create;
                    secondaryQueryInsert = constructInsertQuery();

                    // execute query and the secondary query
                    rowsAffected = executeQuery(DataAPI.getSession(), secondaryQueryInsert);
                } else if (rowsAffected == 1) {
                    // records found
                    if (DataAPI.dataDictionary.containsKey(XID) && DataAPI.dataDictionary.get(XID) != null &&
                            DataAPI.dataDictionary.get(XID).knownFields != null && DataAPI.dataDictionary.get(XID).knownFields.get(primaryKey) != null) {
                        constructUpdateQuery();
                        this.crud = CRUD.create;
                        // when the ID is not known -> do queryByExample to retrieve it
                        // when the ID is known (present in the dataDictionary) -> update the rest of the columns
                        // there is no secondary query to be executed after update: all information already in dataDictionary
                        rowsAffected = executeQuery(DataAPI.getSession(), null);
                        if (rowsAffected != 1) {
                            throw new Exception("Could not update.");
                        }
                    }
                }
                break;
            default:
                break;
        }
        return rowsAffected;
    }

    /**
     * add to the list of KnownFields those that have a value in dataDictionary and remove them from UnknownFields
     */
    private void updateKnownFields() throws Exception {
        Iterator unknownFieldsIterator = unknownFields.entrySet().iterator();
        String primaryKey = DataAPI.keyIndex.get(dataSet).pk;

        /* search through all unknown fields and if the encoding "$$_..._$$" has a value in DataAPI.dataDictionary,
         * add it to the known fields and remove it from the unknownFields
         */
        while (unknownFieldsIterator.hasNext()) {
            String idUnknown = ((Map.Entry<String, String>) unknownFieldsIterator.next()).getKey();
            String XIDUnknown = unknownFields.get(idUnknown);

            // do not remove PK from the unknown fields
            if (primaryKey.equals(idUnknown)) {
                continue;
            }

            // replace "$$_SysDateTime_$$" with the time set at the start of the program execution
            if (XIDUnknown.equals("$$_SysDateTime_$$")) {
                knownFields.put(idUnknown, DataAPI.getTime());
                unknownFieldsIterator.remove();
                continue;
            }

            // if the value of the XID was previously computed, remove it from the unknown fields
            // and move it to the known fields
            if (DataAPI.dataDictionary.containsKey(XIDUnknown)) {
                XID xidObject = DataAPI.dataDictionary.get(XIDUnknown);

                if (xidObject != null && xidObject.knownFields != null && xidObject.knownFields.containsKey(idUnknown)) {
                    knownFields.put(idUnknown, xidObject.knownFields.get(idUnknown));
                    unknownFieldsIterator.remove();
                }
            }
        }
    }

    /**
     * add to the list of KnownFields the foreign keys that have a value in dataDictionary
     */
    private void updateKnownFieldsWithForeignKeys() {
        /* Add to the known fields the foreign keys with a value received previously and saved in DataAPI.dataDictionary */
        for (Map.Entry<String, ForeignKey> foreignKeyEntry : foreignKeys.entrySet()) {
            String XIDForeignKey = foreignKeyEntry.getKey();
            ForeignKey foreignKey = foreignKeyEntry.getValue();

            if (DataAPI.dataDictionary.containsKey(XIDForeignKey)) {
                XID xidObject = DataAPI.dataDictionary.get(XIDForeignKey);
                if (xidObject != null && xidObject.knownFields != null && xidObject.knownFields.containsKey(foreignKey.referencedColumn)) {
                    String previouslyFoundValue = xidObject.knownFields.get(foreignKey.referencedColumn);
                    // save the value of foreign key in known fields
                    knownFields.put(foreignKey.referencingColumn, previouslyFoundValue);
                }
            }
        }
    }

    /**
     * Add key and value to the two stringbuilders with formatting
     * @param keys StringBuilder
     * @param values StringBuilder
     * @param key String to be added to the keys
     * @param value String to be added to the values
     */
    private void addFieldToStringBuilder(StringBuilder keys, StringBuilder values, String key, String value) {
        keys.append(COMA_SPACE);
        keys.append(key);

        values.append(COMA_SPACE);
        values.append(SINGLE_QUOTE);
        values.append(value);
        values.append(SINGLE_QUOTE);
    }
    /**
     * Construct the insert SQL query; then construct a select query to determine the PK of the newly inserted row
     * IMPORTANT: the tables must have AUTO-INCREMENT set to be able to insert any rows
     *
     * @return query for selecting the id of the inserted row
     * @throws Exception Validation error
     */
    private String constructInsertQuery() throws Exception {
        // if there are fields still unknown, return error
        if (!validateFieldsForInsertStatement(DataAPI.dataDictionary)) {
            throw new Exception("Validation failed. Some information is not available.");
        }

        if (knownFields.size() < 1) {
            System.err.println("Nothing to insert.");
            this.query = null;
            return null;
        }

        // all the unknown fields were found, append together all fields and values to form the insert query
        StringBuilder keys = new StringBuilder();
        StringBuilder fields = new StringBuilder();
        for (String knownField : knownFields.keySet()) {
            keys.append(knownField);
            keys.append(COMA_SPACE);
            // replace string "null" with actual null
            if (knownFields.get(knownField) == null) {
                fields.append(knownFields.get(knownField));
                fields.append(COMA_SPACE);
            } else {
                fields.append(SINGLE_QUOTE);
                fields.append(knownFields.get(knownField));
                fields.append(SINGLE_QUOTE);
                fields.append(COMA_SPACE);
            }
        }
        // remove last ", "
        keys.delete(keys.length() - COMA_SPACE.length(), keys.length());
        fields.delete(fields.length() - COMA_SPACE.length(), fields.length());

        if (knownFields.get("last_modified_date") == null) {
            addFieldToStringBuilder(keys, fields,"last_modified_date", DataAPI.getTime());
        }

        if (knownFields.get("created_date") == null) {
            addFieldToStringBuilder(keys, fields,"created_date", DataAPI.getTime());
        }

        this.query = String.format("INSERT INTO %s (%s) VALUES (%s);", this.dataSet, keys, fields);

        /*
         * after the insert, we want to retrieve the PK and UKs inserted
         * construct the SELECT query
         */

        // SELECT	-> the newly inserted PK and UKs
        StringBuilder selectStatement = new StringBuilder();
        selectStatement.append(DataAPI.keyIndex.get(dataSet).pk);
        selectStatement.append(COMA_SPACE);

        // add all the UK columns if exist
        if (DataAPI.keyIndex.get(dataSet).uk != null) {
            for (Map.Entry<String, ArrayList<String>> entry : DataAPI.keyIndex.get(dataSet).uk.entrySet()) {
                for (String uniqueKeyColumnName : entry.getValue()) {
                    selectStatement.append(uniqueKeyColumnName);
                    selectStatement.append(COMA_SPACE);
                }
            }
        }

        // remove last ", "
        selectStatement.delete(selectStatement.length() - COMA_SPACE.length(), selectStatement.length());

        // WHERE	-> this.knownFields
        String[] conditions = getEquals(knownFields, " is ");
        if (conditions == null) {
            return null;
        }
        String condition = String.join(" AND ", conditions);

        // return as second sql query "SELECT id FROM ..."
        return String.format("SELECT %s FROM %s WHERE %s;", selectStatement.toString(), this.dataSet, condition);
    }

    /**
     * Construct the select SQL query to be executed
     * @return second query to be executed
     * @throws Exception Invalid request: there must be at least a field unknown
     */
    private void constructSelectQuery() throws Exception {
        if (unknownFields.keySet().size() < 1) {
            throw new Exception("Invalid request: there are no fields specified for the retrieve operation!");
        }

        // SELECT	-> unknownFields
        String selectStatement = String.join(" , ", unknownFields.keySet().stream().toArray(String[] ::new));

        // WHERE	-> this.knownFields
        String[] conditions = getEquals(knownFields, " is ");
        if (conditions == null) {
            return;
        }
        String condition = String.join(" AND ", conditions);

        // select the SQL query
        this.query =  String.format("SELECT %s FROM %s WHERE %s;", selectStatement, this.dataSet, condition);
    }

    /**
     * Construct the update SQL query to be executed
     * @return second query to be executed
     */
    private void constructUpdateQuery() throws Exception {
        XID xid = DataAPI.dataDictionary.get(XID);
        String primaryKey = DataAPI.keyIndex.get(dataSet).pk;
        String[] values = getEquals(knownFields, "=");
        if (values == null) {
            return;
        }
        String valuesConcatenated = String.join(" , ", values);

        if (knownFields.get("last_modified_date") == null) {
            valuesConcatenated += ", last_modified_date='" + DataAPI.getTime()+SINGLE_QUOTE;
        }

        if (knownFields.get("created_date") == null) {
            valuesConcatenated += ", created_date='" + DataAPI.getTime()+SINGLE_QUOTE;
        }


        // select the SQL query
        this.query = String.format("UPDATE %s SET %s WHERE %s='%s'",
                this.dataSet, valuesConcatenated, primaryKey, xid.knownFields.get(primaryKey));
    }

    /**
     * Search if the unknown fields were assigned by a previous query
     * @param dataDictionarry hashmap to be validated
     * @return true if all unknownFields are found in the DataAPI.dataDictionary; false if one unknown field was not found
     */
    private boolean validateFieldsForInsertStatement(HashMap<String, XID> dataDictionarry) {
        for (Map.Entry<String,String> entry : unknownFields.entrySet()) {
            String XID = entry.getValue();
            String id = entry.getKey();

            // skip the pk field (it cannot exist in the database)
            String primaryKey = DataAPI.keyIndex.get(dataSet).pk;
            if (!id.equals(primaryKey)) {
                // if the value is still unknown in the global map (still null)
                if (dataDictionarry.containsKey(XID)) {
                    if (dataDictionarry.get(XID) == null || dataDictionarry.get(XID).knownFields.get(id) == null) {
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
    private String[] getEquals(TreeMap<String, String> map, String nullDelimiter) {
        String[] resultingConditions = new String[map.size()];

        if (map.size() < 1) {
            return null;
        }

        int currentIndex = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            // if value is null, use "key is null" syntax
            if (entry.getValue() == null) {
                resultingConditions[currentIndex++] = entry.getKey() + nullDelimiter + entry.getValue();
            } else {
                // else, use "key = value"
                resultingConditions[currentIndex++] = entry.getKey() + "='" + entry.getValue() + SINGLE_QUOTE;
            }
        }

        return resultingConditions;
    }

    /**
     * Execute SQL query
     *
     * IMPORTANT: Assume there is only one record returned
     *
     * @return array containing the columns returned by the sql query
     */
    private int executeQuery(Session session, String secondaryQueryInsert) {
        if (this.query == null)
            return -1;

        NativeQuery sqlQuery = session.createSQLQuery(query);
        int rowsAffected = -1;

        /* only if the crud is "create", execute the secondary query */
        if (crud == CRUD.create) {
            session.beginTransaction();
            rowsAffected = sqlQuery.executeUpdate();
            session.getTransaction().commit();

            // do the secondary query: for insert/update, it should be "SELECT id FROM TABLE WHERE (...)"
            if (secondaryQueryInsert != null) {
                sqlQuery = session.createSQLQuery(secondaryQueryInsert);
            } else {
                return rowsAffected;
            }
        }

        // get all the fields from the sql query
        sqlQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

        // !! Returned fields can be Integers, so the value in the HashMap must be a parent of all types:
        // Strings, Integer, etc. => Object must be used
        List<HashMap<String, Object>> aliasToValueMapList = sqlQuery.list();

        System.err.println("=====SQL response:=========" + aliasToValueMapList + "================");

        // append to the known fields of the XID object all the rows returned by the SQL
        TreeMap<String, String> knownFields = new TreeMap<>();
        for (HashMap<String, Object> result : aliasToValueMapList) {
            for (String key : result.keySet()) {
                knownFields.put(key, result.get(key).toString());
            }
        }

        // return the number of rows returned by the sql query
        if (rowsAffected == -1) {
            rowsAffected = aliasToValueMapList.size();
        }

        // if unknownFields has no entries, it means there is no information required:
        // ex: insert with a known id
        if (unknownFields.size() > 0) {
            // get the XID corresponding to the PK
            String XID = unknownFields.get(DataAPI.keyIndex.get(dataSet).pk);

            // if the XID is already in the MAP,
            // update known fields in the map with ones returned as a result of running the current sql query
            if (DataAPI.dataDictionary.containsKey(XID) && DataAPI.dataDictionary.get(XID) != null) {
                // knownFields is null; init treeMap
                if (DataAPI.dataDictionary.get(XID).knownFields == null) {
                    DataAPI.dataDictionary.get(XID).knownFields = new TreeMap<>();
                }
                // put new fields in dataDictionary
                DataAPI.dataDictionary.get(XID).knownFields.putAll(knownFields);
            } else {
                // XID is not present in the map; create and insert a new instance of XID object into the map
                DataAPI.dataDictionary.put(XID, new XID(XID, this.dataSet, knownFields));
            }
        }

        return rowsAffected;
    }

    /**
     * Get SQL style date time
     * @param session Sql session
     * @return string containing current system date time
     * @throws Exception Cannot get time from SQL
     */
    static String getTime(Session session) throws Exception {
        NativeQuery sqlQuery = session.createSQLQuery("SELECT NOW();");
        List<Object> requestRoutines = sqlQuery.getResultList();

        if (requestRoutines.size() != 1)
            throw new Exception("Could not get current datetime");

        return requestRoutines.get(0).toString();
    }

    static void setKeys(Session session, String dataSet) {
        // keys from dataSet were already added, nothing to do
        if (DataAPI.keyIndex.containsKey(dataSet)) {
            return;
        }

        // add an entry for the current dataSet in the keyIndex data structure
        DataAPI.keyIndex.put(dataSet, new Key());

        String getKeysQuery = "SELECT innerTable.constraint_type AS 'CONSTRAINT_TYPE', keyCol.`COLUMN_NAME` AS 'COLUMN_NAME', `keyCol`.`REFERENCED_TABLE_NAME` AS 'REFERENCED_TABLE_NAME', innerTable.constraint_name AS 'CONSTRAINT_NAME' " +
        "FROM (SELECT constr.constraint_type, constr.constraint_name, constr.table_name " +
                "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS constr " +
                "WHERE constr.table_name='"+dataSet+"') innerTable " +
        "INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE keyCol ON keyCol.table_name=innerTable.table_name AND keyCol.`CONSTRAINT_NAME`=innerTable.`CONSTRAINT_NAME` " +
        "ORDER BY constraint_type;";

        NativeQuery sqlQuery = session.createSQLQuery(getKeysQuery);

        // get all the fields from the sql query
        sqlQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        List<HashMap<String, String>> aliasToValueMapList = sqlQuery.list();

        System.err.println("=====SQL response:=========" + aliasToValueMapList + "================");

        for (HashMap<String, String> row : aliasToValueMapList) {
            String constraintType = row.get("CONSTRAINT_TYPE");
            switch (constraintType) {
                case "PRIMARY KEY":
                    DataAPI.keyIndex.put(dataSet, new Key(row.get("COLUMN_NAME")));
                    break;
                case "UNIQUE":
                    HashMap<String, ArrayList<String>> uk = DataAPI.keyIndex.get(dataSet).uk;
                    if (uk == null) {
                        DataAPI.keyIndex.get(dataSet).uk = new HashMap<>();
                        uk = DataAPI.keyIndex.get(dataSet).uk;
                    }

                    ArrayList<String> columnNames = uk.get(row.get("CONSTRAINT_NAME"));
                    if (columnNames == null) {
                        uk.put(row.get("CONSTRAINT_NAME"), new ArrayList<>());
                        columnNames = uk.get(row.get("CONSTRAINT_NAME"));
                    }

                    columnNames.add(row.get("COLUMN_NAME"));
                    break;
                default:
                    break;
            }
        }
    }

    static void getFKRelations(Session session, String dataSet, Stack<JSONObject> saveSets, HashSet<String> dataSetsHistory) {
        // if dataSet was already expanded, do nothing
        if (!dataSetsHistory.add(dataSet)) {
            return;
        }

        // TODO: When do we increment the count of current dataSet?
        int count = 1;

        // construct query for selecting constraints
        String getKeysQuery = "SELECT innerTable.constraint_type AS 'CONSTRAINT_TYPE', keyCol.`COLUMN_NAME` AS 'COLUMN_NAME', " +
                "`keyCol`.`REFERENCED_TABLE_NAME` AS 'REFERENCED_TABLE_NAME', innerTable.constraint_name AS 'CONSTRAINT_NAME', " +
                "`keyCol`.`REFERENCED_COLUMN_NAME` AS 'REFERENCED_COLUMN_NAME' " +
                "FROM (SELECT constr.constraint_type, constr.constraint_name, constr.table_name " +
                "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS constr " +
                "WHERE constr.table_name='"+dataSet+"') innerTable " +
                "INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE keyCol ON keyCol.table_name=innerTable.table_name AND keyCol.`CONSTRAINT_NAME`=innerTable.`CONSTRAINT_NAME` " +
                "ORDER BY constraint_type;";

        NativeQuery sqlQuery = session.createSQLQuery(getKeysQuery);

        // get all the fields from the sql query
        sqlQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        List<HashMap<String, String>> aliasToValueMapList = sqlQuery.list();

        // create new JSON object for current dataSet
        JSONObject saveSetItem = new JSONObject();
        JSONArray _ROW_ = new JSONArray();
        JSONObject _ROW_item_ = new JSONObject();

        // set dataSet and CRUD
        saveSetItem.put("$$_DataSet_$$", dataSet);
        _ROW_item_.put("$$_CRUD_$$", "merge");

        // set the primary key and the foreign keys
        for (HashMap<String, String> row : aliasToValueMapList) {
            String constraintType = row.get("CONSTRAINT_TYPE");
            String refTable = row.get("REFERENCED_TABLE_NAME");
            switch (constraintType) {
                case "PRIMARY KEY":
                   _ROW_item_.put(row.get("COLUMN_NAME"), String.format("$$_%s[%d]_$$", dataSet, count));
                    break;
                case "FOREIGN KEY":
                    _ROW_item_.put(row.get("COLUMN_NAME"), String.format("$$_%s[%d].%s_$$", refTable, count, row.get("REFERENCED_COLUMN_NAME")));
                    getFKRelations(session, refTable, saveSets, dataSetsHistory);
                    break;
                default:
                    break;
            }
        }
        _ROW_.add(_ROW_item_);
        saveSetItem.put("$$_ROW_$$", _ROW_);
        saveSets.push(saveSetItem);
    }
}
