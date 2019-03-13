package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.utils.PDFUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.dcm4che3.data.Sequence;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class Study {

    private PDDocument pdfDoc;
    private byte[] pdfAsBytes; //We want to keep this to make sure pdf/a docs appear untampered
    private Map<Integer, String> nonSequenceDicomElements;
    private Map<Integer, Sequence> sequenceDicomElements;

    public Study() {

    }

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

    public void setPdfFields(byte[] pdfAsBytes) {
        this.pdfAsBytes = pdfAsBytes;
    }

    //Simple elements includes non-pdf and non-sequence elements only
    public void setSimpleDicomElements(Map<Integer, String> elements) {
        nonSequenceDicomElements = elements;
    }

    public void setSequenceDicomElements() {

    }

    public void savePdf(String filepath) {
        PDFUtils.savePdf(pdfDoc, filepath);
    }

    public String extractTextFromPage(double x1, double x2, double x3, double x4, int pageIndex) throws IOException {
        return PDFUtils.getTextFromRectangle(x1, x2, x3, x4, pdfDoc.getPage(pageIndex));
    }

    // for testing - do not use in final project
    // TODO: consider using Linked HashMap to maintain order
    public String dumpData() {
        String output = "";
        for (Map.Entry<Integer, String> entry : nonSequenceDicomElements.entrySet()) {
            output += entry.getKey().toString() + " " + entry.getValue() + "\n";
        }
        return output;
    }
}
