package com.abehrdigital.dicomprocessor.utils;

import com.abehrdigital.dicomprocessor.models.AttachmentData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import javax.sql.rowset.serial.SerialBlob;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.sql.Blob;
import java.sql.SQLException;

public class AttachmentDataThumbnailAdder {
    private static final String DEFAULT_IMAGE_FORMAT = "jpg";
    private static final ImageType DEFAULT_IMAGE_TYPE = ImageType.RGB;
    private static final int DEFAULT_PAGE_INDEX_FOR_THUMBNAIL_EXTRACTION = 0;
    private static final int SMALL_THUMBNAIL_SIZE = 20;
    private static final int MEDIUM_THUMBNAIL_SIZE = 50;
    private static final int LARGE_THUMBNAIL_SIZE = 100;

    public static void addThumbnails(AttachmentData attachmentData) throws Exception {
        Blob attachmentDataBlob = attachmentData.getBlobData();
        if (attachmentDataBlob != null) {
            int blobLength = (int) attachmentDataBlob.length();
            int startingBlobPosition = 1;
            PDDocument pdfBlobDocument = PDDocument.load(new ByteArrayInputStream(
                    attachmentDataBlob.getBytes(startingBlobPosition, blobLength)
            ));
            PDFRenderer pdfRenderer = new PDFRenderer(pdfBlobDocument);

            Blob thumbnailSmallBlob = getThumbnail(SMALL_THUMBNAIL_SIZE, pdfRenderer);
            Blob thumbnailMediumBlob = getThumbnail(MEDIUM_THUMBNAIL_SIZE, pdfRenderer);
            Blob thumbnailLargeBlob = getThumbnail(LARGE_THUMBNAIL_SIZE, pdfRenderer);

            attachmentData.setSmallThumbnail(thumbnailSmallBlob);
            attachmentData.setMediumThumbnail(thumbnailMediumBlob);
            attachmentData.setLargeThumbnail(thumbnailLargeBlob);

            pdfBlobDocument.close();
        } else {
            throw new InvalidObjectException("Blob data is null in the provided attachment Data");
        }
    }

    private static Blob getThumbnail(int dpiSize, PDFRenderer pdfRenderer) throws IOException, SQLException {
        BufferedImage bufferedImageFromPDF = pdfRenderer.renderImageWithDPI(
                DEFAULT_PAGE_INDEX_FOR_THUMBNAIL_EXTRACTION,
                dpiSize,
                DEFAULT_IMAGE_TYPE
        );
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImageFromPDF, DEFAULT_IMAGE_FORMAT, byteArrayOutputStream);
        byteArrayOutputStream.flush();
        byte[] imageInByte = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        return new SerialBlob(imageInByte);
    }
}
