package net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.common;

// minimum needed fields for sending an SMS message
public class SMSMessageBase {

    private String phoneNumber;
    private String message;
    
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "SMSMessageBase ["
                + "phoneNumber=" + getPhoneNumber() != null ? getPhoneNumber() : "" + ", "
                + "message=" + getMessage() != null ? getMessage() : ""
                + "]";
    }
}
