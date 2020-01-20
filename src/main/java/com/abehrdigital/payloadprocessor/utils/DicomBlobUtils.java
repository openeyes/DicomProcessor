package com.abehrdigital.payloadprocessor.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.sql.rowset.serial.SerialBlob;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Iterator;

public class DicomBlobUtils {

    private static final String imageFormat = "png";

    public static SerialBlob convertDicomImagesToPdf(Blob blob) throws Exception {
        ImageReader reader = getImageReaderFromBlob(blob);
        PDDocument document = new PDDocument();
        addImagesToDocument(document, reader);
        byte[] pdfBytes = convertOpenPdfDocumentToByteArray(document);

        return new SerialBlob(pdfBytes);
    }

    private static ImageReader getImageReaderFromBlob(Blob blob) throws IOException, SQLException {
        ImageInputStream inputStream = javax.imageio.ImageIO.createImageInputStream(blob.getBinaryStream());
        if (inputStream != null && inputStream.length() != 0) {
            Iterator<ImageReader> iteratorIO = ImageIO.getImageReaders(inputStream);
            if (iteratorIO != null && iteratorIO.hasNext()) {
                ImageReader reader = iteratorIO.next();
                reader.setInput(inputStream);
                return reader;
            } else {
                throw new IOException("No image readers found");
            }
        } else {
            throw new IOException("No input stream found");
        }
    }

    private static byte[] convertOpenPdfDocumentToByteArray(PDDocument document) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        document.save(byteArrayOutputStream); //Enter path to save your file with .pdf extension
        document.close();
        return byteArrayOutputStream.toByteArray();
    }

    private static void addImagesToDocument(PDDocument document, ImageReader reader) throws IOException {
        int pages = reader.getNumImages(true);

        if(pages < 1) {
            throw new IOException("No images have been found");
        }

        for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
            BufferedImage bufferedImage = reader.read(pageIndex);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject pdfImageObject = JPEGFactory.createFromImage(document, bufferedImage, 1F);

            PDPageContentStream content = new PDPageContentStream(document, page);
            content.drawImage(pdfImageObject, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());

            content.close();
        }
    }

    public static SerialBlob convertDicomBlobToSingleImage(Blob blob) throws SQLException, IOException {
        BufferedImage image = getFirstImage(blob);
        byte[] imageBytes = convertBufferedImageToByteArray(image);
        if(imageBytes.length > 0 ) {
            return new SerialBlob(imageBytes);
        } else {
            throw new IOException("Image cannot be extracted");
        }
    }

    private static BufferedImage getFirstImage(Blob blob) throws IOException, SQLException {
        ImageInputStream inputStream = ImageIO.createImageInputStream(blob.getBinaryStream());
        return ImageIO.read(inputStream);
    }

    private static byte[] convertBufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, imageFormat, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static int getHashCode(Blob blob) throws SQLException {
        int blobLength = (int) blob.length();
        byte[] blobAsBytes = blob.getBytes(1, blobLength);
        return java.util.Arrays.hashCode(blobAsBytes);
    }
}
