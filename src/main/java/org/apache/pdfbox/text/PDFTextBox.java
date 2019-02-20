package org.apache.pdfbox.text;

import java.util.List;

public class PDFTextBox {
    private List<TextPosition> textPositions;
    private String asString;
    private float minX, minY, maxX, maxY;

    public PDFTextBox(List<TextPosition> textPositions){
        this.textPositions = textPositions;
        storeString(textPositions);
    }

    private void storeString(List<TextPosition> textPositions){
        StringBuilder stringBuilder = new StringBuilder();
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

                stringBuilder.append(textPosition.getUnicode());
            }
            this.asString = stringBuilder.toString();
        }
    }

    public float width(){
        return maxY - minY;
    }

    public float height(){
        return maxX - minX;
    }

    @Override
    public String toString(){
        return asString;
    }

    public List<TextPosition> getTextPositions(){
        return textPositions;
    }
}
