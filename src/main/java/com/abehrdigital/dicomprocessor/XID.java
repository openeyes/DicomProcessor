package com.abehrdigital.dicomprocessor;

import java.util.TreeMap;

public class XID {
    String XID;
    String DataSet;
    TreeMap<String, String> knownFields;

    public XID(String XID, String DataSet, TreeMap<String, String> knownFields) {
        this.XID = XID;
        this.DataSet = DataSet;
        this.knownFields = knownFields;
    }

    @Override
    public String toString() {
        return "   {xid=" + XID + ", dataSet=" + DataSet + ", knownFields=" + knownFields + "}   ";
    }
}
