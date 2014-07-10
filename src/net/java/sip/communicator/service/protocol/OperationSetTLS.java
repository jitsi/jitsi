/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.security.cert.*;

/**
 * An <tt>OperationSet</tt> that allows access to information about TLS used by 
 * the protocol provider.
 *
 * @author Markus Kilas
 */
public interface OperationSetTLS
    extends OperationSet
{
   /**
    * Returns the negotiated cipher suite
    *
    * @return The cipher suite name used for instance
    * "TLS_RSA_WITH_AES_256_CBC_SHA" or null if TLS is not used.
    */
    String getCipherSuite();

   /**
    * Returns the negotiated SSL/TLS protocol.
    *
    * @return The protocol name used for instance "TLSv1".
    */
    String getProtocol();

   /**
    * Returns the TLS server certificate chain with the end entity certificate
    * in the first position and the issuers following (if any returned by the
    * server).
    *
    * @return The TLS server certificate chain.
    */
    Certificate[] getServerCertificates();
}
