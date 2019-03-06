package org.apache.pdfbox.text;

import java.util.List;

/**
 * This class represents a PDF Text Object as defined in the Adobe PDF Specification. The name "PDFTextBox" has been
 * chosen instead of "PDFTextObject" to avoid confusion with objects in an OOP sense.
 *
 * @author Adrian Brenton
 */
public class PDFTextBox {
    private List<TextPosition> textPositions;
    private float minX, minY, maxX, maxY;

    public PDFTextBox(List<TextPosition> textPositions){
        this.textPositions = textPositions;
        setTextBoxCoordinates();
    }


    private void setTextBoxCoordinates(){
        if(!textPositions.isEmpty()) {
            this.minX = textPositions.get(0).getX();
            this.maxX = this.minX;
            this.minY = textPositions.get(0).getY();
            this.maxY = this.minX;
            for (TextPosition textPosition : textPositions) {
                if (textPosition.getX() < minX) {
                    minX = textPosition.getX();
                }
                if (textPosition.getX() > maxX) {
                    maxX = textPosition.getX();
                }
                if (textPosition.getY() < minY) {
                    minY = textPosition.getY();
                }
                if (textPosition.getY() > maxY) {
                    maxY = textPosition.getY();
                }
            }
        }
    }

    public float width(){
        return maxY - minY;
    }

    public float height(){
        return maxX - minX;
    }

    /**
     * Return a string representation of the PDFTextBox. This String representation is concatenation of the
     * TextPositions in the textPositions member variable.
     *
     * @return The String representation of the instance.
     */
    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        for (TextPosition textPosition : textPositions) {
            stringBuilder.append(textPosition.getUnicode());
        }
        return stringBuilder.toString();
    }

    public List<TextPosition> getTextPositions(){
        return textPositions;
    }
}
