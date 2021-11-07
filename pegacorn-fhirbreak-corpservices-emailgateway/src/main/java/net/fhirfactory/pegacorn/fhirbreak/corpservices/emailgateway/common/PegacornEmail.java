package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common;

import java.util.List;

// A basic class for the components of email that are supported
public class PegacornEmail {
    
    private String from;
    private String to;
    private String cc;
    private String bcc;
    private String subject;
    private String content;           //TODO this and attachments may be long so probably should be streams instead
    private List<byte[]> attachments;
    
    
    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getTo() {
        return to;
    }
    public void setTo(String to) {
        this.to = to;
    }
    public String getCc() {
        return cc;
    }
    public void setCc(String cc) {
        this.cc = cc;
    }
    public String getBcc() {
        return bcc;
    }
    public void setBcc(String bcc) {
        this.bcc = bcc;
    }
    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public List<byte[]> getAttachments() {
        return attachments;
    }
    public void setAttachments(List<byte[]> attachments) {
        this.attachments = attachments;
    }
    
    
}
