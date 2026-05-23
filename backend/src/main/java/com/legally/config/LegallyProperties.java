package com.legally.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "legally")
public class LegallyProperties {

    private Gemini gemini = new Gemini();
    private Firebase firebase = new Firebase();
    private Database database = new Database();
    private Cors cors = new Cors();
    private Upload upload = new Upload();

    public Gemini getGemini() {
        return gemini;
    }

    public void setGemini(Gemini gemini) {
        this.gemini = gemini;
    }

    public Firebase getFirebase() {
        return firebase;
    }

    public void setFirebase(Firebase firebase) {
        this.firebase = firebase;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-2.5-flash";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Database {
        private String mode = "local";
        private String name = "legally";
        private String cloudSqlInstanceConnectionName = "";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCloudSqlInstanceConnectionName() {
            return cloudSqlInstanceConnectionName;
        }

        public void setCloudSqlInstanceConnectionName(String cloudSqlInstanceConnectionName) {
            this.cloudSqlInstanceConnectionName = cloudSqlInstanceConnectionName;
        }

        public boolean isCloudSqlMode() {
            return "cloud-sql".equalsIgnoreCase(mode);
        }
    }

    public static class Firebase {
        private boolean enabled = false;
        private String credentialsPath = "";
        private String projectId = "";
        private String storageBucket = "";
        private boolean requireAuth = false;
        private boolean anonymousOnly = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCredentialsPath() {
            return credentialsPath;
        }

        public void setCredentialsPath(String credentialsPath) {
            this.credentialsPath = credentialsPath;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getStorageBucket() {
            return storageBucket;
        }

        public void setStorageBucket(String storageBucket) {
            this.storageBucket = storageBucket;
        }

        public boolean isRequireAuth() {
            return requireAuth;
        }

        public void setRequireAuth(boolean requireAuth) {
            this.requireAuth = requireAuth;
        }

        public boolean isAnonymousOnly() {
            return anonymousOnly;
        }

        public void setAnonymousOnly(boolean anonymousOnly) {
            this.anonymousOnly = anonymousOnly;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Upload {
        private String localDir = "uploads";

        public String getLocalDir() {
            return localDir;
        }

        public void setLocalDir(String localDir) {
            this.localDir = localDir;
        }
    }
}
