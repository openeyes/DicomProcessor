package com.abehrdigital.dicomprocessor;

import java.util.ArrayList;
import java.util.HashMap;

class TableKey {
    String primaryKey;
    HashMap<String, ArrayList<String>> uniqueKeys;

    TableKey(){}
    TableKey(String pk){
        this.uniqueKeys = uniqueKeys;
    }

    @Override
    public String toString() {
        return "PK: " + primaryKey + "   uk: " + uniqueKeys;
    }
}