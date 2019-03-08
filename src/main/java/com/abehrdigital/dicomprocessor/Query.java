package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.exceptions.*;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.xml.crypto.Data;
import java.util.*;

public class Query {

    private NativeQuery sqlQuery;
    private String dataSet;
    private String XID;
    private CrudOperation crudOperation;
    private TreeMap<String, String> unknownFields;
    private TreeMap<String, String> knownFields;
    private TreeMap<String, ForeignKey> foreignKeys;

    // for custom sql to be run for this query
    private ArrayList<ArrayList<String>> queriesParameters;
    private ArrayList<String> customSqlQueries;
    private DataAPI dataAPI;

    private final static String COMA_SPACE = ", ";
    private final static int ONE_ROW = 1;
    private final static int NO_ROWS = 0;

    enum CrudOperation {
        CREATE, RETRIEVE, MERGE, DELETE
    }

    Query(String dataSet, String XID, CrudOperation crud, TreeMap<String, String> knownFields, TreeMap<String, String> unknownFields,
          TreeMap<String, ForeignKey> foreignKeys, ArrayList<ArrayList<String>> queriesParameters, ArrayList<String> customSqlQueries,
          DataAPI dataAPI) {
        this.dataSet = dataSet;
        this.XID = XID;
        this.crudOperation = crud;
        this.knownFields = knownFields;
        this.unknownFields = unknownFields;
        this.foreignKeys = foreignKeys;
        this.queriesParameters = queriesParameters;
        this.customSqlQueries = customSqlQueries;
        this.dataAPI = dataAPI;
    }

    @Override
    public String toString() {
        return "Query [dataSet=" + dataSet + ", crud=" + crudOperation +
                ", \n\tunknownFields=" + unknownFields +
                ", \n\tknownFields=" + knownFields +
                ", \n\tforeignKeys=" + foreignKeys +
                "]\n";
    }

    // construct sql query
    void constructAndRunQuery(Session session) throws InvalidNumberOfRowsAffectedException, ValuesNotFoundException,
            EmptyKnownFieldsException, NoSearchedFieldsProvidedException {
        // before constructing the query, update the list of known fields
        updateKnownFieldsForeignKeys();

        // try running the custom queries, defined in the prototype for the current saveSet item
        try {
            runCustomQueries(session);
            // all custom queries executed correctly => no need to go further
            return;
        } catch (InvalidNumberOfRowsAffectedException | Exception e) {
            // at least one query failed => proceed with the execution
            //System.out.println("at least one query failed => proceed with the execution");
        }

        switch (this.crudOperation) {
            case RETRIEVE:
                constructSelectQuery(session);
                executeQuery(session, ONE_ROW);
                break;
            case MERGE:
                runMergeStatement(session);
                break;
            default:
                break;
        }
    }

    private void runMergeStatement(Session session) throws ValuesNotFoundException, EmptyKnownFieldsException,
            InvalidNumberOfRowsAffectedException, NoSearchedFieldsProvidedException {
        int rowsAffected = 0;

        // pk is already in memory;
        // try to update fields
        if (isPrimaryKeyKnown()) {
            //System.out.println("+++++++++++FOUND++++++++UPDATE++++++++");
            try {
                update(session);
                // update succeeded; return
                return;
            } catch (InvalidNumberOfRowsAffectedException e) {
                // at least one query failed => proceed with the execution
                //System.out.println("at least one query failed => proceed with the execution");
            }
        }

        // pk is not in memory;
        // construct retrieve operation (select)
        this.crudOperation = CrudOperation.RETRIEVE;
        //System.out.println("SS+1: creating select");
        constructSelectQuery(session);
        //System.out.println("SS+2: done");
        rowsAffected = executeQuery(session, NO_ROWS);
        //System.out.println("SS+6: creating select " + rowsAffected);

        // no records in the database: insert and save the newly introduced id into the dataDictionary
        if (rowsAffected == 0) {
            //System.out.println("+++++++++++INSERT++++++++ ");

            this.crudOperation = CrudOperation.CREATE;
            constructInsertQuery(session);

            // execute query and the secondary query
            executeQuery(session, ONE_ROW);

            // records were in the database, but they are not in memory (dataDictionary)
        } else if (rowsAffected == 1) {
            //System.out.println("+++++++++++UPDATE++++++++ ");

            this.crudOperation = CrudOperation.MERGE;

            // try to update fields
            if (isPrimaryKeyKnown()) {
                update(session);
            }
        }
    }

    /**
     * Run custom queries defined in the prototype for the current saveSet
     * @param session current session
     * @return true if all queries finished successfully, false if one failed
     */
    private void runCustomQueries(Session session) throws InvalidNumberOfRowsAffectedException, Exception {
        if (customSqlQueries != null && !customSqlQueries.isEmpty()) {
            CrudOperation crudSave = this.crudOperation;
            this.crudOperation = CrudOperation.RETRIEVE;

            for (int cusomSqlQueryIndex = 0; cusomSqlQueryIndex < customSqlQueries.size(); cusomSqlQueryIndex++) {
                HashMap<String, String> xidParameterList = new HashMap<>();

                // execute retrieve query
                //TODO: if one query fails, should everything fail? (currently yes) or if ALL fail, then fail?
                this.sqlQuery = session.createSQLQuery(
                        resolveCustomQueryParameters(
                                customSqlQueries.get(cusomSqlQueryIndex),
                                queriesParameters.get(cusomSqlQueryIndex),
                                xidParameterList));
                for (Map.Entry<String, String> entryParameter : xidParameterList.entrySet()) {
                    //System.out.println("Rep324lacing: **" + entryParameter.getKey() + "**  with " + entryParameter.getValue());
                    this.sqlQuery.setParameter(entryParameter.getKey(), entryParameter.getValue());
                    //System.out.println("AFTER");
                }
                //System.out.println("trying to execute custom sql: " + this.sqlQuery);

                // if one fails, stop running other custom sql queries
                // exception will be thrown
                executeQuery(session, ONE_ROW);
            }
            this.crudOperation = crudSave;
            return;
        }
        //System.out.println("++There are no custom queries");
        throw new Exception("No queries to execute.");
    }

    /**
     * Replace parameters of custom query with corresponding values from DataDictionary.
     * @param customSql String custom sql query to be parsed and
     * @param queryParameters list of parameters to be replaced with corresponding values
     * @param xidParameterList map to store pairs of (":param" => valueOfParam), which is used to set parameters to NativeQuery
     * @return String updated custom sql query
     */
    private String resolveCustomQueryParameters(String customSql, ArrayList<String> queryParameters, HashMap<String, String> xidParameterList) {
        //System.out.println("SSS4: " + customSql);
        for (int indexParameters = 0; indexParameters < queryParameters.size(); indexParameters++) {
            String parameterXID = queryParameters.get(indexParameters);
            //System.out.println("SSS34: " + parameterXID);
            String[] parameterXidSplit = parameterXID.split("\\.");

            String parameterStrippedXID = parameterXidSplit[0] + "_$$";
            String parameterStrippedField = parameterXidSplit[1].substring(0,parameterXidSplit[1].length() - 3);
            //System.out.println("parameterStrippedXID: " + parameterStrippedXID);
            //System.out.println("parameterStrippedField: " + parameterStrippedField);

            customSql = customSql.replace(parameterXID, " :parameter_" + indexParameters);
            xidParameterList.put("parameter_" + indexParameters, dataAPI.dataDictionary.get(parameterStrippedXID).knownFields.get(parameterStrippedField));
        }
        //System.out.println("SSS1 " + customSql);
        return customSql;
    }

    public void update(Session session) throws InvalidNumberOfRowsAffectedException {
        constructUpdateQuery(session);
        // when the ID is not known -> do queryByExample to retrieve it
        // when the ID is known (present in the dataDictionary) -> update the rest of the columns
        executeQuery(session, ONE_ROW);
    }

    /**
     * check if dataDictionary has the primary key in the knownFields for the current dataSet
     * @return true if the primary key is known, false otherwise
     */
    private boolean isPrimaryKeyKnown() {
        String primaryKey = dataAPI.keyIndex.get(dataSet).primaryKey;
        XID xid = dataAPI.dataDictionary.get(XID);
        return xid != null && xid.knownFields != null && xid.knownFields.get(primaryKey) != null;
    }

    /**
     * add to the list of KnownFields those that have a value in dataDictionary and remove them from UnknownFields
     */
    private void updateKnownFields() {
        Iterator unknownFieldsIterator = unknownFields.entrySet().iterator();
        String primaryKey = dataAPI.keyIndex.get(dataSet).primaryKey;

        /* search through all unknown fields and if the encoding "$$_..._$$" has a value in dataAPI.dataDictionary,
         * add it to the known fields and remove it from the unknownFields
         */
        while (unknownFieldsIterator.hasNext()) {
            String unknownFieldId = ((Map.Entry<String, String>) unknownFieldsIterator.next()).getKey();
            String xidUnknown = unknownFields.get(unknownFieldId);

            // do not remove PK from the unknown fields
            if (primaryKey.equals(unknownFieldId)) {
                continue;
            }

            // replace "$$_SysDateTime_$$" with the time set at the start of the program execution
            if (xidUnknown.equals("$$_SysDateTime_$$")) {
                //System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@" + unknownFieldId);
                knownFields.put(unknownFieldId, dataAPI.getTime());
                unknownFieldsIterator.remove();
                continue;
            }

            // if the value of the XID was previously computed, remove it from the unknown fields
            // and move it to the known fields
            if (dataAPI.dataDictionary.containsKey(xidUnknown)) {
                XID xidObject = dataAPI.dataDictionary.get(xidUnknown);

                if (xidObject != null && xidObject.knownFields != null && xidObject.knownFields.containsKey(unknownFieldId)) {
                    knownFields.put(unknownFieldId, xidObject.knownFields.get(unknownFieldId));
                    unknownFieldsIterator.remove();
                }
            }
        }


        if (dataAPI.dataDictionary.containsKey(this.XID) && dataAPI.dataDictionary.get(this.XID).knownFields != null) {
            //System.out.println("SSS: " + dataAPI.dataDictionary.get(this.XID));
            //System.out.println("SSS: " + dataAPI.dataDictionary.get(this.XID).knownFields);
            //System.out.println("SSS: " + dataAPI.dataDictionary.get(this.XID).knownFields.entrySet());
            for (Map.Entry<String, String> knownFieldDataDictionary : dataAPI.dataDictionary.get(this.XID).knownFields.entrySet()) {
                this.knownFields.put(knownFieldDataDictionary.getKey(), knownFieldDataDictionary.getValue());
                //System.out.println("SS added" + knownFieldDataDictionary.getKey());
            }
        }
    }

    /**
     * add to the list of KnownFields the foreign keys that have a value in dataDictionary
     */
    private void updateKnownFieldsForeignKeys() {
        updateKnownFields();

        /* Add to the known fields the foreign keys with a value received previously and saved in dataAPI.dataDictionary */
        for (Map.Entry<String, ForeignKey> foreignKeyEntry : foreignKeys.entrySet()) {
            String XIDForeignKey = foreignKeyEntry.getKey();
            ForeignKey foreignKey = foreignKeyEntry.getValue();

            if (dataAPI.dataDictionary.containsKey(XIDForeignKey)) {
                XID xidObject = dataAPI.dataDictionary.get(XIDForeignKey);
                if (xidObject != null && xidObject.knownFields != null && xidObject.knownFields.containsKey(foreignKey.referencedColumn)) {
                    String previouslyFoundValue = xidObject.knownFields.get(foreignKey.referencedColumn);
                    // save the value of foreign key in known fields
                    knownFields.put(foreignKey.referencingColumn, previouslyFoundValue);
                }
            }
        }
    }

    /**
     * Construct the insert SQL query; then construct a select query to determine the PK of the newly inserted row
     * IMPORTANT: the tables must have AUTO-INCREMENT set to be able to insert any rows
     *
     * @return query for selecting the id of the inserted row
     * @throws Exception Validation error
     */
    private void constructInsertQuery(Session session) throws ValuesNotFoundException, EmptyKnownFieldsException {
        // if there are fields still unknown, return error
        validateFieldsForInsertStatement(dataAPI.dataDictionary);

        if (knownFields.isEmpty()) {
            throw new EmptyKnownFieldsException("Nothing to insert.");
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
                fields.append(" :");
                fields.append(knownField);
                fields.append(COMA_SPACE);
            }
        }
        // remove last ", "
        keys.delete(keys.length() - COMA_SPACE.length(), keys.length());
        fields.delete(fields.length() - COMA_SPACE.length(), fields.length());

        this.sqlQuery = session.createSQLQuery(String.format("INSERT INTO %s (%s) VALUES (%s);", this.dataSet, keys, fields));

        // add parameters from knownFields to the NativeQuery
        updateSqlQueryWithParameters();
    }

    /**
     * Construct the select SQL query to be executed
     * @return true if the query could be constructed; false otherwise
     * @throws Exception Invalid request: there must be at least a field unknown
     */
    private void constructSelectQuery(Session session) throws NullPointerException, NoSearchedFieldsProvidedException {
        if (unknownFields.keySet().size() < 1) {
            throw new NoSearchedFieldsProvidedException("There must be at least one unknown field to be retrieved.");
        }

        // SELECT	-> unknownFields
        String selectStatement = String.join(" , ", unknownFields.keySet().stream().toArray(String[]::new));

        // WHERE	-> this.knownFields
        ArrayList<String> conditions = getEquals(knownFields, " is ");
        String condition = String.join(" AND ", conditions);

        // select the SQL query
        this.sqlQuery = session.createSQLQuery(String.format("SELECT %s FROM %s WHERE %s ;", selectStatement, dataSet, condition));

        // add parameters from knownFields to the NativeQuery
        updateSqlQueryWithParameters();
    }

    /**
     * Construct the update SQL query to be executed
     */
    private void constructUpdateQuery(Session session) {
        XID xid = dataAPI.dataDictionary.get(XID);
        String primaryKey = dataAPI.keyIndex.get(dataSet).primaryKey;
        ArrayList<String> values = getEquals(knownFields, " = ");
        String valuesConcatenated = String.join(" , ", values);

        // select the SQL query
        this.sqlQuery = session.createSQLQuery(String.format("UPDATE %s SET %s WHERE %s = :primaryKeyValue",
                this.dataSet, valuesConcatenated, primaryKey))
        .setParameter("primaryKeyValue", xid.knownFields.get(primaryKey));

        //System.out.println("PK: " + primaryKey + "     VAL: " + xid.knownFields.get(primaryKey));

        // add parameters from knownFields to the NativeQuery
        updateSqlQueryWithParameters();
    }

    /**
     * add known fields as parameters to the sqlQuery
     */
    private void updateSqlQueryWithParameters() {
        for (Map.Entry<String, String> entryKnownField : knownFields.entrySet()) {
            // if a value exists for the field and it's not the primary key
            if (entryKnownField.getValue() != null && !entryKnownField.getKey().equals(dataAPI.keyIndex.get(dataSet).primaryKey)) {
                //System.out.println("Replacing: " + entryKnownField.getKey() + "  with " + entryKnownField.getValue());
                this.sqlQuery.setParameter(entryKnownField.getKey(), entryKnownField.getValue());
            }
        }
    }

    /**
     * Search if the unknown fields were assigned by a previous query
     * @param dataDictionarry hashmap to be validated
     */
    private void validateFieldsForInsertStatement(HashMap<String, XID> dataDictionarry) throws ValuesNotFoundException {
        for (Map.Entry<String, String> entry : unknownFields.entrySet()) {
            String XID = entry.getValue();
            String id = entry.getKey();

            // skip the pk field (it cannot exist in the database)
            String primaryKey = dataAPI.keyIndex.get(dataSet).primaryKey;
            if (!id.equals(primaryKey)) {
                // if the value is still unknown in the global map (still null)
                if (dataDictionarry.containsKey(XID)) {
                    if (dataDictionarry.get(XID) == null || dataDictionarry.get(XID).knownFields.get(id) == null) {
                        throw new ValuesNotFoundException();
                    }
                }
            }
        }
    }

    /**
     * Get a list of conditions from the given map fields;
     * use "=" as delimiter for normal values
     * use "is" for NULLs
     *
     * @return array of Objects (strings)
     */
    private ArrayList<String> getEquals(TreeMap<String, String> map, String nullDelimiter) throws NullPointerException {
        ArrayList<String> resultingConditions = new ArrayList<>();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            // don't add the primary key
            //System.out.println("key: " + entry.getKey());
            if (!entry.getKey().equals(dataAPI.keyIndex.get(dataSet).primaryKey)) {
                //System.out.println("\treplacing " + entry.getKey() + "    " + entry.getValue());
                // if value is null, use "key is null" syntax
                if (entry.getValue() == null) {
                    resultingConditions.add(entry.getKey() + nullDelimiter + "null");
                } else {
                    // else, use "key = value"
                    resultingConditions.add(entry.getKey() + " = :" + entry.getKey());
                }
            }
        }

        return resultingConditions;
    }

    /**
     * Execute SQL query
     * <p>
     * IMPORTANT: Assume there is only one record returned
     * <p>
     * Merge and insert act the same, the only difference being the merge has NO secondaryQueryInsert
     *
     * @return array containing the columns returned by the sql query
     */
    private int executeQuery(Session session, int minRowsExpected) throws NullPointerException, InvalidNumberOfRowsAffectedException {
        TreeMap<String, String> knownFields = new TreeMap<>();

        // TODO: NOT SURE MERGE IS NEEDED HERE
        if (this.crudOperation == CrudOperation.CREATE || this.crudOperation == CrudOperation.MERGE) {
            //System.out.println("=1= Execute insert");
            executeInsertUpdate(this.sqlQuery);
        }

        if (this.crudOperation == CrudOperation.CREATE) {
            int newlyInsertedId = getNewlyInsertedId(session);
            String primaryKey = dataAPI.keyIndex.get(dataSet).primaryKey;

            knownFields.put(primaryKey, String.valueOf(newlyInsertedId));
        } else if (this.crudOperation == CrudOperation.RETRIEVE) {
            // TODO: remove aliasToValueMapList
            List<HashMap<String, Object>> aliasToValueMapList = executeSelect(this.sqlQuery, minRowsExpected);

            // append to the known fields of the XID object all the rows returned by the SQL
            for (HashMap<String, Object> result : aliasToValueMapList) {
                for (String key : result.keySet()) {
                    knownFields.put(key, result.get(key).toString());
                }
            }
        }

        // if unknownFields has no entries, it means there is no information required
        if (!unknownFields.isEmpty()) {
            //System.out.println("SS+4: UPDATING");
            // get the XID corresponding to the PK
            String XID = unknownFields.get(dataAPI.keyIndex.get(dataSet).primaryKey);

            // if the XID is already in the MAP,
            // update known fields in the map with ones returned as a result of running the current sql query
            if (dataAPI.dataDictionary.containsKey(XID) && dataAPI.dataDictionary.get(XID) != null) {
                // knownFields is null; init treeMap
                if (dataAPI.dataDictionary.get(XID).knownFields == null) {
                    dataAPI.dataDictionary.get(XID).knownFields = new TreeMap<>();
                }
                // put new fields in dataDictionary
                dataAPI.dataDictionary.get(XID).knownFields.putAll(knownFields);
            } else {
                // XID is not present in the map; create and insert a new instance of XID object into the map
                dataAPI.dataDictionary.put(XID, new XID(XID, this.dataSet, knownFields));
            }
        }
        //System.out.println("SS+4: done updating " + knownFields.size());

        return knownFields.size();
    }

    /**
     * Get SQL style date time
     *
     * @param session Sql session
     * @return string containing current system date time; if time could not be retrieved, return null
     * @throws InvalidSqlResponse error when retrieving time from database
     */
    static String getTime(Session session) throws InvalidSqlResponse {
        NativeQuery sqlQuery = session.createSQLQuery("SELECT NOW();");
        List<Object> requestRoutines = sqlQuery.getResultList();

        if (requestRoutines.size() != 1) {
            throw new InvalidSqlResponse("Could not get the time from the current session.");
        }

        return requestRoutines.get(0).toString();
    }

    static void setKeys(Session session, String dataSet, DataAPI dataAPI) {
        // keys from dataSet were already added, nothing to do
        if (dataAPI.keyIndex.containsKey(dataSet)) {
            return;
        }

        // add an entry for the current dataSet in the keyIndex data structure
        dataAPI.keyIndex.put(dataSet, new TableKey());

        String getKeysQuery =
            "SELECT innerTable.constraint_type AS 'CONSTRAINT_TYPE', keyCol.`COLUMN_NAME` AS 'COLUMN_NAME', " +
                "`keyCol`.`REFERENCED_TABLE_NAME` AS 'REFERENCED_TABLE_NAME', innerTable.constraint_name AS 'CONSTRAINT_NAME' " +
            "FROM (SELECT constr.constraint_type, constr.constraint_name, constr.table_name " +
                "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS constr " +
                "WHERE constr.table_name = :dataSet) innerTable " +
            "INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE keyCol ON keyCol.table_name=innerTable.table_name AND " +
                "keyCol.`CONSTRAINT_NAME`=innerTable.`CONSTRAINT_NAME` " +
            "ORDER BY constraint_type;";

        NativeQuery sqlQuery = session.createSQLQuery(getKeysQuery)
            .setParameter("dataSet", dataSet);

        // get all the fields from the sql query
        sqlQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        List<HashMap<String, String>> aliasToValueMapList = sqlQuery.list();

        // TODO: DEBUG
        Query.prettyPrintQueryResultString(aliasToValueMapList);

        for (HashMap<String, String> row : aliasToValueMapList) {
            String constraintType = row.get("CONSTRAINT_TYPE");
            switch (constraintType) {
                case "PRIMARY KEY":
                    dataAPI.keyIndex.put(dataSet, new TableKey(row.get("COLUMN_NAME")));
                    break;
                case "UNIQUE":
                    HashMap<String, ArrayList<String>> uniqueKeys = dataAPI.keyIndex.get(dataSet).uniqueKeys;
                    if (uniqueKeys == null) {
                        dataAPI.keyIndex.get(dataSet).uniqueKeys = new HashMap<>();
                        uniqueKeys = dataAPI.keyIndex.get(dataSet).uniqueKeys;
                    }

                    ArrayList<String> columnNames = uniqueKeys.get(row.get("CONSTRAINT_NAME"));
                    if (columnNames == null) {
                        uniqueKeys.put(row.get("CONSTRAINT_NAME"), new ArrayList<>());
                        columnNames = uniqueKeys.get(row.get("CONSTRAINT_NAME"));
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
            "WHERE constr.table_name= :dataSet) innerTable " +
            "INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE keyCol ON keyCol.table_name=innerTable.table_name AND keyCol.`CONSTRAINT_NAME`=innerTable.`CONSTRAINT_NAME` " +
            "ORDER BY constraint_type;";

        NativeQuery sqlQuery = session.createSQLQuery(getKeysQuery)
            .setParameter("dataSet", dataSet);

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

    private static int getNewlyInsertedId(Session session) throws InvalidNumberOfRowsAffectedException{
        List<HashMap<String, Object>> aliasToValueMapList = executeSelect(
                session.createSQLQuery("SELECT LAST_INSERT_ID();"),
                ONE_ROW);

        if (aliasToValueMapList.size() != 1) {
            throw new InvalidNumberOfRowsAffectedException("SELECT LAST_INSERT_ID();");
        }

        return Integer.parseInt(aliasToValueMapList.get(0).get("LAST_INSERT_ID()").toString());
    }

    static void insertAttachmentItem(Session session, int eventAttachmentGroupID, int attachment_data_id)
            throws InvalidNumberOfRowsAffectedException {
        executeInsertUpdate(
            session.createSQLQuery("INSERT INTO event_attachment_item (attachment_data_id, event_attachment_group_id," +
                " system_only_managed) VALUES ( :attachment_data_id , :eventAttachmentGroupID, 1);")
                .setParameter("attachment_data_id", attachment_data_id)
                .setParameter("eventAttachmentGroupID", eventAttachmentGroupID));
    }

    private static List<HashMap<String, Object>> executeSelect(NativeQuery sqlQuery, int minRowsExpected)
            throws InvalidNumberOfRowsAffectedException {
        System.err.println("=======SELECT======== ");
        // get all the fields from the sql query
        sqlQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

        // Returned fields can be Integers, so the value in the HashMap must be a parent of all types:
        // Strings, Integer, etc. => Object must be used
        List<HashMap<String, Object>> aliasToValueMapList = sqlQuery.list();

        // TODO: DEBUG
        Query.prettyPrintQueryResultObject(aliasToValueMapList);

        // throw an exception of the number of rows affected does not correspond to the one expected
        if (aliasToValueMapList.size() < minRowsExpected || aliasToValueMapList.size() > 1) {
            throw new InvalidNumberOfRowsAffectedException(aliasToValueMapList.size() +
                    " rows returned! Expected no less than " + minRowsExpected + " rows and no more than one.");
        }
        return aliasToValueMapList;
    }

    private static void executeInsertUpdate(NativeQuery sqlQuery) throws InvalidNumberOfRowsAffectedException {
        System.err.println("=======INSERT======== ");
        int numberRowsAffected = sqlQuery.executeUpdate();
        //System.out.println("=2= " + numberRowsAffected);

        if (numberRowsAffected != ONE_ROW) {
            //System.out.println("=3= error");
            throw new InvalidNumberOfRowsAffectedException();
        }
    }

    static int insertIfNotExistsAttachmentGroup(Session session, int event_id, String elementTypeClassName)
            throws InvalidNumberOfRowsAffectedException {
        NativeQuery sqlQuery = session.createSQLQuery("SELECT id from element_type where class_name = :elementTypeClassName")
                .setParameter("elementTypeClassName", elementTypeClassName);

        // get the id of the element_type of given class
        List<HashMap<String, Object>> aliasToValueMapList = executeSelect(sqlQuery, ONE_ROW);
        if (aliasToValueMapList.size() != 1) {
            System.err.println("Element_type could not be found!");
            return -1;
        }
        int elementTypeId = Integer.parseInt(aliasToValueMapList.get(0).get("id").toString());

        aliasToValueMapList = executeSelect(
            session.createSQLQuery("SELECT id from event_attachment_group where event_id = :event_id and element_type_id = :elementTypeId")
                .setParameter("event_id", event_id)
                .setParameter("elementTypeId", elementTypeId),
            NO_ROWS);

        if (aliasToValueMapList.size() == 1) {
            // exists; return the id
            return Integer.parseInt(aliasToValueMapList.get(0).get("id").toString());
        }

        // does not exist, insert
        executeInsertUpdate(
            session.createSQLQuery("INSERT INTO event_attachment_group (event_id, element_type_id) VALUES ( :event_id , :elementTypeId );")
            .setParameter("event_id", event_id)
            .setParameter("elementTypeId", elementTypeId));

        // get the newly inserted ID
        return getNewlyInsertedId(session);
    }

    private static void prettyPrintQueryResultObject(List<HashMap<String, Object>> aliasToValueMapList) {
        System.err.println("========================SQL response:========================\n\t\t\t"
                + aliasToValueMapList +
                "\n==============================================================");
    }

    private static void prettyPrintQueryResultString(List<HashMap<String, String>> aliasToValueMapList) {
        System.err.println("========================SQL response:========================\n"
                + aliasToValueMapList +
                "\n==============================================================");
    }
}
