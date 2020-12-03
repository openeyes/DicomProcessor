package com.abehrdigital.payloadprocessor.utils;

import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;

public class PDFUtils {
    public static PDDocument extractPdfDocumentFromBytes(byte[] binaryPDF) throws IOException { //TODO: Check if PDDocument.load() solves this instead
        PDFParser pdfParser = null;
        PDDocument pdDocument = null;

        RandomAccessRead pdfData = new RandomAccessBuffer(binaryPDF);

        try {
            pdfParser = new PDFParser(pdfData);
            pdfParser.parse();
            pdDocument = pdfParser.getPDDocument();
        } catch (IOException ex) {
            throw ex;
        }
        return pdDocument;
    }

    public static PDDocument extractPdfDocumentFromBlob(Blob binaryBlop) throws SQLException, IOException {
        int blopLength = (int) binaryBlop.length();
        return extractPdfDocumentFromBytes(binaryBlop.getBytes(1,blopLength));
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
}