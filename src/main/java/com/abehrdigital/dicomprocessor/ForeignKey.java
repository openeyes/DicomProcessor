package com.abehrdigital.dicomprocessor;

public class ForeignKey {
    String referenced_column;    // foreign table; e.g. id
    String referencing_column;   // current table; e.g. service_id

    ForeignKey(String referenced_column, String referencing_column){
        this.referenced_column = referenced_column;
        this.referencing_column = referencing_column;
    }

    @Override
    public String toString() {
        return referencing_column + ">" + referenced_column;
    }
}
