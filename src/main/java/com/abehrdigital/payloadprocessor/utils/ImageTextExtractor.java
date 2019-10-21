package com.abehrdigital.payloadprocessor.utils;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageTextExtractor {
    private Tesseract imageReader;
    private static final String imageReaderDataPath ="src/main/resources/tessdata/";

    public ImageTextExtractor(){
        imageReader = new Tesseract();
    }


    public  String read(BufferedImage image, Rectangle rectangle) throws TesseractException {
        String text = imageReader.doOCR(image, rectangle);
        text = text.trim().replaceAll("\n", "");
        return text;
    }
}
