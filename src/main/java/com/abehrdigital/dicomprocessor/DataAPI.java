package com.abehrdigital.dicomprocessor;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.abehrdigital.dicomprocessor.utils.HibernateUtil;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DataAPI {

    /* dataDictionary: keeps information about mappings $$_name[X]_$$ -> value */
    static HashMap<String, XID> dataDictionary;
    /* keyIndex: store information about the keys in the table: PK and UKs */
    static HashMap<String, Key> keyIndex;
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

    static void printKeyMap(String message, HashMap<String, Key> dataDictionary) {
        System.out.println("============");
        System.out.println(message);
        for (Map.Entry<String, Key> entry : dataDictionary.entrySet()) {
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
                DataAPI.printMap("DataAPI.map: ", DataAPI.dataDictionary);

                // construct the SQL query based on the CRUD operation and the fields found in Query object
                query.constructAndRunQuery();

                // remove query from array
                queryIterator.remove();

                // set query to null to be eligible to be removed by GC
                query = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    enum JsonObjectClassType {
        String, JSONObject, JSONArray
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
        ArrayList<Query> ret = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonData);


        for (Object key : json.keySet()) {
            Object data = json.get(key.toString());

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
                case JSONObject:
                    break;
                case JSONArray:
                    if (key.toString().contains("XID_Map")) {
                        parseXID_Map((JSONArray) data);
                    } else if (key.toString().contains("SaveSet")) {
                        ret.addAll(parseSaveSet((JSONArray) data));
                    }
                    break;
                default:
                    break;
            }
        }
        return ret;
    }

    /**
     * Parse array of JSON objects
     * @param saveSet array of JSON objects
     * @return list of Query objects containing parsed information
     */
    private static ArrayList<Query> parseSaveSet(JSONArray saveSet) throws Exception {
        ArrayList<Query> ret = new ArrayList<>();
        // get the list of commands
        for (Object o : saveSet) {
            JSONObject command = (JSONObject) o;
            // get table name
            String dataSet = (String) command.get("$$_DataSet_$$");

            // get keys for this table and set them in DataAPI.keyIndex
            Query.setKeys(getSession(), dataSet);

            // get the row
            Object o3 = command.get("$$_ROW_$$");
            // row may be a JSON array
            if (o3 instanceof JSONArray) {
                // parse all JSON objects in the array
                for (Object o2 : (JSONArray) o3) {
                    // add Query object resulted to the array result
                    ret.add(parseJsonQuery((JSONObject) o2, dataSet));
                }
            // or a JSON object
            } else if (o3 instanceof JSONObject) {
                // parse JSON object and add the Query object resulted to the array result
                ret.add(parseJsonQuery((JSONObject) o3, dataSet));
            }
        }
        return ret;
    }

    /**
     * Parse initial information given as array of JSONObjects;
     * Save info into XID_map
     *
     * @param XID_MapJSON array of JSON objects to be parsed
     */
    private static void parseXID_Map(JSONArray XID_MapJSON) throws Exception {
        // create a map of XID string pointing to a XID object: eg. "$$_event_type[1]_$$" => XID Object
        for (Object o : XID_MapJSON) {
            JSONObject XIDData = (JSONObject) o;

            // get all information needed to create a new Query object
            TreeMap<String, String> knownFields = new TreeMap<>();
            String XID = null, dataSet = null;
            for (Object key1 : XIDData.keySet()) {
                String keyStr = (String) key1;
                switch (keyStr) {
                    case "$$_XID_$$":
                        XID = XIDData.get("$$_XID_$$").toString();
                        break;
                    case "$$_DataSet_$$":
                        dataSet = XIDData.get("$$_DataSet_$$").toString();
                        break;
                    default:
                        knownFields.put(keyStr, XIDData.get(keyStr).toString());
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
        Query.CRUD crud = null;
        // unknownFields: {id -> XID}
        TreeMap<String, String> unknownFields = new TreeMap<>();
        // knownFields: {id -> String:value}
        TreeMap<String, String> knownFields = new TreeMap<>();
        // foreignKeys: {XID -> {field_in_XID_table, field_in_current_table}}
        TreeMap<String, ForeignKey> foreignKeys = new TreeMap<>();
        String XID = null;

        // for each field in the json row, save the key and the value
        //   in either knownFields or unknownFields
        for (Object o : query.keySet()) {
            String key = (String) o;
            String value = query.get(key).toString();
            switch (key) {
                case "$$_CRUD_$$":
                    crud = Query.CRUD.valueOf(value);
                    break;
                default:
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
                            String pk = DataAPI.keyIndex.get(dataSet).pk;
                            if (key.equals(pk)) {
                                XID = value;
                            }

                            // save the key-value pair into unknown fields
                            unknownFields.put(key, value);

                            if (!value.equals("$$_SysDateTime_$$")) {
                                // add the unknown variable to the lookup table only if it is not a date
                                // date is set at the start of the program's execution
                                dataDictionary.put(value, new XID(value, null, null));
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
        return new Query(dataSet, XID, crud, knownFields, unknownFields, foreignKeys);
    }

    /**
     * Get the current session if it is set. If not, create one.
     * @return current session
     * @throws Exception Cannot open a new session.
     */
    static Session getSession() throws Exception {
        // if a session does not exists, open a new session
        if (session == null) {
            session = HibernateUtil.getSessionFactory().openSession();
        }
        if (session == null) {
            throw new Exception("Could not open a new session!");
        }
        return session;
    }

    /**
     * Get the current time if it is set. If not, get the time in sql format using a select query.
     * @return current date time
     * @throws Exception Cannot get current time.
     */
    static String getTime() throws Exception {
        if (DataAPI.time == null) {
            DataAPI.time = Query.getTime(session);
        }
        if (session == null) {
            throw new Exception("Could not get current date time!");
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

    /**
     * Apply all sql queries after parsing the json string
     * @param id int id of user who makes the changes
     * @param jsonData String json to be parsed
     * @throws Exception Nothing to parse; Could not open a new session!; Could not get current date time!
     */
    static void magic(String id, String jsonData) throws Exception {
        if (jsonData.isEmpty()) {
            throw new Exception("Nothing to parse");
        }
        try {
            // TODO: use "user_id" for insert/merge operations
            /* basic initialization */
            dataDictionary = new HashMap<>();
            keyIndex = new HashMap<>();
            userId = id;

            // set session and time at the start of the execution
            session = getSession();
            time = getTime();

            // parse and execute the sql queries from the json string
            applyQueries(parseJson(jsonData));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // DEBUG
        DataAPI.printMap("Final", DataAPI.dataDictionary);
        DataAPI.printKeyMap("Final", DataAPI.keyIndex);
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

            DataAPI.magic("1", DataAPI.getEventTemplate());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
