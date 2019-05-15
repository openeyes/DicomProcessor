package com.abehrdigital.dicomprocessor;


import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomInputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
    private static final String imageFormat = "png";
    private Attributes attributes;
    private int pdfTagNumber;

    public DicomParser(Blob dicomBlob) throws Exception {
        this.dicomBlob = dicomBlob;
        //this.file = dicomFile;
        this.study = new Study(); //TODO: consider injecting this dependency
        this.pdfTagNumber = 4325393; // TODO: change so that this is not hardcoded
        try {
            run();
        } catch (Exception exception) {
            throw new Exception(exception);
        }
    }

    private void init() throws IOException, SQLException {
        dicomInputStream = new DicomInputStream(dicomBlob.getBinaryStream());
        attributes = dicomInputStream.readDataset(-1, -1);
    }

    private Map<String, String> simpleElements() {
        Map<String, String> elements = new HashMap<>();
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
                elements.put(Integer.toHexString(tag), value.trim());
            }
        }
        return elements;
    }

    public void run() throws IOException, SQLException {
        init();
        // Put set study pdf member variables:
        byte[] pdfBytes = attributes.getBytes(pdfTagNumber);
        ImageInputStream inputStream = javax.imageio.ImageIO.createImageInputStream(dicomBlob.getBinaryStream());
        BufferedImage image = ImageIO.read(inputStream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, imageFormat, baos);
        study.setImageFields(baos.toByteArray());
        study.setPdfFields(pdfBytes);
        study.setSimpleDicomElements(simpleElements());
    }

    public Study getStudy() {
        return study;
    }
}
