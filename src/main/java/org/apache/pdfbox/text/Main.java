package org.apache.pdfbox.text;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("working!");
        File file = new File(args[0]);
        PDDocument pdDoc = PDDocument.load(file);
        PDPageTree pages = pdDoc.getPages();
        PDPage firstPage = pages.get(0);
        //PDFTextStripper stripper2 = new PDFTextStripper();
        //stripper2.getText(pdDoc);
        getTextFromRectangle(0,0,2000,2000, firstPage);

    }

    public static void getTextFromRectangle(double upperLeftX, double upperLeftY, double width, double height, PDPage page) throws IOException {
        PDFTextBoxesStripper stripper = new PDFTextBoxesStripper(); //we only need 1, maybe move outside method
        stripper.extractPDFTextBoxes(page);
        List<PDFTextBox> textObjs = stripper.getPdfTextBoxes();
        for (PDFTextBox textObj : textObjs){
            List<String> matchedGroups = checkForMatch(textObj, "([0-9]+)");
            for(String matchedText : matchedGroups){
                System.out.println(matchedText + "!!!!!!!!");
            }
        }
    }

    public static String getTextFromRectangle(double upperLeftX, double upperLeftY, double width, double height, PDPage page, String expectedPattern) throws IOException {
        PDFTextStripperByArea stripper = new PDFTextStripperByArea(); //we only need 1, maybe move outside method
        Rectangle2D region = new Rectangle.Double(upperLeftX, upperLeftY, width, height);
        String regionName = "testRegion";
        stripper.addRegion(regionName, region);
        stripper.extractRegions(page);
        return stripper.getTextForRegion("testRegion");
    }

    public static List<String> checkForMatch(PDFTextBox textBox, String pattern){
        return checkForMatch(textBox.toString(), pattern);
    }

    public static List<String> checkForMatch(String text, String pattern){
        List<String> matchedGroupText = new ArrayList<String>();
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(text);
        if(matcher.find()) {
            for (int i = 0; i <= matcher.groupCount(); i++) {
                try {
                    matchedGroupText.add(matcher.group(i));
                } catch (IllegalStateException ise) {
                    System.out.println("oops");
                }
            }
        }
        return matchedGroupText;
    }
}
