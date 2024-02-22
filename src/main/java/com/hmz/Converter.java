package com.hmz;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Converter {

    private Map<String, Object> openapi30;
    private ConverterOptions options;

    // Constructeur de Converter
    public Converter(Map<String, Object> openApiDocument, ConverterOptions options) {
        this.openapi30 = openApiDocument;
        this.options = options;
    }

    public void convert() {
        // Initialisation : Préparation du document pour la conversion
        log("Converting from OpenAPI 3.1 to 3.0");

        // 1. Convertir les types nullable
        convertNullableTypeArray(this.openapi30);

        // 2. Supprimer l'objet webhooks, si présent
        removeWebhooksObject(this.openapi30);

        // 3. Supprimer les mots-clés de schéma non supportés
        removeUnsupportedSchemaKeywords(this.openapi30);

        // 4. Renommer ou supprimer $comment selon les cas
        if (this.options.isConvertSchemaComments()) {
            renameSchema$comment(this.openapi30);
        } else {
            deleteSchema$comment(this.openapi30);
        }

        // 5. Convertir les contenus média et encodages spécifiques
        convertJsonSchemaContentMediaType(this.openapi30);

        convertConstToEnum(this.openapi30);

        convertJsonSchemaContentEncoding(this.openapi30);

        // 6. Simplifier les références non-schéma
        simplifyNonSchemaRef(this.openapi30);

        // 7. Supprimer les identifiants de licence
        removeLicenseIdentifier(this.openapi30);

        // D'autres conversions peuvent être ajoutées ici suivant le même modèle

        // Finalisation : Tâches de nettoyage ou de validation post-conversion
        log("Conversion completed.");
    }

    public void convertJsonSchemaExamples(Map<String, Object> schema) {
        schema.forEach((key, value) -> {
            if (value instanceof Map) {
                Map<String, Object> subSchema = (Map<String, Object>) value;
                if ("examples".equals(key)) {
                    List<Object> examples = (List<Object>) subSchema.get(key);
                    if (examples != null && !examples.isEmpty()) {
                        Object firstExample = examples.get(0);
                        if (this.options.isDeleteExampleWithId() && firstExample instanceof Map && ((Map<?, ?>) firstExample).containsKey("id")) {
                            log("Deleted schema example with `id` property: " + firstExample);
                        } else {
                            subSchema.put("example", firstExample);
                            log("Replaces examples with examples[0]. Old examples: " + examples);
                        }
                        subSchema.remove("examples");
                    }
                } else {
                    convertJsonSchemaExamples(subSchema); // Recursive call for nested objects
                }
            } else if (value instanceof List) {
                ((List<?>) value).forEach(item -> {
                    if (item instanceof Map) {
                        convertJsonSchemaExamples((Map<String, Object>) item); // Recursive call for items in arrays
                    }
                });
            }
        });
    }

    // Method to start the conversion process for the entire OpenAPI document
    public void convertDocument(Map<String, Object> openApiDocument) {
        convertJsonSchemaExamples(openApiDocument);
    }

    public void convertConstToEnum(Map<String, Object> schema) {
        schema.forEach((key, value) -> {
            if (value instanceof Map) {
                // Cas récursif: la valeur est un objet JSON, qui pourrait être un schéma.
                Map<String, Object> subSchema = (Map<String, Object>) value;
                convertConstToEnum(subSchema); // Appel récursif pour traiter les sous-schémas.
            }
            // Traitement spécifique pour la clé 'const'.
            if ("const".equals(key)) {
                Object constantValue = schema.get(key);
                schema.remove("const");
                schema.put("enum", Collections.singletonList(constantValue));
                log(String.format("Converted const: %s to enum", constantValue));
            }
        });
    }

    private void convertNullableTypeArray(Map<String, Object> schema) {
        schema.forEach((key, value) -> {
            if ("type".equals(key) && value instanceof List) {
                List<?> types = (List<?>) value;
                if (types.size() == 2 && types.contains("null")) {
                    types.remove("null");
                    Object nonNullType = types.get(0); // Après avoir supprimé "null", il reste un seul type.
                    schema.put("type", nonNullType);
                    schema.put("nullable", true);
                    log("Converted schema type array to nullable: " + nonNullType);
                }
            } else if (value instanceof Map) {
                convertNullableTypeArray((Map<String, Object>) value); // Appel récursif pour les sous-schémas
            }
        });
    }


    private void removeWebhooksObject(Map<String, Object> openapi) {
        if (openapi.containsKey("webhooks")) {
            openapi.remove("webhooks");
            log("Deleted webhooks object");
        }
    }


    private void removeUnsupportedSchemaKeywords(Map<String, Object> schema) {
        final List<String> keywordsToRemove = Arrays.asList("$id", "$schema", "unevaluatedProperties");
        schema.forEach((key, value) -> {
            if (keywordsToRemove.contains(key)) {
                schema.remove(key);
                log("Removed unsupported schema keyword: " + key);
            } else if (value instanceof Map) {
                removeUnsupportedSchemaKeywords((Map<String, Object>) value); // Appel récursif pour les sous-schémas
            }
        });
    }


    private void log(String message) {
        System.out.println(message); // Ou utilisez un logger SLF4J si configuré.
    }


    private void renameSchema$comment(Map<String, Object> schema) {
        schema.forEach((key, value) -> {
            if ("$comment".equals(key)) {
                Object comment = schema.remove("$comment");
                schema.put("x-comment", comment);
                // Log: "schema $comment renamed to x-comment"
            } else if (value instanceof Map) {
                // Appel récursif pour traiter les sous-schémas
                renameSchema$comment((Map<String, Object>) value);
            } else if (value instanceof List) {
                // Traitement des listes pour les schémas potentiellement imbriqués dans des listes
                ((List<?>) value).forEach(item -> {
                    if (item instanceof Map) {
                        renameSchema$comment((Map<String, Object>) item);
                    }
                });
            }
        });
    }


    private void deleteSchema$comment(Map<String, Object> schema) {
        schema.forEach((key, value) -> {
            if ("$comment".equals(key)) {
                schema.remove("$comment");
                // Log: "schema $comment deleted"
            } else if (value instanceof Map) {
                // Appel récursif pour les sous-schémas
                deleteSchema$comment((Map<String, Object>) value);
            } else if (value instanceof List) {
                // Traitement des listes pour les schémas potentiellement imbriqués dans des listes
                ((List<?>) value).forEach(item -> {
                    if (item instanceof Map) {
                        deleteSchema$comment((Map<String, Object>) item);
                    }
                });
            }
        });
    }


    private void convertJsonSchemaContentMediaType(Map<String, Object> schema) {
        schema.forEach((key, value) -> {
            // Traitement récursif pour les sous-objets et les listes.
            if (value instanceof Map) {
                convertJsonSchemaContentMediaType((Map<String, Object>) value);
            } else if (value instanceof List) {
                ((List<?>) value).forEach(item -> {
                    if (item instanceof Map) {
                        convertJsonSchemaContentMediaType((Map<String, Object>) item);
                    }
                });
            }

            // Logique de conversion spécifique.
            if ("type".equals(key) && "string".equals(schema.get(key)) && schema.containsKey("contentMediaType")) {
                String contentMediaType = (String) schema.get("contentMediaType");
                if ("application/octet-stream".equals(contentMediaType)) {
                    if (schema.containsKey("format")) {
                        String format = (String) schema.get("format");
                        if (!"binary".equals(format)) {
                            // Log error: schema already has a non-binary format.
                            return;
                        }
                        // Si le format est déjà 'binary', on supprime simplement 'contentMediaType'.
                        schema.remove("contentMediaType");
                    } else {
                        schema.remove("contentMediaType");
                        schema.put("format", "binary");
                        // Log: Converted schema contentMediaType to format: binary
                    }
                }
            }
        });
    }


    private void convertJsonSchemaContentEncoding(Map<String, Object> schema) {
        schema.forEach((key, value) -> {
            // Appel récursif pour les sous-schémas et les listes.
            if (value instanceof Map) {
                convertJsonSchemaContentEncoding((Map<String, Object>) value);
            } else if (value instanceof List) {
                ((List<?>) value).forEach(item -> {
                    if (item instanceof Map) {
                        convertJsonSchemaContentEncoding((Map<String, Object>) item);
                    }
                });
            }

            // Logique de conversion spécifique pour 'contentEncoding'.
            if ("type".equals(key) && "string".equals(schema.get(key)) && schema.containsKey("contentEncoding")) {
                String contentEncoding = (String) schema.get("contentEncoding");
                if ("base64".equals(contentEncoding)) {
                    if (schema.containsKey("format")) {
                        String format = (String) schema.get("format");
                        if (!"byte".equals(format)) {
                            // Log error: Non-byte format already present, unable to convert.
                            return;
                        }
                        // Si le format est déjà 'byte', on supprime simplement 'contentEncoding'.
                        schema.remove("contentEncoding");
                    } else {
                        // Aucun format préalable, conversion directe.
                        schema.remove("contentEncoding");
                        schema.put("format", "byte");
                        // Log: Converted 'contentEncoding: base64' to 'format: byte'.
                    }
                } else {
                    // Log error: Unsupported contentEncoding value.
                }
            }
        });
    }


    private void simplifyNonSchemaRef(Map<String, Object> schema) {
        schema.forEach((key, value) -> {
            if (value instanceof Map) {
                Map<String, Object> node = (Map<String, Object>) value;
                if (node.containsKey("$ref") && node.size() > 1) {
                    // Log: Down convert reference object to JSON Reference
                    node.keySet().removeIf(k -> !"$ref".equals(k));
                }
                simplifyNonSchemaRef(node); // Appel récursif pour les sous-objets
            } else if (value instanceof List) {
                ((List<?>) value).forEach(item -> {
                    if (item instanceof Map) {
                        simplifyNonSchemaRef((Map<String, Object>) item); // Appel récursif pour les éléments de la liste
                    }
                });
            }
        });
    }


    private void removeLicenseIdentifier(Map<String, Object> openapi) {
        if (openapi.containsKey("info")) {
            Map<String, Object> info = (Map<String, Object>) openapi.get("info");
            if (info != null && info.containsKey("license")) {
                Map<String, Object> license = (Map<String, Object>) info.get("license");
                if (license != null && license.containsKey("identifier")) {
                    // Log: Removed info.license.identifier
                    license.remove("identifier");
                }
            }
        }
    }

    public void convertAndSave(String outputFileName) {
        try {
            ObjectMapper objectMapper;
            String textOutput;
            // Détermination du format basé sur le nom du fichier de sortie
            if (outputFileName != null && !outputFileName.isEmpty()) {
                if (outputFileName.endsWith(".json")) {
                    objectMapper = new ObjectMapper();
                    textOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.openapi30);
                } else { // YAML par défaut
                    objectMapper = new ObjectMapper(new YAMLFactory());
                    textOutput = objectMapper.writeValueAsString(this.openapi30);
                }
                // Assurer que le dossier de sortie existe
                Files.createDirectories(Paths.get(outputFileName).getParent());

                // Écriture dans le fichier
                Files.write(Paths.get(outputFileName), textOutput.getBytes());
            } else {
                // Si aucun nom de fichier de sortie n'est fourni, imprimer dans la console
                if (outputFileName.endsWith(".json")) {
                    objectMapper = new ObjectMapper();
                    textOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.openapi30);
                } else { // YAML par défaut
                    objectMapper = new ObjectMapper(new YAMLFactory());
                    textOutput = objectMapper.writeValueAsString(this.openapi30);
                }
                System.out.println(textOutput);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
