package com.abehrdigital.payloadprocessor.utils;

import java.util.Random;

public class RandomStringGenerator {
    private static final String candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String generateWithDefaultChars(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();

        for (int iterationCount = 0; iterationCount < length; iterationCount++) {
            stringBuilder.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }

        return stringBuilder.toString();
    }
}
