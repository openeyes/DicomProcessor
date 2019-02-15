package com.abehrdigital.dicomprocessor.utils;

import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextObject;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFUtils {
    public static PDDocument extractPdfFromBytes(byte[] binaryPDF) { //TODO: Check if PDDocument.load() solves this instead
        PDFParser pdfParser = null;
        PDDocument pdDocument = null;

        RandomAccessRead pdfData = new RandomAccessBuffer(binaryPDF);

        try {
            pdfParser = new PDFParser(pdfData);
            pdfParser.parse();
            pdDocument = pdfParser.getPDDocument();
        } catch (IOException ex) {
            System.out.println("Exception to be handled later"); //TODO: need some added exception handling
        }
        return pdDocument;
    }

    public static void savePdf(PDDocument pdf, String filepath) {
        try {
            pdf.save(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //returns the width of the page in points (1pt = 1/72")
    public static double pageWidth(PDPage page) {
        return page.getMediaBox().getWidth();
    }

    //returns the height of the page in points (1pt = 1/72")
    public static double pageHeight(PDPage page) {
        return page.getMediaBox().getHeight();
    }

    //would be better if this just took two diffrent coordinate points (upperLeft and lowerRight)
    //or maybe just take rectangle, page, and textstripper
    public static String getTextFromRectangle(double upperLeftX, double upperLeftY, double width, double height, PDPage page) throws IOException {
        PDFTextStripperByArea stripper = new PDFTextStripperByArea(); //we only need 1, maybe move outside method
        Rectangle2D region = new Rectangle.Double(upperLeftX, upperLeftY, width, height);
        String regionName = "testRegion";
        stripper.addRegion(regionName, region);
        stripper.extractRegions(page);

        return stripper.getTextForRegion("testRegion");
    }

    //TODO: Need to change this so that it uses PdfTextStripper instead of PdfTextStripperByArea - We really don't need all these arguments
    public static List<TextObject> extractPdfTextObjects(double upperLeftX, double upperLeftY, double width, double height, PDPage page) throws IOException {
        PDFTextStripperByArea stripper = new PDFTextStripperByArea(); //we only need 1, maybe move outside method
        Rectangle2D region = new Rectangle.Double(upperLeftX, upperLeftY, width, height);
        String regionName = "testRegion";
        stripper.addRegion(regionName, region);
        stripper.extractRegions(page);
        String output = stripper.getTextForRegion("testRegion");
        return stripper.getTextObjects();
        //TODO: put this stuff into a new method.
        /*
        for (TextObject textObj : textObjs){
            List<String> matchedGroups = checkForMatch(textObj, "([0-9]+)");
            for(String matchedText : matchedGroups){
                System.out.println(matchedText + "!!!!!!!!");
            }
        }
        return output;
        */
    }

    public static List<String> checkForMatch(TextObject textObj, String pattern){
        return checkForMatch(textObj.toString(), pattern);
    }

    public static List<String> checkForMatch(String text, String pattern){
        List<String> matchedGroupText = new ArrayList<String>();
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(text);
        if(matcher.find()) {
            for (int i = 0; i <= matcher.groupCount(); i++) {
                try {
                    System.out.println(text);
                    matchedGroupText.add(matcher.group(i));
                } catch (IllegalStateException ise) {
                    System.out.println("oops");
                }
            }
        }
        return matchedGroupText;
    }

}