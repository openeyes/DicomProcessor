package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.AttachmentData;

import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.imageio.ImageIO;
import javax.sql.rowset.serial.SerialBlob;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

public class Query {

    private String query;
    private String dataSet;
    private String XID;
    private CRUD crudOperation;
    private TreeMap<String, String> unknownFields;
    private TreeMap<String, String> knownFields;
    private TreeMap<String, ForeignKey> foreignKeys;

    // for custom sql to be run for this query
    private ArrayList<ArrayList<String>> queriesParameters;
    private ArrayList<String> queriesSQL;

    private final static String SPACE = " ";
    private final static String COMA_SPACE = ", ";
    private final static String SINGLE_QUOTE  = "'";

    // TODO: CAPITALISE to match enum documentation; the prototype also needs changing
    enum CRUD {
        create, retrieve, merge, delete
    }

    Query(String dataSet, String XID, CRUD crud, TreeMap<String, String> knownFields, TreeMap<String, String> unknownFields,
          TreeMap<String, ForeignKey> foreignKeys, ArrayList<ArrayList<String>> queriesParameters, ArrayList<String> queriesSQL) {
        this.dataSet = dataSet;
        this.XID = XID;
        this.crudOperation = crud;
        this.knownFields = knownFields;
        this.unknownFields = unknownFields;
        this.foreignKeys = foreignKeys;
        this.queriesParameters = queriesParameters;
        this.queriesSQL = queriesSQL;
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
    int constructAndRunQuery(Session session) {
        // before constructing the query, update the list of known fields
        updateKnownFieldsForeignKeys();
        if (queriesSQL != null && queriesSQL.size() != 0) {
            CRUD crudSave = this.crudOperation;
            this.crudOperation = CRUD.retrieve;
            boolean success = true;
            System.out.println("SSS2 ");
            for (int indexCustomQuery = 0; indexCustomQuery < queriesSQL.size(); indexCustomQuery++) {
                String customSql = queriesSQL.get(indexCustomQuery);
                System.out.println("SSS4: " + customSql);
                for (int indexParameters = 0; indexParameters < queriesParameters.get(indexCustomQuery).size(); indexParameters++) {
                    System.out.println(queriesParameters.get(indexCustomQuery).get(indexParameters));
                    String parameterXID = queriesParameters.get(indexCustomQuery).get(indexParameters);

                    String[] split = queriesParameters.get(indexCustomQuery).get(indexParameters).split("\\.");

                    String parameterStrippedXID = split[0] + "_$$";
                    String parameterStrippedField = split[1].substring(0,split[1].length() - 3);
                    System.out.println(parameterStrippedXID);
                    System.out.println(parameterStrippedField);
                    System.out.println(DataAPI.dataDictionary.get(parameterStrippedXID).knownFields.get(parameterStrippedField));

                    customSql = customSql.replace(parameterXID, DataAPI.dataDictionary.get(parameterStrippedXID).knownFields.get(parameterStrippedField));
                }
                System.out.println("SSS1 " + customSql);

                // execute retrieve query
                //TODO: if one query fails, should everything fail?
                // (currently yes)
                // or if ALL fail, then fail?
                this.query = customSql;
                System.out.println("trying to execute custom sql: " + this.query);

                // if one fails, stop running other custom sql queries
                if (executeQuery(session, null) != 1) {
                    System.out.println("failed to execute");
                    success = false;
                    break;
                }
            }
            this.crudOperation = crudSave;

            if (success) {
                System.out.println("Succes, no need to go further");
                return 1;
            }
        }

        String secondaryQueryInsert;
        int rowsAffected = 0;

        switch (this.crudOperation) {
            case retrieve:
                if (!constructSelectQuery()) {
                    return -1;
                }
                // execute select query
                // there is no secondary query to be executed after select
                rowsAffected = executeQuery(session, null);
                break;
            case merge:
                String primaryKey = DataAPI.keyIndex.get(dataSet).primaryKey;

                // pk is already in memory;
                // try to update fields
                if (isPrimaryKeyKnown()) {
                    System.out.println("+++++++++++FOUND++++++++UPDATE++++++++");
                    // update succeeded; return
                    if (update(session)) {
                        return 1;
                    }
                }

                // pk is not in memory;
                // construct retrieve operation (select)
                this.crudOperation = CRUD.retrieve;
                if (constructSelectQuery()) {
                    System.out.println("+++++++++++SELECT++++++++ ");
                    rowsAffected = executeQuery(session, null);
                }

                // no records in the database: insert and save the newly introduced id into the dataDictionary
                if (rowsAffected == 0) {
                    System.out.println("+++++++++++INSERT++++++++ ");

                    this.crudOperation = CRUD.create;
                    secondaryQueryInsert = constructInsertQuery();

                    // execute query and the secondary query
                    rowsAffected = executeQuery(session, secondaryQueryInsert);

                // records were in the database, but they are not in memory (dataDictionary)
                } else if (rowsAffected == 1) {
                    System.out.println("+++++++++++UPDATE++++++++ ");

                    // try to update fields
                    if (isPrimaryKeyKnown()) {
                        update(session);
                    }
                }
                break;
            default:
                break;
        }
        return rowsAffected;
    }

    public boolean update(Session session) {
        if (!constructUpdateQuery()) {
            return false;
        }
        // update requires inserting into the table, so set the CRUD operation to create
        this.crudOperation = CRUD.create;
        // when the ID is not known -> do queryByExample to retrieve it
        // when the ID is known (present in the dataDictionary) -> update the rest of the columns
        // there is no secondary query to be executed after update: all information already in dataDictionary
        int rowsAffected = executeQuery(session, null);
        if (rowsAffected != 1) {
            return false;
        }

        return true;
    }

    /**
     * check if dataDictionary has the primary key in the knownFields for the current dataSet
     * @return
     */
    private boolean isPrimaryKeyKnown() {
        String primaryKey = DataAPI.keyIndex.get(dataSet).primaryKey;
        XID xid = DataAPI.dataDictionary.get(XID);
        return xid != null && xid.knownFields != null && xid.knownFields.get(primaryKey) != null;
    }

    /**
     * add to the list of KnownFields those that have a value in dataDictionary and remove them from UnknownFields
     */
    private void updateKnownFields() {
        Iterator unknownFieldsIterator = unknownFields.entrySet().iterator();
        String primaryKey = DataAPI.keyIndex.get(dataSet).primaryKey;

        /* search through all unknown fields and if the encoding "$$_..._$$" has a value in DataAPI.dataDictionary,
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
                knownFields.put(unknownFieldId, DataAPI.getTime());
                unknownFieldsIterator.remove();
                continue;
            }

            // if the value of the XID was previously computed, remove it from the unknown fields
            // and move it to the known fields
            if (DataAPI.dataDictionary.containsKey(xidUnknown)) {
                XID xidObject = DataAPI.dataDictionary.get(xidUnknown);

                if (xidObject != null && xidObject.knownFields != null && xidObject.knownFields.containsKey(unknownFieldId)) {
                    knownFields.put(unknownFieldId, xidObject.knownFields.get(unknownFieldId));
                    unknownFieldsIterator.remove();
                }
            }
        }


        if (DataAPI.dataDictionary.containsKey(this.XID) && DataAPI.dataDictionary.get(this.XID).knownFields != null) {
            System.out.println("SSS: " + DataAPI.dataDictionary.get(this.XID));
            System.out.println("SSS: " + DataAPI.dataDictionary.get(this.XID).knownFields);
            System.out.println("SSS: " + DataAPI.dataDictionary.get(this.XID).knownFields.entrySet());
            for (Map.Entry<String, String> knownFieldDataDictionary : DataAPI.dataDictionary.get(this.XID).knownFields.entrySet()) {
                this.knownFields.put(knownFieldDataDictionary.getKey(), knownFieldDataDictionary.getValue());
                System.out.println("SS added" + knownFieldDataDictionary.getKey());
            }
        }
    }

    /**
     * add to the list of KnownFields the foreign keys that have a value in dataDictionary
     */
    private void updateKnownFieldsForeignKeys() {
        updateKnownFields();

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
     * Construct the insert SQL query; then construct a select query to determine the PK of the newly inserted row
     * IMPORTANT: the tables must have AUTO-INCREMENT set to be able to insert any rows
     *
     * @return query for selecting the id of the inserted row
     * @throws Exception Validation error
     */
    private String constructInsertQuery() {
        // if there are fields still unknown, return error
        if (!validateFieldsForInsertStatement(DataAPI.dataDictionary)) {
            return null;
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

        this.query = String.format("INSERT INTO %s (%s) VALUES (%s);", this.dataSet, keys, fields);

        // return query for getting the newly inserted ID
        return "SELECT LAST_INSERT_ID();";
    }

    /**
     * Construct the select SQL query to be executed
     * @return true if the query could be constructed; false otherwise
     * @throws Exception Invalid request: there must be at least a field unknown
     */
    private boolean constructSelectQuery() {
        if (unknownFields.keySet().size() < 1) {
            // TODO: there are no unkownFields: add all the knwonFields into DataAPI.dataDictionary
            /*
            {
              "$$_DataSet_$$": "body_site_type",
              "$$_ROW_$$": [
                {
                  "$$_CRUD_$$": "merge",
                  "body_site_snomed_type": 12413,
                  "title_short": "AB1",
                  "title_full": "ABCDEFG",
                  "title_abbreviated": "ABCD"
                }
              ]
            },
             */
            System.out.println("+_+_+_+_+_++__++_+_+_+_+_+_+_+_");
            return false;
        }

        // SELECT	-> unknownFields
        String selectStatement = String.join(" , ", unknownFields.keySet().stream().toArray(String[] ::new));

        // WHERE	-> this.knownFields
        String[] conditions = getEquals(knownFields, " is ");
        if (conditions == null) {
            return false;
        }
        String condition = String.join(" AND ", conditions);

        // select the SQL query
        this.query =  String.format("SELECT %s FROM %s WHERE %s;", selectStatement, this.dataSet, condition);
        return true;
    }

    /**
     * Construct the update SQL query to be executed
     * @return second query to be executed
     */
    private boolean constructUpdateQuery() {
        XID xid = DataAPI.dataDictionary.get(XID);
        String primaryKey = DataAPI.keyIndex.get(dataSet).primaryKey;
        String[] values = getEquals(knownFields, "=");
        if (values == null) {
            return false;
        }
        String valuesConcatenated = String.join(" , ", values);

        // select the SQL query
        this.query = String.format("UPDATE %s SET %s WHERE %s='%s'",
                this.dataSet, valuesConcatenated, primaryKey, xid.knownFields.get(primaryKey));
        return true;
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
            String primaryKey = DataAPI.keyIndex.get(dataSet).primaryKey;
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

        if (map.isEmpty()) {
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
     * Merge and insert act the same, the only difference being the merge has NO secondaryQueryInsert
     *
     * @return array containing the columns returned by the sql query
     */
    private int executeQuery(Session session, String secondaryQueryInsert) {
        if (this.query == null)
            return -1;

        NativeQuery sqlQuery = session.createSQLQuery(query);
        int rowsAffected = -1;

        /* only if the crud is "create", execute the secondary query */
        if (this.crudOperation == CRUD.create) {
            session.beginTransaction();
            rowsAffected = sqlQuery.executeUpdate();
            session.getTransaction().commit();

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

        TreeMap<String, String> knownFields = new TreeMap<>();

        if (this.crudOperation == CRUD.create) {
            String newlyInsertedId = aliasToValueMapList.get(0).get("LAST_INSERT_ID()").toString();

            // the pk is not an auto-increment column -> the pk must have been specified by hand, so there is nothing to update
            if (Integer.parseInt(newlyInsertedId) == 0) {
                return -1;
            }

            String primaryKey = DataAPI.keyIndex.get(dataSet).primaryKey;
            knownFields.put(primaryKey, newlyInsertedId);
        } else {
            // append to the known fields of the XID object all the rows returned by the SQL
            for (HashMap<String, Object> result : aliasToValueMapList) {
                for (String key : result.keySet()) {
                    knownFields.put(key, result.get(key).toString());
                }
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
            String XID = unknownFields.get(DataAPI.keyIndex.get(dataSet).primaryKey);

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
     * @return string containing current system date time; if time could not be retrieved, return null
     */
    static String getTime(Session session) {
        NativeQuery sqlQuery = session.createSQLQuery("SELECT NOW();");
        List<Object> requestRoutines = sqlQuery.getResultList();

        if (requestRoutines.size() != 1)
            return null;

        return requestRoutines.get(0).toString();
    }

    static void setKeys(Session session, String dataSet) {
        // keys from dataSet were already added, nothing to do
        if (DataAPI.keyIndex.containsKey(dataSet)) {
            return;
        }

        // add an entry for the current dataSet in the keyIndex data structure
        DataAPI.keyIndex.put(dataSet, new TableKey());

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
                    DataAPI.keyIndex.put(dataSet, new TableKey(row.get("COLUMN_NAME")));
                    break;
                case "UNIQUE":
                    HashMap<String, ArrayList<String>> uniqueKeys = DataAPI.keyIndex.get(dataSet).uniqueKeys;
                    if (uniqueKeys == null) {
                        DataAPI.keyIndex.get(dataSet).uniqueKeys = new HashMap<>();
                        uniqueKeys = DataAPI.keyIndex.get(dataSet).uniqueKeys;
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

    public static int insertAttachmentItem(Session session, int eventAttachmentGroupID, int attachment_data_id) {
        executeInsert(session, String.format("INSERT INTO event_attachment_item (attachment_data_id, event_attachment_group_id, system_only_managed) VALUES (%s,%s, 1);", attachment_data_id, eventAttachmentGroupID));

        // get the newly inserted ID
        List<HashMap<String, Object>> aliasToValueMapList = executeSelect(session, "SELECT LAST_INSERT_ID();");
        if (aliasToValueMapList.size() == 1) {
            return Integer.parseInt(aliasToValueMapList.get(0).get("LAST_INSERT_ID()").toString());
        }

        return -1;
    }

    public static List<HashMap<String, Object>> executeSelect(Session session, String selectQuery) {
        NativeQuery sqlQuery = session.createSQLQuery(selectQuery);

        sqlQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        List<HashMap<String, Object>> aliasToValueMapList = sqlQuery.list();

        System.err.println("=====SQL response:=========" + aliasToValueMapList + "================");

        return aliasToValueMapList;
    }

    public static int executeInsert(Session session, String insertQuery) {
        NativeQuery sqlQuery = session.createSQLQuery(insertQuery);

        session.beginTransaction();
        int rowsAffected = sqlQuery.executeUpdate();
        if (rowsAffected == 1) {
            session.getTransaction().commit();
        }

        return rowsAffected;
    }

    public static int insertIfNotExistsAttachmentGroup(Session session, String dataSet, int event_id, String elementTypeClassName){
        // get the id of the element_type of given class
        List<HashMap<String, Object>> aliasToValueMapList = executeSelect(session, String.format("SELECT id from element_type where class_name='%s'", elementTypeClassName));
        if (aliasToValueMapList.size() != 1) {
            System.err.println("Element_type could not be found!");
            return -1;
        }
        int elementTypeId = Integer.parseInt(aliasToValueMapList.get(0).get("id").toString());


        aliasToValueMapList = executeSelect(session,
                String.format("SELECT id from %s where event_id=%d and element_type_id='%d'", dataSet, event_id, elementTypeId));

        if (aliasToValueMapList.size() == 1) {
            // exists; return the id
            return Integer.parseInt(aliasToValueMapList.get(0).get("id").toString());
        }

        // does not exist, insert
        executeInsert(session,
                String.format("INSERT INTO %s (event_id, element_type_id) VALUES (%s,%s);", dataSet, event_id, elementTypeId));

        // get the newly inserted ID
        aliasToValueMapList = executeSelect(session, "SELECT LAST_INSERT_ID();");
        if (aliasToValueMapList.size() == 1) {
            return Integer.parseInt(aliasToValueMapList.get(0).get("LAST_INSERT_ID()").toString());
        }

        return -1;
    }

    public static boolean processAndAddThumbnails(AttachmentData attachmentData, Session session, int attachment_data_id) {
        List<HashMap<String, Object>> aliasToValueMapList = executeSelect(session,
                String.format("SELECT blob_data from attachment_data where id=%d", attachment_data_id));

        try {
            PDDocument pdfBlobDocument = PDDocument.load(new ByteArrayInputStream((byte[]) aliasToValueMapList.get(0).get("blob_data")));
            PDFRenderer pdfRenderer = new PDFRenderer(pdfBlobDocument);

            System.out.println("8: wrote image");
            Blob thumbnailSmallBlob = getThumbnail(20, pdfRenderer);
            Blob thumbnailMediumBlob = getThumbnail(50, pdfRenderer);
            Blob thumbnailLargeBlob = getThumbnail(100, pdfRenderer);

            pdfBlobDocument.close();

            // save the blob thumbnails into the attachmentData
            session.beginTransaction();
            attachmentData.setSmallThumbnail(thumbnailSmallBlob);
            attachmentData.setMediumThumbnail(thumbnailMediumBlob);
            attachmentData.setLargeThumbnail(thumbnailLargeBlob);
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Blob getThumbnail(int dpiSize, PDFRenderer pdfRenderer) throws IOException, SQLException {
        // using just the first page (index 0)
        BufferedImage bufferedImageFromPDF = pdfRenderer.renderImageWithDPI(0, dpiSize, ImageType.RGB);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImageFromPDF, "jpg", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        byte[] imageInByte = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        return new SerialBlob(imageInByte);
    }
}
