/*
 * Copyright (c) 2021 ACT Health
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common;

import java.util.ArrayList;
import java.util.List;

// A basic class for the components of email that are supported
public class PegacornEmail {
    
    private String from;
    private List<String> to;
    private List<String> cc;
    private String subject;
    private String content;               //TODO this may be long and could possibly be a stream instead
    private String contentType;
    private List<PegacornEmailAttachment> attachments = new ArrayList<>();
    
    
    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public List<String> getTo() {
        return to;
    }
    public void setTo(List<String> to) {
        this.to = to;
    }
    public List<String> getCc() {
        return cc;
    }
    public void setCc(List<String> cc) {
        this.cc = cc;
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
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public List<PegacornEmailAttachment> getAttachments() {
        return attachments;
    }
    public void setAttachments(List<PegacornEmailAttachment> attachments) {
        this.attachments = attachments;
    }
    
	@Override
	public String toString() {
		return "PegacornEmail [from=" + from + ", to=" + to + ", cc=" + cc + ", subject=" + subject + ", attachments="
				+ attachments + "]";
	}
}
