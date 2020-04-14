/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
