package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.utils.PDFUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.dcm4che3.data.Sequence;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.sql.rowset.serial.SerialBlob;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Map;

public class Study {

    private PDDocument pdfDoc;
    private byte[] attachmentBytes;
    private byte[] imageBytes;
    private Blob dicomBlob;
    private static final String imageFormat = "png";
    private Map<String, String> nonSequenceDicomElements;
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

    public SerialBlob getAttachmentAsBlob() throws SQLException {
        return new SerialBlob(attachmentBytes);
    }

    public SerialBlob getImageAsBlob() throws SQLException, IOException {
        ImageInputStream inputStream = javax.imageio.ImageIO.createImageInputStream(dicomBlob.getBinaryStream());
        BufferedImage image = ImageIO.read(inputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, imageFormat, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return new SerialBlob(imageBytes);
    }

    public void setDicomBlob(Blob dicomBlob) {
        this.dicomBlob = dicomBlob;
    }


    public void setAttachmentBytes(byte[] attachmentBytes) {
        this.attachmentBytes = attachmentBytes;
    }

    //Simple elements includes non-pdf and non-sequence elements only
    public void setSimpleDicomElements(Map<String, String> elements) {
        nonSequenceDicomElements = elements;
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
        for (Map.Entry<String, String> entry : nonSequenceDicomElements.entrySet()) {
            output += entry.getKey().toString() + " " + entry.getValue() + "\n";
        }
        return output;
    }
}
