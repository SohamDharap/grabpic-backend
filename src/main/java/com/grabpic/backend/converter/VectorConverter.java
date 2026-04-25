package com.grabpic.backend.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Converter(autoApply = false)
public class VectorConverter implements AttributeConverter<float[], String> {

    private static final int VECTOR_DIMENSION = 512;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.######", 
            DecimalFormatSymbols.getInstance(Locale.US));

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        
        if (attribute.length != VECTOR_DIMENSION) {
            throw new IllegalArgumentException(
                "Vector dimension must be " + VECTOR_DIMENSION + ", got " + attribute.length);
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(DECIMAL_FORMAT.format(attribute[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        // Handle both pgvector array format and bracket format
        String content = dbData.trim();
        if (content.startsWith("[") && content.endsWith("]")) {
            content = content.substring(1, content.length() - 1);
        }

        if (content.isEmpty()) {
            return new float[VECTOR_DIMENSION];
        }

        String[] parts = content.split(",");
        float[] result = new float[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            try {
                String trimmed = parts[i].trim();
                if (trimmed.isEmpty()) {
                    result[i] = 0.0f;
                } else {
                    result[i] = Float.parseFloat(trimmed);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Invalid vector value at index " + i + ": '" + parts[i] + "'", e);
            }
        }

        if (result.length != VECTOR_DIMENSION) {
            throw new IllegalArgumentException(
                "Vector dimension must be " + VECTOR_DIMENSION + ", got " + result.length);
        }

        return result;
    }

    public static String convertFloatArrayToVectorString(float[] embedding) {
        return new VectorConverter().convertToDatabaseColumn(embedding);
    }

    public static float[] convertVectorStringToFloatArray(String vectorString) {
        return new VectorConverter().convertToEntityAttribute(vectorString);
    }
}
