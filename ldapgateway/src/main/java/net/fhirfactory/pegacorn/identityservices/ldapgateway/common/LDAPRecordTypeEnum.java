package net.fhirfactory.pegacorn.identityservices.ldapgateway.common;

public enum LDAPRecordTypeEnum {
    LDAP_RECORD_USER_RECORD("ldap-record-user"),
    LDAP_RECORD_ORGANIZATION_RECORD("ldap-record-user");

    private String recordType;
    private LDAPRecordTypeEnum(String code){ this.recordType = code;}

    public String getRecordType(){return(this.recordType);}
}
