package com.legally.model.dto;

/**
 * POST /api/uploads response; reference the client sends in {@link ConsultRequest.MediaRef}.
 */
public class UploadResponse {

    /** Public URL or firebase:// path for later byte reads. */
    private String url;
    private String mimeType;

    /** {@code firebase} or {@code local}. */
    private String storageType;
    private String fileName;

    public UploadResponse() {
    }

    public UploadResponse(String url, String mimeType, String storageType, String fileName) {
        this.url = url;
        this.mimeType = mimeType;
        this.storageType = storageType;
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
