package com.abehrdigital.payloadprocessor;


import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.util.TagUtils;

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
    private byte[] attachmentBytes;
    private static final int MAXIMUM_SIMPLE_ELEMENT_BYTE_LENGTH = 1000;
    private static final String IMAGE_TYPE_FOR_MANUAL_EXTRACTION = "IJG (jpeg-6b) library with lossless patch";


    public DicomParser(Blob dicomBlob, Study study) throws Exception {
        this.dicomBlob = dicomBlob;
        this.study = study;
        run();
    }

    private void init() throws IOException, SQLException {
        dicomInputStream = new DicomInputStream(dicomBlob.getBinaryStream());
        attributes = dicomInputStream.readDataset(-1, -1);
    }

    private Map<String, String> simpleElements() {
        Map<String, String> elements = new HashMap<>();
        boolean manualPixelDataExtractionRequired = false;
        for (int tag : attributes.tags()) {

            if (Tag.EncapsulatedDocument != TagUtils.normalizeRepeatingGroup(tag)) {
                byte[] valueAsByte;
                try {
                    valueAsByte = (byte[]) attributes.getValue(tag); //casted because getValue() returns Object
                    if (tag == Tag.DerivationDescription) {
                        String value = new String(valueAsByte, Charset.forName("UTF-8")).trim();
                        if (value.equals(IMAGE_TYPE_FOR_MANUAL_EXTRACTION)) {
                            manualPixelDataExtractionRequired = true;
                        }
                    }
                } catch (ClassCastException ex) {
                    //TODO: handle cases where value is a sequence
                    continue;
                }

                if (valueAsByte.length < MAXIMUM_SIMPLE_ELEMENT_BYTE_LENGTH) {
                    String value = new String(valueAsByte, Charset.forName("UTF-8"));
                    elements.put(Integer.toHexString(tag), value.trim());
                }
            } else {
                attachmentBytes = (byte[]) attributes.getValue(tag);
            }
        }

        if (manualPixelDataExtractionRequired) {
            extractPixelDataManually();
        }
        return elements;
    }

    private void extractPixelDataManually() {
        for (int tag : attributes.tags()) {
            if (tag == Tag.PixelData) {
                byte[] valueAsByte;

                for (Object var : (Fragments) attributes.getValue(tag)) {
                    try {
                        valueAsByte = (byte[]) var;
                        attachmentBytes = valueAsByte;

                    } catch (ClassCastException ex) {
                        //Ignore everything that is not a byte array
                    }
                }
            }
        }
    }

    public void run() throws Exception {
        init();
        study.setDicomBlob(dicomBlob);
        study.setSimpleDicomElements(simpleElements());
        study.setAttachmentBytes(attachmentBytes);
    }

    public Study getStudy() {
        return study;
    }
}
