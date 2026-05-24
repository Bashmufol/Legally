package com.legally.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "legally")
public class LegallyProperties {

    private Gemini gemini = new Gemini();
    private Firebase firebase = new Firebase();
    private Database database = new Database();
    private Cors cors = new Cors();
    private Upload upload = new Upload();
    private SerpApi serpApi = new SerpApi();
    private Session session = new Session();

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

    public SerpApi getSerpApi() {
        return serpApi;
    }

    public void setSerpApi(SerpApi serpApi) {
        this.serpApi = serpApi;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public static class Session {
        private int ttlHours = 72;
        private String cleanupCron = "0 0 * * * *";

        public int getTtlHours() {
            return ttlHours;
        }

        public void setTtlHours(int ttlHours) {
            this.ttlHours = ttlHours;
        }

        public String getCleanupCron() {
            return cleanupCron;
        }

        public void setCleanupCron(String cleanupCron) {
            this.cleanupCron = cleanupCron;
        }
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

    public static class SerpApi {
        private boolean enabled = true;
        private String apiKey = "";
        private int maxResults = 8;
        private int maxPagesToFetch = 3;
        private int maxExcerptChars = 6000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public int getMaxPagesToFetch() {
            return maxPagesToFetch;
        }

        public void setMaxPagesToFetch(int maxPagesToFetch) {
            this.maxPagesToFetch = maxPagesToFetch;
        }

        public int getMaxExcerptChars() {
            return maxExcerptChars;
        }

        public void setMaxExcerptChars(int maxExcerptChars) {
            this.maxExcerptChars = maxExcerptChars;
        }

        public boolean isConfigured() {
            return enabled && apiKey != null && !apiKey.isBlank();
        }
    }
}
