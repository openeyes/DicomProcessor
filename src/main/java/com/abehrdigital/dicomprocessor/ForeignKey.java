package com.abehrdigital.dicomprocessor;

public class ForeignKey {
    public String referenced_column;    // foreign table => id
    public String referencing_column;   // current table => service_id

    public ForeignKey(String referenced_column, String referencing_column){
        this.referenced_column = referenced_column;
        this.referencing_column = referencing_column;
    }
}
