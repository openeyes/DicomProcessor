package com.abehrdigital.payloadprocessor;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;

import com.abehrdigital.payloadprocessor.exceptions.*;
import com.abehrdigital.payloadprocessor.models.AttachmentData;
import com.abehrdigital.payloadprocessor.utils.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DataAPI {

    public DataAPI(Session session) {
        this.session = session;
    }

    private enum JsonObjectClassType {
        String, JSONObject, JSONArray
    }

    private String _OE_System;
    private String _Version;
    private String time;
    private String userId;
    private Session session;
    private HashMap<String, JSONArray> originalSaveSets;

    /* dataDictionary: keeps information about mappings $$_name[X]_$$ -> value */
    HashMap<String, XID> dataDictionary;
    /* keyIndex: store information about the keys in the table: PK and UKs */
    HashMap<String, TableKey> keyIndex;

    private Savepoint savepoint;
    public Savepoint setSavepoint(final String savePoint) {
        this.session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException      {
                //System.out.println("==BefSave point==");
                savepoint = connection.setSavepoint(savePoint);
                //System.out.println("==After Save point==");
            }
        });
        return savepoint;
    }

    public void rollbackSavepoint(final Savepoint savepoint) {
        this.session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException     {
                //System.out.println("==Before rollback==");
                connection.rollback(savepoint);
                //System.out.println("==After rollback==");
            }
        });
    }

    /**
     * Pretty print HashMap with additional message
     * @param message Custom message
     * @param dataDictionary HashMap to be printed
     */
    static void printMap(String message, HashMap<String, XID> dataDictionary) {
        //System.out.println("============");
        //System.out.println(message);
        for (Map.Entry<String, XID> entry : dataDictionary.entrySet()) {
            //System.out.println(entry.getKey() + "  =>  " + entry.getValue());
        }
        //System.out.println("============");
    }

    static void printKeyMap(String message, HashMap<String, TableKey> dataDictionary) {
        //System.out.println("============");
        //System.out.println(message);
        for (Map.Entry<String, TableKey> entry : dataDictionary.entrySet()) {
            //System.out.println(entry.getKey() + "  =>  " + entry.getValue());
        }
        //System.out.println("============");
    }

    /**
     * Construct the sql query for each Query object received and run it against the database.
     *
     * @param saveSetQueries Query object which needs to be run.
     */
    private void applyQueries(ArrayList<ArrayList<Query>> saveSetQueries) throws SQLException, ValuesNotFoundException,
            EmptyKnownFieldsException, InvalidNumberOfRowsAffectedException, NoSearchedFieldsProvidedException {
        // System.out.println("number of queries: "+ saveSetQueries.size());
        // System.out.println("queries: "+ saveSetQueries);

        // for each saveset, start a new transaction:
        int transactionNumber = 0;
        for (ArrayList<Query> saveSetQuery : saveSetQueries) {
            //////////////////START Of Transaction////////////////
            try {

                savepoint = setSavepoint("savepoint_"+(transactionNumber++));
                //System.out.println("==SetSavepoint== " + savepoint.getSavepointName());

                Iterator queryIterator = saveSetQuery.iterator();

                while (queryIterator.hasNext()) {
                    Query query = (Query) queryIterator.next();

                    // TODO DEBUG
                    //System.err.println(query);
                    DataAPI.printMap("DataAPI.map: ", this.dataDictionary);
                    DataAPI.printKeyMap("KEY.map: ", this.keyIndex);

                    // construct the SQL query based on the CRUD operation and the fields found in Query object
                    query.constructAndRunQuery(session);

                    // remove query from array
                    queryIterator.remove();
                }
            } catch(InvalidNumberOfRowsAffectedException | ValuesNotFoundException | EmptyKnownFieldsException |
                    NoSearchedFieldsProvidedException exception){
                //System.out.println("Exception occured rolling back to save point");
                rollbackSavepoint(savepoint);
                throw exception;
            }
            ////END Of Transaction////////////////
        }
    }

    /**
     * Open and parse a JSON file and return a list of Query objects
     *
     * @param textData name of the json to be parsed
     * @return a list of Query objects, containing parsed information from the json
     * @throws IOException file not found
     * @throws ParseException parser exception
     */
    private ArrayList<ArrayList<Query>> parseJson(String textData) throws Exception {
        ArrayList<ArrayList<Query>> saveSets = new ArrayList<>();
        JSONParser jsonParser = new JSONParser();
        JSONObject json = (JSONObject) jsonParser.parse(textData);


        //System.out.println("==1: ");
        //System.out.println("==1: "+(String []) json.keySet().stream().toArray(String[] ::new));
        //System.out.println("==1: "+((String []) json.keySet().stream().toArray(String[] ::new)).length);
        for (String key : (String []) json.keySet().stream().toArray(String[] ::new)) {
            Object data = json.get(key);

            // depending on the current object class, parse the data
            JsonObjectClassType jsonClass = JsonObjectClassType.valueOf(data.getClass().getSimpleName());
            //System.out.println("==2: "+jsonClass + "     " + key);
            switch (jsonClass) {
                case String:
                    switch (key) {
                    case "_OE_System":
                        this._OE_System = data.toString();
                        break;
                    case "_Version":
                        this._Version = data.toString();
                        break;
                    default:
                        break;
                }
                    break;
                case JSONArray:
                    if (key.contains("XID_Map")) {
                        //System.out.println("parseMap");
                        parseXidMap((JSONArray) data);
                    } else if (key.contains("SaveSet")) {
                        //System.out.println("parseSaveSet");
                        this.originalSaveSets.put("$$_SaveSet[" + (this.originalSaveSets.size()+1) + "]_$$", (JSONArray) data);
                        saveSets.add(new ArrayList<>(parseSaveSet((JSONArray) data)));
                    }
                    break;
                default:
                    break;
            }
        }
        return saveSets;
    }

    /**
     * Parse array of JSON objects
     * @param saveSets array of JSON objects
     * @return list of Query objects containing parsed information
     */
    private ArrayList<Query> parseSaveSet(JSONArray saveSets) throws Exception {
        ArrayList<Query> saveSet = new ArrayList<>();
        // get the list of commands
        for (Object saveSetObject : saveSets) {
            JSONObject command = (JSONObject) saveSetObject;
            String dataSet = (String) command.get("$$_DataSet_$$");
            //System.out.println("33: " + dataSet);

            // get keys for this table and set them in DataAPI.keyIndex
            Query.setKeys(this.session, dataSet, this);

            // get the row
            Object rowContent = command.get("$$_ROW_$$");
            // row may be a JSON array
            if (rowContent instanceof JSONArray) {
                // parse all JSON objects in the array
                for (Object jsonContent : (JSONArray) rowContent) {
                    // add Query object resulted to the array result
                        if(!jsonContent.toString().equals("{}")) {
                            saveSet.add(parseJsonQuery((JSONObject) jsonContent, dataSet));
                        }
                }
            // or a JSON object
            } else if (rowContent instanceof JSONObject) {
                // parse JSON object and add the Query object resulted to the array result
                saveSet.add(parseJsonQuery((JSONObject) rowContent, dataSet));
            }
        }
        return saveSet;
    }

    /**
     * Parse initial information given as array of JSONObjects;
     * Save info into XID_map
     *
     * @param XidMapJSON array of JSON objects to be parsed
     */
    private void parseXidMap(JSONArray XidMapJSON) throws Exception {
        // create a map of XID string pointing to a XID object: eg. "$$_event_type[1]_$$" => XID Object
        for (Object xidDataObject : XidMapJSON) {
            JSONObject xidData = (JSONObject) xidDataObject;

            // get all information needed to create a new Query object and insert the Query object to the dataDictionary
            TreeMap<String, String> knownFields = new TreeMap<>();
            String XID = null, dataSet = null;
            for (Object keyObject : xidData.keySet()) {
                String keyStr = (String) keyObject;
                switch (keyStr) {
                    case "$$_XID_$$":
                        XID = xidData.get("$$_XID_$$").toString();
                        break;
                    case "$$_DataSet_$$":
                        dataSet = xidData.get("$$_DataSet_$$").toString();
                        break;
                    default:
                        knownFields.put(keyStr, xidData.get(keyStr).toString());
                        break;
                }
            }
            // get keys for this table and set them in this.keyIndex
            Query.setKeys(this.session, dataSet, this);

            // save the info in a new XID object
            dataDictionary.put(XID, new XID(XID, dataSet, knownFields));
        }
    }

    /**
     * Parse contents of a JSON object "$$_ROW_$$": get the CRUD and a list of known and unknown fields
     *
     * @param query JSONObject query to be parsed
     * @param dataSet table name
     * @return a new Query object with parsed information
     */
    private Query parseJsonQuery(JSONObject query, String dataSet) {
        Query.CrudOperation crudOperation = null;
        // unknownFields: {id -> XID}
        TreeMap<String, String> unknownFields = new TreeMap<>();
        // knownFields: {id -> String:value}
        TreeMap<String, String> knownFields = new TreeMap<>();
        // foreignKeys: {XID -> {field_in_XID_table, field_in_current_table}}
        TreeMap<String, ForeignKey> foreignKeys = new TreeMap<>();
        String XID = null;
        ArrayList<ArrayList<String>> queriesParameters = null;
        ArrayList<String> customSqlQueries = null;

        // for each field in the json row, save the key and the value
        //   in either knownFields or unknownFields
        for (String key : (String []) query.keySet().stream().toArray(String[] ::new)) {
            switch (key) {
                case "$$_CRUD_$$":
                    crudOperation = Query.CrudOperation.valueOf(query.get(key).toString());
                    //System.out.println("crudOp: " + crudOperation);
                    break;
                case "$$_QUERIES_$$":
                    JSONArray customQueries = (JSONArray) query.get(key);
                    queriesParameters = new ArrayList<>();
                    customSqlQueries = new ArrayList<>();

                    for (Object objectQuery : customQueries) {
                        JSONObject customQuery = (JSONObject) objectQuery;

                        JSONArray parameters = (JSONArray) customQuery.get("$$_PARAMETERS_$$");
                        ArrayList<String> queryParameters = new ArrayList<>();
                        for (Object objectParameter: parameters) {
                            queryParameters.add(objectParameter.toString());
                        }
                        queriesParameters.add(queryParameters);

                        String customSqlQuery = customQuery.get("$$_SQL_$$").toString();
                        customSqlQueries.add(customSqlQuery);
                    }
                    break;
                default:
                    String value = query.get(key).toString();
                    // unknown fields have value prefixed and suffixed with '$$'
                    if (value.startsWith("$$") && value.endsWith("$$")) {
                        // if this is a reference to a field from another table
                        if (value.contains(".")) {
                            String[] split = value.split("\\.");

                            String referencedXID = split[0] + "_$$";
                            String referencingColumn = key;
                            String referencedColumn = split[1].substring(0,split[1].length() - 3);

                            foreignKeys.put(referencedXID, new ForeignKey(referencedColumn, referencingColumn));
                        } else {
                            // save the XID encoding for the primary key field:
                            String primaryKey = this.keyIndex.get(dataSet).primaryKey;
                            if (key.equals(primaryKey)) {
                                XID = value;
                            }

                            // save the key-value pair into unknown fields
                            unknownFields.put(key, value);

                            if (!value.equals("$$_SysDateTime_$$")) {
                                // add the unknown variable to the lookup table only if it is not a date
                                // date is set at the start of the program's execution
                                dataDictionary.put(value, new XID(value, dataSet, null));
                            }
                        }
                    // known fields
                    } else {
                        // convert null value strings to null and insert them into knownFields
                        if (value.equals("null")) {
                            value = null;
                        }
                        knownFields.put(key, value);
                    }
                    break;
            }
        }

        // create and return new Query object with the information parsed
        return new Query(dataSet, XID, crudOperation, knownFields, unknownFields, foreignKeys, queriesParameters, customSqlQueries, this);
    }

    /**
     * Get the current time if it is set. If not, get the time in sql format using a select query.
     * If datetime cannot be retrieved, return default date:
     * @return current date time
     */
    String getTime() {
        if (this.session == null) {
            return "1901-01-01 00:00:00";
        }
        if (this.time == null) {
            try {
                this.time = Query.getTime(this.session);
            } catch (InvalidSqlResponse invalidSqlResponse) {
                invalidSqlResponse.printStackTrace();
                this.time = "1901-01-01 00:00:00";
            }
        }
        return this.time;
    }

    /**
     * Read contents of a JSON file and return the resulting String
     * @return String json file
     */
    String getEventTemplate() {
        try {
            JSONParser parser = new JSONParser();

            FileReader reader = new FileReader("./src/JSON.json");
            JSONObject json = (JSONObject) parser.parse(reader);

            return json.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void init(String userId, Session session) throws Exception {
        dataDictionary = new HashMap<>();
        keyIndex = new HashMap<>();
        this.userId = userId;
        this.session = session;
        this.originalSaveSets = new HashMap<>();

        // set session and time at the start of the execution
        time = getTime();
    }

    /**
     * Apply all sql queries after parsing the json string
     * @param userId String id of user who makes the changes
     * @param textData String json to be parsed
     * @throws Exception Nothing to parse; Could not open a new session!; Could not get current date time!
     */
    String magic(String userId, String textData, Session session) throws Exception, EmptyKnownFieldsException,
            ValuesNotFoundException, InvalidNumberOfRowsAffectedException, NoSearchedFieldsProvidedException {
        DataAPI.printDebugBanner(250, "Magic starts here");
        // TODO: needs renaming
        // TODO: use "user_id" for insert/merge operations
        if (textData.isEmpty()) {
            throw new Exception("Nothing to parse");
        }

        /* basic initialization */
        init(userId, session);

        // parse and execute the sql queries from the json string
        applyQueries(parseJson(textData));

        // DEBUG
        DataAPI.printMap("Final", this.dataDictionary);
        DataAPI.printKeyMap("Final", this.keyIndex);

        // construct the json
        String updatedJSON = getUpdatedJson();

        DataAPI.printDebugBanner(250, "Magic ends here");
        return updatedJSON;
    }

    /**
     * construct a new json with the updated ids found through the first run over the json file
     *
     * @return String updated json
     */
    String getUpdatedJson() {
        JSONArray xidMap = new JSONArray();
        for (Map.Entry<String, XID> entryDataDictionary : dataDictionary.entrySet()) {
            String mappingName = entryDataDictionary.getKey();
            XID xid = entryDataDictionary.getValue();
            JSONObject xidMapEntry = new JSONObject();
            xidMapEntry.put("$$_XID_$$", mappingName);
            xidMapEntry.put("$$_DataSet_$$", xid.DataSet);
            for (Map.Entry<String, String> entryKnownFields : xid.knownFields.entrySet()) {
                xidMapEntry.put(entryKnownFields.getKey(), entryKnownFields.getValue());
            }
            xidMap.add(xidMapEntry);
        }

        JSONObject updatedJson = new JSONObject();
        updatedJson.put("_OE_System", "ABEHR Jason Local v3.0");
        updatedJson.put("_Version", "v0.1");
        for (Map.Entry<String, JSONArray> entry : this.originalSaveSets.entrySet()) {
            updatedJson.put(entry.getKey(), entry.getValue());
        }
        updatedJson.put("$$_XID_Map_$$", xidMap);

        return updatedJson.toJSONString();
    }

    /**
     * Recursively search the foreign keys for a given dataSet to find all dependencies.
     * Save them in a json-manner string.
     *
     * @param dataSet the table name desired
     * @return String json string containing a template of table dependencies
     */
    public String createTemplate(String dataSet) {
        JSONObject jsonFile = new JSONObject();
        jsonFile.put("_OE_System", "ABEHR Jason Local v3.0");
        jsonFile.put("_Version", "v0.1");

        // save in a stack, the jsonObject of dataSets, created by recursively searching for foreign keys
        // starting from a given dataSet.
        Stack<JSONObject> saveSets = new Stack<>();
        Query.getFKRelations(this.session, dataSet, saveSets, new HashSet<String>());

        jsonFile.put("$$_XID_Map_$$", new JSONArray());

        // TODO: hardcoded value [1]
        jsonFile.put("$$_SaveSet[1]_$$", saveSets);

        return jsonFile.toJSONString();
    }

    /**
     * Main function
     * @param args args
     */
    public static void main(String[] args) throws EmptyKnownFieldsException, ValuesNotFoundException, Exception,
            InvalidNumberOfRowsAffectedException, NoSearchedFieldsProvidedException {
        /*// create json template with all dependencies for a given dataSet
         String jsonFromTemplate = createTemplate("event_attachment_item");
        // apply the json on the database
         DataAPI.magic("1", jsonFromTemplate);*/


        DataAPI dataAPI = new DataAPI(HibernateUtil.getSessionFactory().openSession());
        dataAPI.session.beginTransaction();
        String textData = dataAPI.getEventTemplate();
        String modifiedTextData = dataAPI.magic("1", textData, dataAPI.session);
        //System.out.println(textData);
        //System.out.println(modifiedTextData);

       /* DataAPI.session =  HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        AttachmentData attachmentData = DataAPI.session.get(AttachmentData.class, 16);
        DataAPI.linkAttachmentDataWithEvent(attachmentData, 4686438,
                "OEModule\\OphGeneric\\models\\Attachment", session);
        */

        dataAPI.session.getTransaction().commit();
    }

    void linkAttachmentDataWithEvent(AttachmentData attachmentData, int eventId,
                                     String elementTypeClassName, String eventClassName) throws InvalidNumberOfRowsAffectedException {
        // insert a new record for the event_attachment_group if there isn't already one in the table for the givven event_id
        int eventAttachmentGroupID = Query.insertIfNotExistsAttachmentGroup(this.session, eventId, elementTypeClassName, eventClassName);

        // insert a new record for the event_attachment_item table to link together the attachment_data with the attachment_group
        Query.insertIfNotExistAttachmentItem(this.session, eventAttachmentGroupID, attachmentData.getId(), null);
    }



    void linkAttachmentDataWithEventNewGroup(AttachmentData attachmentData, int eventId, String elementTypeClassName, String eventClassName) throws InvalidNumberOfRowsAffectedException {
        linkAttachmentDataWithEventNewGroup(attachmentData, eventId, elementTypeClassName, eventClassName, null);
    }

    void linkAttachmentDataWithEventNewGroup(AttachmentData attachmentData, int eventId, String elementTypeClassName, String eventClassName, String eventDocumentViewSet) throws InvalidNumberOfRowsAffectedException {
        // insert a new record for the event_attachment_group if there isn't already one in the table for the givven event_id
        int eventAttachmentGroupID = Query.insertNewAttachmentGroup(this.session, eventId, elementTypeClassName, eventClassName);

        // insert a new record for the event_attachment_item table to link together the attachment_data with the attachment_group
        Query.insertIfNotExistAttachmentItem(this.session, eventAttachmentGroupID, attachmentData.getId(), eventDocumentViewSet);
    }

    static void printDebugBanner(int length, String message) {
        String delimiter = new String(new char[length]).replace('\0', '=') + "\n";
        delimiter = delimiter + delimiter + delimiter;
        message = new String(new char[length/10]).replace('\0', '\t') + message + "\n";
//        System.err.println(delimiter + message + delimiter);

//        Logger.getLogger(RequestWorker.class.getName()).log(Level.SEVERE,
//                "REQUEST WORKER EXCEPTION WHEN EVALUATING JAVASCRIPT ->  " + getStackTraceAsString(exception));
    }
}
