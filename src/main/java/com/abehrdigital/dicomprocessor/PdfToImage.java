package com.abehrdigital.dicomprocessor;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class PdfToImage {
    public static void generateImage(PDFPage page, String outFileName) throws IOException {
        if (page == null) {
            return;
        }
        // create the image
        Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(),
                (int) page.getBBox().getHeight());
        BufferedImage bufferedImage = new BufferedImage(rect.width, rect.height,
                BufferedImage.TYPE_INT_RGB);
        Image image = page.getImage(
                rect.width, rect.height,    // width & height
                rect,    // clip rect
                null,    // null for the ImageObserver
                true,    // fill background with white
                true     // block until drawing is done
        );
        Graphics2D bufImageGraphics = bufferedImage.createGraphics();
        bufImageGraphics.drawImage(image, 0, 0, null);
        ImageIO.write(bufferedImage, "PNG", new File(outFileName));
    }

    public static void convertPdfToImage(ByteBuffer blob_data, String outFileName, int[] pages) throws IOException {
        PDFFile pdf = new PDFFile(blob_data);
        for (int pageNo : pages) {
            generateImage(pdf.getPage(pageNo), outFileName + "_" + pageNo + ".png");
        }
    }
    public static void convertPdfToImage(ByteBuffer blob_data, String outFileName) throws IOException {
        PDFFile pdf = new PDFFile(blob_data);
        for (int pageNo = 1; pageNo <= pdf.getNumPages(); pageNo++) {
            generateImage(pdf.getPage(pageNo), outFileName + "_" + pageNo + ".png");
        }
    }
}
