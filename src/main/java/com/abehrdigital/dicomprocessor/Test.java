package com.abehrdigital.dicomprocessor;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Test {
    // TODO: SELECT episode_id, id, event_type_id FROM event (.....?? where??)
    static HashMap<String, XID> XID_map = new HashMap<>();
    static String _OE_System;
    static String _Version;

    /**
     * apply the actions inside a json string to the database
     */
    public static void applyQuery(String jsonFileName) {
        ArrayList<Query> queries;
        try {
            queries = parseJson(jsonFileName);

            for (int i = 0; i < 0; i++) {// for (Query q : queries) {////
                doQueryByExample(queries.get(i));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save in a map the results from applying the query.
     */
    private static boolean doQueryByExample(Query q) {
        boolean changesWereMade = false;
        // at the moment, secondary_query_insert is the select query coming from the insert; to get the info inserted
        String secondary_query_insert = q.constructQuery(XID_map);
        HashMap<String, String> queryResult = q.executeQuery(secondary_query_insert);


        System.out.println("RESULT: ");
        System.out.println(queryResult);

        if (queryResult == null)
            return false;

        //TODO: executeQuery should insert new values in the XID_map
        // this \|/ is not required anymore, only for completing the Query fields
//        // assuming there is only one record.
//        // TODO: what if there are multiple records returned??
//        for (Map.Entry<String, String> entry : q.unknownFields.entrySet()) {
//            String unknownField = entry.getKey(); // ex: id, name
//            if (queryResult.containsKey(unknownField)) {
//                String valueFound = queryResult.get(unknownField);
//                // save the values of the returned variables in the map
//                XID_map.put(entry.getValue(), valueFound);
//                changesWereMade = true;
//            }
//        }
        return changesWereMade;
    }


    enum JsonObjectClassType {
        String, JSONObject, JSONArray
    }

    /**
     * Open and parse a JSON file and return a list of Query objects
     *
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ParseException
     */
    private static ArrayList<Query> parseJson(String filename) throws FileNotFoundException, IOException, ParseException {
        ArrayList<Query> ret = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(new FileReader(filename));

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

    public static ArrayList<Query> parse_save_set(JSONArray saveSet) {
        ArrayList<Query> ret = new ArrayList<>();
        // get the list of commands
        for (Object o : saveSet) {
            JSONObject command = (JSONObject) o;
            // get table name
            String dataSet = (String) command.get("$$_DataSet_$$");
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

    public static void parse_XID_Map(JSONArray XID_Map) {
        // create a map of XID string pointing to a XID object: eg. "$$_event_type[1]_$$" => XID Object
        for (Object o : XID_Map) {
            JSONObject XID_data = (JSONObject) o;

            TreeMap<String, String> knownFields = new TreeMap<>();
            String XID = null, DataSet = null;
            for (Object key1 : XID_data.keySet()) {
                String keyStr = (String) key1;
                switch (keyStr) {
                    case "$$_XID_$$":
                        XID = XID_data.get("$$_XID_$$").toString();
                        break;
                    case "$$_DataSet_$$":
                        DataSet = XID_data.get("$$_DataSet_$$").toString();
                        break;
                    default:
                        knownFields.put(keyStr, XID_data.get(keyStr).toString());
                        break;
                }
            }
            // save the info in a new XID object
            XID_map.put(XID, new XID(XID, null, DataSet, knownFields));
        }
    }

    /**
     * Parse contents of a JSON object "$$_ROW_$$": get the CRUD and a list of known and unknown fields
     *
     * @param query
     * @param dataSet
     * @return
     */
    public static Query parseJsonQuery(JSONObject query, String dataSet) {
        String CRUD = null;
        // unknownFields: {id -> XID}
        TreeMap<String, String> unknownFields = new TreeMap<>();
        // knownFields: {id -> String:value}
        TreeMap<String, String> knownFields = new TreeMap<>();
        // foreignKeys: {XID -> String:field_in_XID_table}
        TreeMap<String, String> foreignKeys = new TreeMap<>();

        // for each field in the json row, save the key and the value
        //   in either knownFields or unknownFields
        for (Iterator iterator = query.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next().toString();
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
                            String[] split = value.split(".");
                            foreignKeys.put(split[0], split[1]);
                        } else {
                            unknownFields.put(key, value);
                            // add the unknown variable to the lookup table
                            XID_map.put(value, null);
                        }
                    } else {
                        knownFields.put(key, value);
                    }
                    break;
            }
        }

        // create new Query object with the information parsed
        return new Query(dataSet, CRUD, knownFields, unknownFields, foreignKeys);
    }

    public static void main(String[] args) {
        applyQuery("C:/Users/Stefan/Desktop/JSON.json");

        System.out.println(_OE_System);
        System.out.println(_Version);
        System.out.println(XID_map);
    }
}
