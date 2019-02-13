package org.apache.pdfbox.text;

import java.io.IOException;
import java.util.List;

public class PDFTextObjectStripper extends PDFTextStripper{
    private List<TextObject> textObjects;

    /**
     * Instantiate a new PDFTextStripper object.
     *
     * @throws IOException If there is an error loading the properties.
     */
    public PDFTextObjectStripper() throws IOException {
    }

    public List<TextObject> getTextObjects(){
        return textObjects;
    }

    public TextObject getTextObject(int index){
        return textObjects.get(index);
    }
}
