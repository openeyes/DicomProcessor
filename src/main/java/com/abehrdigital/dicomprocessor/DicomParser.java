package com.abehrdigital.dicomprocessor;


import com.abehrdigital.dicomprocessor.utils.PDFUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomInputStream;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DicomParser {
    private Study study;
    private Blob dicomBlob;
    private DicomInputStream dicomInputStream;
    private Attributes attributes;
    private int pdfTagNumber;

    public DicomParser(Blob dicomBlob) {
        this.dicomBlob = dicomBlob;
        //this.file = dicomFile;
        this.study = new Study(); //TODO: consider injecting this dependency
        this.pdfTagNumber = 4325393; // TODO: change so that this is not hardcoded
        try {
            run();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void init() throws IOException, SQLException {
        System.out.println(dicomBlob);
        dicomInputStream = new DicomInputStream(dicomBlob.getBinaryStream());
        attributes = dicomInputStream.readDataset(-1, -1);
    }

    private Map<Integer, String> simpleElements() {
        Map<Integer, String> elements = new HashMap<>();
        for (int tag : attributes.tags()) {
            if (tag != pdfTagNumber) {
                byte[] valueAsByte;
                try {
                    valueAsByte = (byte[]) attributes.getValue(tag); //casted because getValue() returns Object
                } catch (ClassCastException ex) {
                    //TODO: handle cases where value is a sequence
                    continue;
                }
                String value = new String(valueAsByte, Charset.forName("UTF-8"));
                elements.put(tag, value);
            }
        }
        return elements;
    }

    public void run() throws IOException, SQLException {
        init();
        // Put set study pdf member variables:
        byte[] pdfBytes = attributes.getBytes(pdfTagNumber);
        study.setPdfFields(pdfBytes, PDFUtils.extractPdfFromBytes(pdfBytes));
        study.setSimpleDicomElements(simpleElements());
    }

    public Study getStudy() {
        return study;
    }


}
