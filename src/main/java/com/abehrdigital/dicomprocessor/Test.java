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
    static HashMap<String, String> map = new HashMap<String, String>();

    /**
     * apply the actions inside a json string to the database
     */
    public static void applyQuery(String jsonFileName) {
        ArrayList<Query> queries;
        try {
            queries = parseJson(jsonFileName);

            boolean changesWereMade = true;
//			while (changesWereMade) {
//				changesWereMade = false;
            for (int i = 0; i < 1; i++) {// for (Query q : queries) {////
                changesWereMade |= doQueryByExample(queries.get(i));
//				}
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
        q.constructQuery(map);
        HashMap<String, String> queryResult = q.executeQuery();

        if (queryResult == null)
            return false;

        // assuming there is only one record.
        // TODO: what if there are multiple records returned??
        for (Map.Entry<String, String> entry : q.unknownFields.entrySet()) {
            String unknownField = entry.getKey(); // ex: id, name
            if (queryResult.containsKey(unknownField)) {
                String valueFound = queryResult.get(unknownField);
                // save the values of the returned variables in the map
                map.put(entry.getValue(), valueFound);
                changesWereMade = true;
            }
        }
        return changesWereMade;
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
        ArrayList<Query> ret = new ArrayList<Query>();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(new FileReader(filename));

        //TODO: THERE ARE MULTIPLE ARRAYS OF $$_SaveSet[1]_$$

        // get the list of commands
        JSONArray saveSet = (JSONArray) json.get("$$_SaveSet[1]_$$");
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
        System.out.println(ret);

        //TODO: what should be done with $$_XID_Map_$$ ???
        map.put("$$_event_type[1]_$$", "27");

        return ret;
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
        TreeMap<String, String> unknownFields = new TreeMap<String, String>();
        TreeMap<String, String> knownFields = new TreeMap<String, String>();

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
                        unknownFields.put(key, value);
                        // add the unknown variable to the lookup table
                        map.put(value, null);
                    } else {
                        knownFields.put(key, value);
                    }
                    break;
            }
        }

        // create new Query object with the information parsed
        return new Query(dataSet, CRUD, knownFields, unknownFields);
    }

    public static void main(String[] args) {
        applyQuery("C:/Users/Stefan/Desktop/JSON.json");
//        applyQuery("C:/Users/Stefan/Desktop/2019-01-24a JSON.json");
    }
}
