package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common;


// basic attachment with data base64 encoded inline
//TODO extend for url reference based attachments
public class PegacornEmailAttachment {
    
    private String name;
    private String contentType;
    private String data;
    private Long size;
    private String hash;
    private String creationTime;
        
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    /**
     * @return base64 coded representation of the attachment content
     */
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    /**
     * @return The base64 representation of the SHA-1 hash for the attachment content
     */
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }
}
