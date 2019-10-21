package com.abehrdigital.payloadprocessor.utils;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageTextExtractor {
    private Tesseract imageReader;
    private static final String imageReaderDataPath ="C:\\tessdata\\";

    public ImageTextExtractor(){
        imageReader = new Tesseract();
        imageReader.setDatapath(imageReaderDataPath);
    }


    public  String read(BufferedImage image, Rectangle rectangle) throws TesseractException {
        String text = imageReader.doOCR(image, rectangle);
        text = text.trim().replaceAll("\n", "");
        return text;
    }
}
