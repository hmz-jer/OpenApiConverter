package com.hmz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@SpringBootApplication
public class OpenApiConverterApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(OpenApiConverterApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -jar votreapplication.jar <path to OpenAPI document>");
            return;
        }

        String inputFilePath = args[0];
        Map<String, Object> openApiDocument = loadOpenApiDocument(inputFilePath);

        if (openApiDocument == null) {
            System.out.println("Failed to load the OpenAPI document.");
            return;
        }

        String outputFileName = generateOutputFileName(inputFilePath);

        ConverterOptions options = new ConverterOptions();
        // Configurer les options de conversion si nécessaire


        @SuppressWarnings("unchecked")
        Map<String, Object> clonedOpenApiDocument = (Map<String, Object>) DeepCloneUtility.deepClone(openApiDocument);

        Converter converter = new Converter(clonedOpenApiDocument, options);

        // Exécuter la conversion et sauvegarder le résultat
        converter.convertAndSave(outputFileName);

        System.out.println("Conversion completed. Output saved to: " + outputFileName);
    }

    private static Map<String, Object> loadOpenApiDocument(String filePath) {
        ObjectMapper objectMapper;

        // Choisir l'ObjectMapper en fonction de l'extension du fichier
        if (filePath.endsWith(".yaml") || filePath.endsWith(".yml")) {
            objectMapper = new ObjectMapper(new YAMLFactory()); // Pour les fichiers YAML
        } else if (filePath.endsWith(".json")) {
            objectMapper = new ObjectMapper(); // Pour les fichiers JSON
        } else {
            System.out.println("Unsupported file format. Please provide a .json or .yaml file.");
            return null;
        }

        try {
            return objectMapper.readValue(new File(filePath), Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String generateOutputFileName(String inputFilePath) {
        Path path = Paths.get(inputFilePath);
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
        String extension = dotIndex == -1 ? "" : fileName.substring(dotIndex);

        return path.getParent().resolve(baseName + "Converted" + extension).toString();
    }
}