package com.abehrdigital.payloadprocessor;

public class ForeignKey {
    String referencedColumn;    // foreign table; e.g. id
    String referencingColumn;   // current table; e.g. service_id

    ForeignKey(String referenced_column, String referencing_column){
        this.referencedColumn = referenced_column;
        this.referencingColumn = referencing_column;
    }

    @Override
    public String toString() {
        return referencingColumn + "=>" + referencedColumn;
    }
}
