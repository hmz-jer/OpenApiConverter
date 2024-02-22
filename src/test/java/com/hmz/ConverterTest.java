package com.hmz;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConverterTest {

    @Test
    public void testConstToEnumConversion() {
        Map<String, Object> openApiDocument = new HashMap<>();
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "string");
        schema.put("const", "exampleValue");
        openApiDocument.put("schema", schema);

        ConverterOptions options = new ConverterOptions();
        @SuppressWarnings("unchecked")
        Map<String, Object> clonedOpenApiDocument = (Map<String, Object>) DeepCloneUtility.deepClone(openApiDocument);

        Converter converter = new Converter(clonedOpenApiDocument, options);
        converter.convert(); // Supposons que cette méthode applique toutes les conversions

        Map<String, Object> convertedSchema = (Map<String, Object>) clonedOpenApiDocument.get("schema");
        assertTrue(convertedSchema.containsKey("enum"));
        assertTrue(((List<?>) convertedSchema.get("enum")).contains("exampleValue"));
    }
}
