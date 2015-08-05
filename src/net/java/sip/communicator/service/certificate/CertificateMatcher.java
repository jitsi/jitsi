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
package net.java.sip.communicator.service.certificate;

import java.security.cert.*;

/**
 * Interface to verify X.509 certificate
 */
public interface CertificateMatcher
{
    /**
     * Implementations check whether one of the supplied identities is
     * contained in the certificate.
     *
     * @param identitiesToTest The that are compared against the certificate.
     * @param cert The X.509 certificate that was supplied by the server or
     *            client.
     * @throws CertificateException When any certificate parsing fails.
     */
    public void verify(Iterable<String> identitiesToTest, X509Certificate cert)
        throws CertificateException;
}
