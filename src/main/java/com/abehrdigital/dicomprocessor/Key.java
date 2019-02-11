package com.abehrdigital.dicomprocessor;

class Key {
    String columnName;
    String tableName;
    public Key(String columnName, String tableName) {
        this.columnName = columnName;
        this.tableName = tableName;
    }

    @Override
    public String toString() {
        return columnName + " -> " + tableName;
    }
}