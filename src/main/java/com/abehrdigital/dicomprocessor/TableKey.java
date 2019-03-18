package com.abehrdigital.dicomprocessor;

import java.util.ArrayList;
import java.util.HashMap;

class TableKey {
    String primaryKey;
    HashMap<String, ArrayList<String>> uniqueKeys;

    TableKey(){}
    TableKey(String primaryKey){
        this.primaryKey = primaryKey;
    }

    @Override
    public String toString() {
        return "PK: " + primaryKey + "   uk: " + uniqueKeys;
    }
}