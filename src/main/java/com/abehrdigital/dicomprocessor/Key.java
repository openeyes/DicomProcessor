package com.abehrdigital.dicomprocessor;

import java.util.ArrayList;
import java.util.HashMap;

class Key {
    String pk;
    HashMap<String, ArrayList<String>> uk;  //uk_name->[col1,col2..]

    public Key(){}
    public Key(String pk){
        this.pk = pk;
    }

    @Override
    public String toString() {
        return "PK: " + pk + "   uk: " + uk;
    }
}