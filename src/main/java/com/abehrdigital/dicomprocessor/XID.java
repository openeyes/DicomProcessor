package com.abehrdigital.dicomprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class XID {
    String XID;
    String DataSet;
    TreeMap<String, String> knownFields;
    HashMap<String, ArrayList<Key>> dataSetKeys;

    XID(String XID, String DataSet, TreeMap<String, String> knownFields, HashMap<String, ArrayList<Key>> dataSetKeys) {
        this.XID = XID;
        this.DataSet = DataSet;
        this.knownFields = knownFields;
        this.dataSetKeys = dataSetKeys;
    }

    @Override
    public String toString() {
        return "   {xid=" + XID + ", dataSet=" + DataSet + ", knownFields=" + knownFields + ", keys= " + dataSetKeys + "}   ";
    }
}
