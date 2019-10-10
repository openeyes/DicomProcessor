package com.abehrdigital.dicomprocessor;


import org.dcm4che3.data.Attributes;
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
        for (int tag : attributes.tags()) {

            if (Tag.EncapsulatedDocument != TagUtils.normalizeRepeatingGroup(tag)) {
                byte[] valueAsByte;
                try {
                    valueAsByte = (byte[]) attributes.getValue(tag); //casted because getValue() returns Object
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
        return elements;
    }

    public void run() throws IOException, SQLException {
        init();
        study.setDicomBlob(dicomBlob);
        study.setSimpleDicomElements(simpleElements());
        study.setAttachmentBytes(attachmentBytes);
    }

    public Study getStudy() {
        return study;
    }
}
