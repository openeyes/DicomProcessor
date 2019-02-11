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

public class Test {

    static HashMap<String, XID> XID_map = new HashMap<>();
    private static String _OE_System;
    private static String _Version;
    private static Session session;
    private static String time;

    /**
     * Pretty print HashMap with additional message
     * @param message Custom message
     * @param XID_map HashMap to be printed
     */
    static void printMap(String message, HashMap<String, XID> XID_map) {
        System.out.println("============");
        System.out.println(message);
        for (Map.Entry<String, XID> entry : XID_map.entrySet()) {
            System.out.println(entry.getKey() + "  =>  " + entry.getValue());
        }
        System.out.println("============");
    }

    /**
     * apply the actions inside a json string to the database
     */
    private static void applyQuery(String jsonFileName) {
        ArrayList<Query> queries;
        try {
            queries = parseJson(jsonFileName);
            Iterator itr = queries.iterator();

            while (itr.hasNext()) {
                Query query = (Query) itr.next();

                // DEBUG purposes
                Test.printMap("Test.map: ", Test.XID_map);
                System.out.println("--" + query + "--");

                // construct the SQL query based on the CRUD operation and the fields found in Query object
                int rows_affected = query.constructAndRunQuery();

                // remove query from array
                itr.remove();

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
     * @param filename name of the json to be parsed
     * @return a list of Query objects, containing parsed information from the json
     * @throws IOException file not found
     * @throws ParseException parser exception
     */
    private static ArrayList<Query> parseJson(String filename) throws Exception {
        ArrayList<Query> ret = new ArrayList<>();
        JSONParser parser = new JSONParser();
        FileReader reader = new FileReader(filename);
        JSONObject json = (JSONObject) parser.parse(reader);

        for (Object key : json.keySet()) {
            Object data = json.get(key.toString());

            // depending on the current object class, parse the data
            JsonObjectClassType json_class = JsonObjectClassType.valueOf(data.getClass().getSimpleName());
            switch (json_class) {
                case String:
                    switch (key.toString()) {
                    case "_OE_System":
                        Test._OE_System = data.toString();
                        break;
                    case "_Version":
                        Test._Version = data.toString();
                        break;
                    default:
                        break;
                }
                    break;
                case JSONObject:
                    break;
                case JSONArray:
                    if (key.toString().contains("XID_Map")) {
                        parse_XID_Map((JSONArray) data);
                    } else if (key.toString().contains("SaveSet")) {
                        ret.addAll(parse_save_set((JSONArray) data));
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
    private static ArrayList<Query> parse_save_set(JSONArray saveSet) throws Exception {
        ArrayList<Query> ret = new ArrayList<>();
        // get the list of commands
        for (Object o : saveSet) {
            JSONObject command = (JSONObject) o;
            // get table name
            String dataSet = (String) command.get("$$_DataSet_$$");

            // get keys for this table
            HashMap<String, ArrayList<Key>> dataSetKeys = Query.getKeys(getSession(), dataSet);

            // get the row
            Object o3 = command.get("$$_ROW_$$");
            // row may be a JSON array
            if (o3 instanceof JSONArray) {
                // parse all JSON objects in the array
                for (Object o2 : (JSONArray) o3) {
                    // add Query object resulted to the array result
                    ret.add(parseJsonQuery((JSONObject) o2, dataSet, dataSetKeys));
                }
            // or a JSON object
            } else if (o3 instanceof JSONObject) {
                // parse JSON object and add the Query object resulted to the array result
                ret.add(parseJsonQuery((JSONObject) o3, dataSet, dataSetKeys));
            }
        }
        return ret;
    }

    /**
     * Parse initial information given as array of JSONObjects;
     * Save info into XID_map
     *
     * @param XID_Map_JSON array of JSON objects to be parsed
     */
    private static void parse_XID_Map(JSONArray XID_Map_JSON) {
        // create a map of XID string pointing to a XID object: eg. "$$_event_type[1]_$$" => XID Object
        for (Object o : XID_Map_JSON) {
            JSONObject XID_data = (JSONObject) o;

            // get all information needed to create a new Query object
            TreeMap<String, String> knownFields = new TreeMap<>();
            String XID = null, dataSet = null;
            for (Object key1 : XID_data.keySet()) {
                String keyStr = (String) key1;
                switch (keyStr) {
                    case "$$_XID_$$":
                        XID = XID_data.get("$$_XID_$$").toString();
                        break;
                    case "$$_DataSet_$$":
                        dataSet = XID_data.get("$$_DataSet_$$").toString();
                        break;
                    default:
                        knownFields.put(keyStr, XID_data.get(keyStr).toString());
                        break;
                }
            }
            // save the info in a new XID object
            XID_map.put(XID, new XID(XID, dataSet, knownFields, Query.getKeys(session, dataSet)));
        }
    }

    /**
     * Parse contents of a JSON object "$$_ROW_$$": get the CRUD and a list of known and unknown fields
     *
     * @param query JSONObject query to be parsed
     * @param dataSet table name
     * @return a new Query object with parsed information
     */
    private static Query parseJsonQuery(JSONObject query, String dataSet, HashMap<String, ArrayList<Key>> dataSetKeys) {
        String CRUD = null;
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
                    CRUD = value;
                    break;
                default:
                    // unknown fields have value prefixed and suffixed with '$$'
                    if (value.startsWith("$$") && value.endsWith("$$")) {
                        // if this is a reference to a field from another table
                        if (value.contains(".")) {
                            String[] split = value.split("\\.");

                            String referenced_XID = split[0] + "_$$";
                            String referencing_column = key;
                            String referenced_column = split[1].substring(0,split[1].length() - 3);

                            foreignKeys.put(referenced_XID, new ForeignKey(referenced_column, referencing_column));
                        } else {
                            // save the XID encoding for the primary key field:
                            if (key.equals(dataSetKeys.get("PRIMARY KEY").get(0).columnName)) {
                                XID = value;
                            }

                            // save the key-value pair into unknown fields
                            unknownFields.put(key, value);

                            if (!value.equals("$$_SysDateTime_$$")) {
                                // add the unknown variable to the lookup table only if it is not a date
                                // date is set at the start of the program's execution
                                XID_map.put(value, new XID(value, null, null, dataSetKeys));
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
        return new Query(dataSet, XID, CRUD, knownFields, unknownFields, foreignKeys);
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
        if (Test.time == null) {
            Test.time = Query.getTime(session);
        }
        if (session == null) {
            throw new Exception("Could not get current date time!");
        }
        return Test.time;
    }

    /**
     * Main function
     * @param args args
     */
    public static void main(String[] args) {
        try {
            // set session and time at the start of the execution
            session = getSession();
            time = getTime();

            // parse and apply the query contained in the JSON file
            applyQuery("./src/JSON2.json");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Test.printMap("Final", Test.XID_map);
    }
}
