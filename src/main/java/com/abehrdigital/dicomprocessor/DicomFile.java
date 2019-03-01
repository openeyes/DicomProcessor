package com.abehrdigital.dicomprocessor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.dcm4che3.data.Sequence;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.SQLException;
import java.util.Map;

/**
 * Represents a Dicom file and its associated data.
 *
 * @author Adrian Brenton
 */
public class DicomFile {

    private PDDocument pdfDoc;
    private byte[] pdfAsBytes; //We want to keep this to make sure pdf/a docs appear untampered
    private Map<Integer, String> nonSequenceDicomElements;
    private Map<Integer, Sequence> sequenceDicomElements;

    public DicomFile() {

    }

    //TODO: for SRP, move this somewhere else
    public String getHeaderJsonString() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gson.toJson(nonSequenceDicomElements);
    }

    public PDDocument getPdfDoc() {
        return pdfDoc;
    }

    public byte[] getPdfAsBytes() {
        return pdfAsBytes;
    }

    public SerialBlob getPdfAsBlob() throws SQLException {
        return new SerialBlob(pdfAsBytes);
    }

    public void setPdfFields(byte[] pdfAsBytes, PDDocument pdfDoc) {
        this.pdfAsBytes = pdfAsBytes;
        this.pdfDoc = pdfDoc;
    }

    //Simple elements includes non-pdf and non-sequence elements only
    public void setSimpleDicomElements(Map<Integer, String> elements) {
        nonSequenceDicomElements = elements;
    }
}
