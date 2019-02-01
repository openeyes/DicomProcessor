package com.abehrdigital.dicomprocessor;

import java.io.FileNotFoundException;
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
    static String _OE_System;
    static String _Version;
    static Session session;
    static String time;

    public static void printMap(String message, HashMap<String, XID> XID_map) {
        System.out.println("============");
        System.out.println(message);
        for (Map.Entry<String, XID> entry : XID_map.entrySet()) {
            System.out.println(entry.getKey() + "  =>  " + entry.getValue());
        }
        System.out.println("============");
    }

    public static void printArr(ArrayList<Query> arr) {
        System.out.println("____________________");
        for (Object o : arr) {
            System.out.println(o);
        }
        System.out.println("____________________");
    }



    /**
     * apply the actions inside a json string to the database
     */
    public static void applyQuery(String jsonFileName) {
        ArrayList<Query> queries;
        try {
            queries = parseJson(jsonFileName);
            Iterator itr = queries.iterator();

            int i = 0;
            int count = 6;
            while (itr.hasNext())
            {
                if (i >= count)
                    break;
                Query q = (Query) itr.next();
                System.out.println("--"+q+"--");

                // at the moment, secondary_query_insert is the select query coming from the insert; to get the info inserted
                String secondary_query_insert = q.constructQuery(XID_map);

                // execute query and the secondary query
                q.executeQuery(session, secondary_query_insert);

                // remove query from array
                itr.remove();

                // set query to null to be eligible to be removed by GC
                q = null;

                i++;
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
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ParseException
     */
    private static ArrayList<Query> parseJson(String filename) throws FileNotFoundException, IOException, ParseException {
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

    public static Session getSession() throws Exception {
        // if a session does not exists, open a new session
        if (session == null) {
            session = HibernateUtil.getSessionFactory().openSession();
        }
        if (session == null) {
            throw new Exception("Could not open a new session!");
        }
        return session;
    }

    public static void main(String[] args) {
        try {
            session = getSession();
            time = Query.getTime(session);

            applyQuery("C:/Users/Stefan/Desktop/JSON.json");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO: if there are multiple RETRIEVE queries on a table, advance number of XID?
        // table[1]
        // table[2]...
        // not for me to do
        /*
         * 1. when there is a retrieve call, how many columns can we retrieve?
         * Answer: ONLY ONE; generally, the ID (pk) ((and) a unique key)
         * 2. what is the naming of the XID object?
         * Answer: $$_tableName[i]_$$, where i is the current row requested from the respective table
         * 3. if there is a query: [select * from table where name="stefan"]; what will be the $$_XID_$$ for this call?
         * Answer: this query is invalid. There should always be a column specified to be selected
         * 4. what if a query returns more than 1 row? for example, [select * from table] => 10 rows
         * Answer: 1 row; multiple rows -> exceptions; no rows -> maybe good? (STRETCH GOAL)
         */
    }
}
