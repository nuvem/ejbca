/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ra;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.cesecore.certificates.crl.RevokedCertInfo;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityInformation;
import org.cesecore.certificates.endentity.ExtendedInformation;
import org.cesecore.util.ValidityDate;
import org.ejbca.core.model.ra.ExtendedInformationFields;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;

/** 
 * UI representation of a result set item from the back end.
 * 
 * Bravely ignoring hard token issuer and printing of user data fields.
 * 
 * @version $Id$
 */
public class RaEndEntityDetails {

    public interface Callbacks {
        RaLocaleBean getRaLocaleBean();
        EndEntityProfile getEndEntityProfile(final int eepId);
    }

    private static final Logger log = Logger.getLogger(RaEndEntityDetails.class);
    private final Callbacks callbacks;

    private final String username;
    private final EndEntityInformation endEntityInformation;
    private final ExtendedInformation extendedInformation;
    private final String subjectDn;
    private final String subjectAn;
    private final String subjectDa;
    private final int eepId;
    private final String eepName;
    private final int cpId;
    private final String cpName;
    private final String caName;
    private final String created;
    private final String modified;
    private final int status;
    
    private EndEntityProfile endEntityProfile = null;
    private SubjectDn subjectDistinguishedName = null;
    private SubjectAlternativeName subjectAlternativeName = null;
    private SubjectDirectoryAttributes subjectDirectoryAttributes = null;

    private int styleRowCallCounter = 0;

    private RaEndEntityDetails next = null;
    private RaEndEntityDetails previous = null;

    public RaEndEntityDetails(final EndEntityInformation endEntity, final Callbacks callbacks,
            final Map<Integer, String> cpIdToNameMap, final Map<Integer, String> eepIdToNameMap, final Map<Integer,String> caIdToNameMap) {
        this(endEntity, callbacks, cpIdToNameMap.get(Integer.valueOf(endEntity.getCertificateProfileId())),
                String.valueOf(eepIdToNameMap.get(Integer.valueOf(endEntity.getEndEntityProfileId()))),
                String.valueOf(caIdToNameMap.get(Integer.valueOf(endEntity.getCAId()))));
    }
    
    public RaEndEntityDetails(final EndEntityInformation endEntity, final Callbacks callbacks,
            final String certProfName, final String eeProfName, final String caName) {
        this.endEntityInformation = endEntity;
        final ExtendedInformation extendedInformation = endEntity.getExtendedinformation();
        this.extendedInformation = extendedInformation==null ? new ExtendedInformation() : extendedInformation;
        this.callbacks = callbacks;
        this.username = endEntity.getUsername();
        this.subjectDn = endEntity.getDN();
        this.subjectAn = endEntity.getSubjectAltName();
        this.subjectDa = this.extendedInformation.getSubjectDirectoryAttributes();
        this.cpId = endEntity.getCertificateProfileId();
        this.cpName = certProfName;
        this.eepId = endEntity.getEndEntityProfileId();
        this.eepName = eeProfName;
        this.caName = caName;
        final Date timeCreated = endEntity.getTimeCreated();
        if(timeCreated != null) {
            this.created = ValidityDate.formatAsISO8601ServerTZ(timeCreated.getTime(), TimeZone.getDefault());
        } else {
            this.created = "";
        }
        final Date timeModified = endEntity.getTimeModified();
        if(timeModified != null) {
            this.modified = ValidityDate.formatAsISO8601ServerTZ(timeModified.getTime(), TimeZone.getDefault());    
        } else {
            this.modified = "";
        }
        
        this.status = endEntity.getStatus();
    }
    public EndEntityInformation getEndEntityInformation() { return endEntityInformation; } 
    public String getUsername() { return username; }
    public String getSubjectDn() { return subjectDn; }
    public String getSubjectAn() { return subjectAn; }
    public String getSubjectDa() { return subjectDa; }
    public String getCaName() { return caName; }
    public String getCpName() {
        if (cpId==EndEntityInformation.NO_CERTIFICATEPROFILE) {
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_info_unknowncp");
        } else if (cpName!=null) {
            return cpName;
        }
        return callbacks.getRaLocaleBean().getMessage("component_eedetails_info_missingcp", cpId);
    }
    public boolean isCpNameSameAsEepName() { return getEepName().equals(getCpName()); }
    public String getEepName() {
        if (eepId==EndEntityInformation.NO_ENDENTITYPROFILE) {
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_info_unknowneep", eepId);
        } else if (eepName!=null) {
            return eepName;
        }
        return callbacks.getRaLocaleBean().getMessage("component_eedetails_info_missingeep", eepId);
    }
    public String getCreated() { return created; }
    public String getModified() { return modified; }
    public String getStatus() {
        switch (status) {
        case EndEntityConstants.STATUS_FAILED:
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_status_failed");
        case EndEntityConstants.STATUS_GENERATED:
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_status_generated");
        case EndEntityConstants.STATUS_KEYRECOVERY:
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_status_keyrecovery");
        case EndEntityConstants.STATUS_NEW:
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_status_new");
        case EndEntityConstants.STATUS_REVOKED:
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_status_revoked");
        }
        return callbacks.getRaLocaleBean().getMessage("component_eedetails_status_other");
    }

    public String getTokenType() {
        switch (endEntityInformation.getTokenType()) {
        case EndEntityConstants.TOKEN_USERGEN:
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_tokentype_usergen");
        case EndEntityConstants.TOKEN_SOFT_JKS:
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_tokentype_jks");
        case EndEntityConstants.TOKEN_SOFT_P12:
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_tokentype_pkcs12");
        case EndEntityConstants.TOKEN_SOFT_PEM:
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_tokentype_pem");
        }
        return "?";
    }

    public boolean isTokenTypeUserGenerated() {
        return endEntityInformation.getTokenType()==EndEntityConstants.TOKEN_USERGEN;
    }
    
    /** Returns the specified key type for this end entity (e.g. "RSA 2048"), or null if none is specified (e.g. if created from the Admin GUI) */
    public String getKeyType() {
        if (extendedInformation != null && extendedInformation.getKeyStoreAlgorithmType() != null) {
            String keyTypeString = extendedInformation.getKeyStoreAlgorithmType();
            if (extendedInformation.getKeyStoreAlgorithmSubType() != null) {
                keyTypeString += " " + extendedInformation.getKeyStoreAlgorithmSubType();
            }
            return keyTypeString;
        }
        return null; // null = hidden in UI
    }

    public boolean isKeyRecoverable() {
        return endEntityInformation.getKeyRecoverable();
    }

    public boolean isEmailEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.EMAIL,0);
    }
    public String getEmail() {
        return endEntityInformation.getEmail();
    }

    public boolean isLoginsMaxEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.MAXFAILEDLOGINS, 0);
    }
    public String getLoginsMax() {
        return Integer.toString(extendedInformation.getMaxLoginAttempts());
    }
    public String getLoginsRemaining() {
        return Integer.toString(extendedInformation.getRemainingLoginAttempts());
    }
    
    public boolean isSendNotificationEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.SENDNOTIFICATION, 0);
    }
    public boolean isSendNotification() {
        return endEntityInformation.getSendNotification();
    }
    
    public boolean isCertificateSerialNumberOverrideEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.CERTSERIALNR, 0);
    }
    public String getCertificateSerialNumberOverride() {
        final BigInteger certificateSerialNumber = extendedInformation.certificateSerialNumber();
        if (certificateSerialNumber!=null) {
            return certificateSerialNumber.toString(16);
        }
        return "";
    }

    public boolean isOverrideNotBeforeEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.STARTTIME, 0);
    }
    public String getOverrideNotBefore() {
        return extendedInformation.getCustomData(ExtendedInformation.CUSTOM_STARTTIME);
    }
    public boolean isOverrideNotAfterEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.ENDTIME, 0);
    }
    public String getOverrideNotAfter() {
        return extendedInformation.getCustomData(ExtendedInformation.CUSTOM_ENDTIME);
    }

    public boolean isCardNumberEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.CARDNUMBER, 0);
    }
    public String getCardNumber() {
        return endEntityInformation.getCardNumber();
    }

    public boolean isNameConstraintsPermittedEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.NAMECONSTRAINTS_PERMITTED, 0);
    }
    public String getNameConstraintsPermitted() {
        final List<String> value = extendedInformation.getNameConstraintsPermitted();
        if (value!=null) {
            return Arrays.toString(extendedInformation.getNameConstraintsPermitted().toArray());
        }
        return "";
    }
    public boolean isNameConstraintsExcludedEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.NAMECONSTRAINTS_EXCLUDED, 0);
    }
    public String getNameConstraintsExcluded() {
        final List<String> value = extendedInformation.getNameConstraintsExcluded();
        if (value!=null) {
            return Arrays.toString(extendedInformation.getNameConstraintsExcluded().toArray());
        }
        return "";
    }

    public boolean isAllowedRequestsEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.ALLOWEDREQUESTS, 0);
    }
    public String getAllowedRequests() {
        final String value = endEntityProfile.getValue(EndEntityProfile.ALLOWEDREQUESTS, 0);
        return value==null ? "1" : value;
    }
    public String getAllowedRequestsUsed() {
        final String value = extendedInformation.getCustomData(ExtendedInformationFields.CUSTOM_REQUESTCOUNTER);
        return value==null ? "0" : value;
    }

    public boolean isIssuanceRevocationReasonEnabled() {
        return getEndEntityProfile().getUse(EndEntityProfile.ISSUANCEREVOCATIONREASON, 0);
    }
    public String getIssuanceRevocationReason() {
        final String reasonCode = extendedInformation.getCustomData(ExtendedInformation.CUSTOM_REVOCATIONREASON);
        if (reasonCode!=null) {
            return callbacks.getRaLocaleBean().getMessage("component_eedetails_field_issuancerevocation_reason_"+reasonCode);
        }
        return callbacks.getRaLocaleBean().getMessage("component_eedetails_field_issuancerevocation_reason_" + RevokedCertInfo.NOT_REVOKED);
    }

    public SubjectDn getSubjectDistinguishedName() {
        if (subjectDistinguishedName==null) {
            this.subjectDistinguishedName = new SubjectDn(getEndEntityProfile(), subjectDn);
        }
        return subjectDistinguishedName;
    }

    public SubjectAlternativeName getSubjectAlternativeName() {
        if (subjectAlternativeName==null) {
            this.subjectAlternativeName = new SubjectAlternativeName(getEndEntityProfile(), subjectAn);
        }
        return subjectAlternativeName;
        
    }

    public SubjectDirectoryAttributes getSubjectDirectoryAttributes() {
        if (subjectDirectoryAttributes==null) {
            this.subjectDirectoryAttributes = new SubjectDirectoryAttributes(getEndEntityProfile(), subjectDa);
        }
        return subjectDirectoryAttributes;
        
    }

    private EndEntityProfile getEndEntityProfile() {
        if (endEntityProfile==null) {
            endEntityProfile = callbacks.getEndEntityProfile(eepId);
        }
        return endEntityProfile;
    }
    
    /**
     * Returns the add approval request ID stored in the extended information
     * @return the ID of the approval request that was submitted to create the end entity
     */
    public String getAddEndEntityApprovalRequestId() {
        String ret = "";
        final ExtendedInformation ext = endEntityInformation.getExtendedinformation();
        if(ext != null) {
            final Integer reqid = ext.getAddEndEntityApprovalRequestId();
            if(reqid != null) {
                ret = reqid.toString();
            }
        }
        return ret;
    }
    
    /**
     * Returns the edit approval request IDs stored in the extended information as one String separated by ';'     
     * @return the IDs of the approval request that were submitted to edit the end entity 
     */
    public String getEditEndEntityApprovalRequestIds() {
        StringBuilder ret = new StringBuilder("");
        final ExtendedInformation ext = endEntityInformation.getExtendedinformation();
        if(ext != null) {
            final List<Integer> ids = ext.getEditEndEntityApprovalRequestIds();
            if(!ids.isEmpty()) {
                for(Integer id : ids) {
                    ret = ret.append("; ").append(id);
                }
                ret.delete(0, 2);
            }
        }
        return ret.toString();
    }
    
    /**
     * Returns the revocation approval request IDs stored in the extended information as one String separated by ';'     
     * @return the IDs of the approval request that were submitted to revoke the end entity 
     */
    public String getRevokeEndEntityApprovalRequestIds() {
        StringBuilder ret = new StringBuilder("");
        final ExtendedInformation ext = endEntityInformation.getExtendedinformation();
        if(ext != null) {
            final List<Integer> ids = ext.getRevokeEndEntityApprovalRequestIds();
            if(!ids.isEmpty()) {
                for(Integer id : ids) {
                    ret = ret.append("; ").append(id);
                }
                ret.delete(0, 2);
            }
        }
        return ret.toString();
    } 
    
    /** @return true every twice starting with every forth call */
    public boolean isEven() {
        styleRowCallCounter++;
        return (styleRowCallCounter+1) / 2 % 2 == 0;
    }
    /** @return true every twice starting with every other call */
    public boolean isEvenTwice() {
        isEven();
        return isEven();
    }

    public RaEndEntityDetails getNext() { return next; }
    public void setNext(RaEndEntityDetails next) { this.next = next; }

    public RaEndEntityDetails getPrevious() { return previous; }
    public void setPrevious(RaEndEntityDetails previous) { this.previous = previous; }
}
