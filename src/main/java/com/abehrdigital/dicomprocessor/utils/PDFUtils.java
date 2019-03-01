package com.abehrdigital.dicomprocessor.utils;

import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextBox;
import org.apache.pdfbox.text.PDFTextBoxesStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import java.awt.Rectangle;
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

    /**
     * @return the width of the page in points (1pt = 1/72")
     */
    public static double pageWidth(PDPage page) {
        return page.getMediaBox().getWidth();
    }

    /**
     * @return the height of the page in points (1pt = 1/72")
     */
    public static double pageHeight(PDPage page) {
        return page.getMediaBox().getHeight();
    }

    /**
     * @param page
     * @return A List of the PDFTextBoxes on the given page
     * @throws IOException
     */
    public static List<PDFTextBox> getPdfTextBoxes(PDPage page) throws IOException {
        PDFTextBoxesStripper stripper = new PDFTextBoxesStripper(); //we only need 1, maybe move outside method
        stripper.extractPDFTextBoxes(page);
        return stripper.getPdfTextBoxes();
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

    /**
     * @param regex regular expression (with or without capturing groups).
     * @param page PDPage to be searched for matches.
     * @return a list of strings that match the capturing groups of the regex pattern.
     */
    public static List<String> findRegexMatchesByTextBox(String regex, PDPage page) throws IOException {
        List<PDFTextBox> boxes = PDFUtils.getPdfTextBoxes(page);
        Pattern pattern = Pattern.compile(regex);
        List<String> matchedStrings = new ArrayList<String>();
        for(PDFTextBox box : boxes){
            Matcher matcher = pattern.matcher(box.toString());
            if(matcher.find()) {
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    try {
                        matchedStrings.add(matcher.group(i));
                    } catch (IllegalStateException ise) {
                        System.out.println("oops"); //TODO: handle exception properly
                    }
                }
            }
        }
        return matchedStrings;
    }
}