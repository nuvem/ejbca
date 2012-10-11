/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.protocol.ocsp.extension.certhash;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.isismtt.ocsp.CertHash;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.cesecore.certificates.ocsp.extension.OCSPExtension;

/**
 * ASN.1 OCSP extension used to return hash values for certificates as part of an OCSP response.
 * 
 * Certificates will be encoded with SHA256
 * 
 * OID for this extension is 1.3.36.8.3.13
 * 
 * @version $Id$
 *
 */
public class OcspCertHashExtension implements OCSPExtension{

    public static final String CERT_HASH_OID = "1.3.36.8.3.13";

    private static final Logger log = Logger.getLogger(OcspCertHashExtension.class);
    
    @Override
    public void init() {
       //Nothing much to do here.      
    }

    @Override
    public Map<ASN1ObjectIdentifier, Extension> process(X509Certificate[] requestCertificates, String remoteAddress, String remoteHost,
            X509Certificate cert, org.bouncycastle.cert.ocsp.CertificateStatus status) throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e) {
            //This state can't be handled, shouldn't return null 
            log.error("Could not create MessageDigest with algorithm SHA256", e);
            throw new IllegalStateException("Could not create MessageDigest with algorithm SHA256", e);
        }
        CertHash certHash;
        try {
            certHash = new CertHash(new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption), md.digest(cert.getEncoded()));
        } catch (CertificateEncodingException e) {
            //This state can't be handled, shouldn't return null 
            log.error("Could not encode certificate " + cert, e);
            throw new IllegalStateException("Could not encode certificate " + cert, e);
        }
        Hashtable<ASN1ObjectIdentifier, Extension> result = new Hashtable<ASN1ObjectIdentifier, Extension>();
        result.put(new ASN1ObjectIdentifier(CERT_HASH_OID), new Extension(new ASN1ObjectIdentifier(CERT_HASH_OID), false,
                new DEROctetString(certHash)));
        return result;
    }

    @Override
    public int getLastErrorCode() {
        //No error codes defined
        return 0;
    }

}