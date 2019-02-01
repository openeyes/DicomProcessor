package com.abehrdigital.dicomprocessor;

import java.util.TreeMap;

public class XID {
    String XID;
    String DataSet;
    String value;
    TreeMap<String, String> knownFields;

    public XID() {
        knownFields = new TreeMap<>();
    }

    public XID(String XID, String value, String DataSet, TreeMap<String, String> knownFields) {
        this.XID = XID;
        this.value = value;
        this.DataSet = DataSet;
        this.knownFields = knownFields;
    }

    @Override
    public String toString() {
        return "   {xid=" + XID + ", dataSet=" + DataSet + ", knownFields=" + knownFields + "}   ";
    }
}