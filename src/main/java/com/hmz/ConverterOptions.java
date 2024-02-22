package com.hmz;

public class ConverterOptions {
    private boolean verbose = false;
    private boolean deleteExampleWithId = false;
    private boolean allOfTransform = false;
    private String authorizationUrl = "";
    private String tokenUrl = "";
    private String scopeDescriptionFile = "";
    private boolean convertSchemaComments = false;

    // Constructeurs
    public ConverterOptions() {
    }

    // Getters et Setters
    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isDeleteExampleWithId() {
        return deleteExampleWithId;
    }

    public void setDeleteExampleWithId(boolean deleteExampleWithId) {
        this.deleteExampleWithId = deleteExampleWithId;
    }

    public boolean isAllOfTransform() {
        return allOfTransform;
    }

    public void setAllOfTransform(boolean allOfTransform) {
        this.allOfTransform = allOfTransform;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getScopeDescriptionFile() {
        return scopeDescriptionFile;
    }

    public void setScopeDescriptionFile(String scopeDescriptionFile) {
        this.scopeDescriptionFile = scopeDescriptionFile;
    }

    public boolean isConvertSchemaComments() {
        return convertSchemaComments;
    }

    public void setConvertSchemaComments(boolean convertSchemaComments) {
        this.convertSchemaComments = convertSchemaComments;
    }
}
