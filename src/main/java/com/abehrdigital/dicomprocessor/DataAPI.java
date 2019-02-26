package com.abehrdigital.dicomprocessor;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.abehrdigital.dicomprocessor.models.AttachmentData;
import com.abehrdigital.dicomprocessor.utils.HibernateUtil;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DataAPI {

    private enum JsonObjectClassType {
        String, JSONObject, JSONArray
    }

    /* dataDictionary: keeps information about mappings $$_name[X]_$$ -> value */
    static HashMap<String, XID> dataDictionary;
    /* keyIndex: store information about the keys in the table: PK and UKs */
    static HashMap<String, TableKey> keyIndex;
    private static String _OE_System;
    private static String _Version;
    private static Session session;
    private static String time;
    private static String userId;

    /**
     * Pretty print HashMap with additional message
     * @param message Custom message
     * @param dataDictionary HashMap to be printed
     */
    static void printMap(String message, HashMap<String, XID> dataDictionary) {
        System.out.println("============");
        System.out.println(message);
        for (Map.Entry<String, XID> entry : dataDictionary.entrySet()) {
            System.out.println(entry.getKey() + "  =>  " + entry.getValue());
        }
        System.out.println("============");
    }

    static void printKeyMap(String message, HashMap<String, TableKey> dataDictionary) {
        System.out.println("============");
        System.out.println(message);
        for (Map.Entry<String, TableKey> entry : dataDictionary.entrySet()) {
            System.out.println(entry.getKey() + "  =>  " + entry.getValue());
        }
        System.out.println("============");
    }

    /**
     * Construct the sql query for each Query object received and run it against the database.
     *
     * @param queries Query object which needs to be run.
     */
    private static void applyQueries(ArrayList<Query> queries) {
        try {
            Iterator queryIterator = queries.iterator();

            while (queryIterator.hasNext()) {
                Query query = (Query) queryIterator.next();

                // DEBUG
                System.out.println(query);
                DataAPI.printMap("DataAPI.map: ", DataAPI.dataDictionary);

                // construct the SQL query based on the CRUD operation and the fields found in Query object
                query.constructAndRunQuery(DataAPI.getSession());

                // remove query from array
                queryIterator.remove();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Open and parse a JSON file and return a list of Query objects
     *
     * @param jsonData name of the json to be parsed
     * @return a list of Query objects, containing parsed information from the json
     * @throws IOException file not found
     * @throws ParseException parser exception
     */
    private static ArrayList<Query> parseJson(String jsonData) throws Exception {
        ArrayList<Query> saveSets = new ArrayList<>();
        JSONParser jsonParser = new JSONParser();
        JSONObject json = (JSONObject) jsonParser.parse(jsonData);


        for (String key : (String []) json.keySet().stream().toArray(String[] ::new)) {
            Object data = json.get(key);

            // depending on the current object class, parse the data
            JsonObjectClassType jsonClass = JsonObjectClassType.valueOf(data.getClass().getSimpleName());
            switch (jsonClass) {
                case String:
                    switch (key.toString()) {
                    case "_OE_System":
                        DataAPI._OE_System = data.toString();
                        break;
                    case "_Version":
                        DataAPI._Version = data.toString();
                        break;
                    default:
                        break;
                }
                    break;
                case JSONArray:
                    if (key.toString().contains("XID_Map")) {
                        parseXidMap((JSONArray) data);
                    } else if (key.toString().contains("SaveSet")) {
                        saveSets.addAll(parseSaveSet((JSONArray) data));
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
    private static ArrayList<Query> parseSaveSet(JSONArray saveSets) throws Exception {
        ArrayList<Query> ret = new ArrayList<>();
        // get the list of commands
        for (Object saveSet : saveSets) {
            JSONObject command = (JSONObject) saveSet;
            String dataSet = (String) command.get("$$_DataSet_$$");

            // get keys for this table and set them in DataAPI.keyIndex
            Query.setKeys(getSession(), dataSet);

            // get the row
            Object rowContent = command.get("$$_ROW_$$");
            // row may be a JSON array
            if (rowContent instanceof JSONArray) {
                // parse all JSON objects in the array
                for (Object jsonContent : (JSONArray) rowContent) {
                    // add Query object resulted to the array result
                    ret.add(parseJsonQuery((JSONObject) jsonContent, dataSet));
                }
            // or a JSON object
            } else if (rowContent instanceof JSONObject) {
                // parse JSON object and add the Query object resulted to the array result
                ret.add(parseJsonQuery((JSONObject) rowContent, dataSet));
            }
        }
        return ret;
    }

    /**
     * Parse initial information given as array of JSONObjects;
     * Save info into XID_map
     *
     * @param XidMapJSON array of JSON objects to be parsed
     */
    private static void parseXidMap(JSONArray XidMapJSON) throws Exception {
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
            // get keys for this table and set them in DataAPI.keyIndex
            Query.setKeys(getSession(), dataSet);

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
    private static Query parseJsonQuery(JSONObject query, String dataSet) {
        Query.CRUD crudOperation = null;
        // unknownFields: {id -> XID}
        TreeMap<String, String> unknownFields = new TreeMap<>();
        // knownFields: {id -> String:value}
        TreeMap<String, String> knownFields = new TreeMap<>();
        // foreignKeys: {XID -> {field_in_XID_table, field_in_current_table}}
        TreeMap<String, ForeignKey> foreignKeys = new TreeMap<>();
        String XID = null;
        ArrayList<ArrayList<String>> queriesParameters = null;
        ArrayList<String> queriesSQL = null;

        // for each field in the json row, save the key and the value
        //   in either knownFields or unknownFields
        for (String key : (String []) query.keySet().stream().toArray(String[] ::new)) {
            switch (key) {
                case "$$_CRUD_$$":
                    crudOperation = Query.CRUD.valueOf(query.get(key).toString());
                    break;
                case "$$_QUERIES_$$":
                    JSONArray customQueries = (JSONArray) query.get(key);
                    queriesParameters = new ArrayList<>();
                    queriesSQL = new ArrayList<>();

                    for (Object objectQuery : customQueries) {
                        JSONObject customQuery = (JSONObject) objectQuery;

                        JSONArray parameters = (JSONArray) customQuery.get("$$_PARAMETERS_$$");
                        ArrayList<String> queryParameters = new ArrayList<>();
                        for (Object objectParameter: parameters) {
                            queryParameters.add(objectParameter.toString());
                        }
                        queriesParameters.add(queryParameters);

                        String customSqlQuery = customQuery.get("$$_SQL_$$").toString();
                        queriesSQL.add(customSqlQuery);
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
                            String primaryKey = DataAPI.keyIndex.get(dataSet).primaryKey;
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
                            knownFields.put(key, null);
                        // insert value strings into knownFields
                        } else {
                            knownFields.put(key, value);
                        }
                    }
                    break;
            }
        }

        // create and return new Query object with the information parsed
        return new Query(dataSet, XID, crudOperation, knownFields, unknownFields, foreignKeys, queriesParameters, queriesSQL);
    }

    /**
     * Get the current session if it is set. If not, create one.
     * @return current session
     * @throws Exception Cannot open a new session.
     */
    static Session getSession() throws Exception {
        // if a session does not exists, open a new session
        if (DataAPI.session == null) {
            DataAPI.session = HibernateUtil.getSessionFactory().openSession();
        }
        if (DataAPI.session == null) {
            throw new Exception("Could not open a new session!");
        }
        return DataAPI.session;
    }

    /**
     * Get the current time if it is set. If not, get the time in sql format using a select query.
     * If datetime cannot be retrieved, return default date:
     * @return current date time
     */
    static String getTime() {
        if (DataAPI.session == null) {
            return "1901-01-01 00:00:00";
        }
        if (DataAPI.time == null) {
            DataAPI.time = Query.getTime(DataAPI.session);
        }
        if (DataAPI.time == null) {
            System.err.println("Could not get the time from the current session. Setting default to 1901-01-01.");
            DataAPI.time = "1901-01-01 00:00:00";
        }
        return DataAPI.time;
    }

    /**
     * Read contents of a JSON file and return the resulting String
     * @return String json file
     */
    static String getEventTemplate() {
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

    private static void init(String userId) throws Exception {
        dataDictionary = new HashMap<>();
        keyIndex = new HashMap<>();
        DataAPI.userId = userId;

        // set session and time at the start of the execution
        session = getSession();
        time = getTime();
    }

    /**
     * Apply all sql queries after parsing the json string
     * @param userId String id of user who makes the changes
     * @param jsonData String json to be parsed
     * @throws Exception Nothing to parse; Could not open a new session!; Could not get current date time!
     */
    static String magic(String userId, String jsonData) throws Exception {
        // TODO: needs renaming
        // TODO: use "user_id" for insert/merge operations
        if (jsonData.isEmpty()) {
            throw new Exception("Nothing to parse");
        }
        try {
            /* basic initialization */
            init(userId);

            // parse and execute the sql queries from the json string
            applyQueries(parseJson(jsonData));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // DEBUG
        DataAPI.printMap("Final", DataAPI.dataDictionary);
        DataAPI.printKeyMap("Final", DataAPI.keyIndex);

        // construct the json
        return getUpdatedJson();
    }

    /**
     * construct a new json with the updated ids found through running the first json
     *
     * @return String updated json
     */
    static String getUpdatedJson() {
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
        updatedJson.put("$$_SaveSet[1]_$$", new JSONArray());
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
    public static String createTemplate(String dataSet) {
        try {
            JSONObject jsonFile = new JSONObject();
            jsonFile.put("_OE_System", "ABEHR Jason Local v3.0");
            jsonFile.put("_Version", "v0.1");

            // save in a stack, the jsonObject of dataSets, created by recursively searching for foreign keys
            // starting from a given dataSet.
            Stack<JSONObject> saveSets = new Stack<>();
            Query.getFKRelations(getSession(), dataSet, saveSets, new HashSet<String>());

            jsonFile.put("$$_XID_Map_$$", new JSONArray());
            // TODO: hardcoded value 1
            jsonFile.put("$$_SaveSet[1]_$$", saveSets);

            return jsonFile.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Main function
     * @param args args
     */
    public static void main(String[] args) {
        try {
            // create json template with all dependencies for a given dataSet
            // String jsonFromTemplate = createTemplate("event_attachment_item");
            // apply the json on the database
            // DataAPI.magic("1", jsonFromTemplate);

            /*
            String jsonData = DataAPI.getEventTemplate();
            String modifiedJsonData = DataAPI.magic("1", jsonData);
            System.out.println(jsonData);
            System.out.println(modifiedJsonData);
            */

            AttachmentData attachmentData = getSession().get(AttachmentData.class, 16);
            DataAPI.linkAttachmentDataWithEvent(attachmentData, 4686438,  "OEModule\\OphGeneric\\models\\Attachment");
            DataAPI.createAndSetThumbnailsOnAttachmentData(attachmentData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createAndSetThumbnailsOnAttachmentData(AttachmentData attachmentData) throws Exception {
        if (!Query.processAndAddThumbnails(attachmentData, DataAPI.getSession(), attachmentData.getId())) {
            System.err.println("Error in creating and setting the thumbnails.");
            return;
        }
    }

    public static void linkAttachmentDataWithEvent(AttachmentData attachmentData, int eventId, String elementTypeClassName) throws Exception {
        int eventAttachmentGroupID = Query.insertIfNotExistsAttachmentGroup(DataAPI.getSession(), "event_attachment_group", eventId, elementTypeClassName.replace("\\", "\\\\"));
        if (eventAttachmentGroupID == -1) {
            System.err.println("A new eventAttachmentGroup record could not be inserted.");
            return;
        }

        int eventAttachmentItemID = Query.insertAttachmentItem(DataAPI.getSession(), eventAttachmentGroupID, attachmentData.getId());
        if (eventAttachmentItemID == -1) {
            System.err.println("A new eventAttachmentItem record could not be inserted.");
            return;
        }
    }
}
