package com.legally.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "legally")
public class LegallyProperties {

    private Gemini gemini = new Gemini();
    private Firebase firebase = new Firebase();
    private Database database = new Database();
    private Cors cors = new Cors();
    private Upload upload = new Upload();
    private Llm llm = new Llm();
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

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
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

    public static class Llm {
        private String providerOrder = "gemini,groq,openrouter,mistral,cloudflare,huggingface";
        private LlmEndpoint groq = new LlmEndpoint(
                "https://api.groq.com/openai/v1", "", "llama-3.3-70b-versatile");
        private LlmEndpoint openrouter = new LlmEndpoint(
                "https://openrouter.ai/api/v1", "", "meta-llama/llama-3.3-70b-instruct:free");
        private LlmEndpoint mistral = new LlmEndpoint(
                "https://api.mistral.ai/v1", "", "mistral-small-latest");
        private LlmEndpoint cloudflare = new LlmEndpoint(
                "https://api.cloudflare.com/client/v4/accounts", "", "@cf/meta/llama-3.1-8b-instruct");
        private LlmEndpoint huggingface = new LlmEndpoint(
                "https://router.huggingface.co/v1", "", "meta-llama/Meta-Llama-3-8B-Instruct");

        public String getProviderOrder() {
            return providerOrder;
        }

        public void setProviderOrder(String providerOrder) {
            this.providerOrder = providerOrder;
        }

        public LlmEndpoint getGroq() {
            return groq;
        }

        public void setGroq(LlmEndpoint groq) {
            this.groq = groq;
        }

        public LlmEndpoint getOpenrouter() {
            return openrouter;
        }

        public void setOpenrouter(LlmEndpoint openrouter) {
            this.openrouter = openrouter;
        }

        public LlmEndpoint getMistral() {
            return mistral;
        }

        public void setMistral(LlmEndpoint mistral) {
            this.mistral = mistral;
        }

        public LlmEndpoint getCloudflare() {
            return cloudflare;
        }

        public void setCloudflare(LlmEndpoint cloudflare) {
            this.cloudflare = cloudflare;
        }

        public LlmEndpoint getHuggingface() {
            return huggingface;
        }

        public void setHuggingface(LlmEndpoint huggingface) {
            this.huggingface = huggingface;
        }
    }

    public static class LlmEndpoint {
        private String baseUrl;
        private String apiKey = "";
        private String model = "";
        private String accountId = "";

        public LlmEndpoint() {
        }

        public LlmEndpoint(String baseUrl, String apiKey, String model) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

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

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank() && model != null && !model.isBlank();
        }
    }
}
