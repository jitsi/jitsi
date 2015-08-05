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
package net.java.sip.communicator.impl.protocol.sip.net;

import java.security.cert.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.sip.address.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.util.*;

/**
 * Matcher that extracts certificate identities according to <a
 * href="http://tools.ietf.org/html/rfc5922#section-7.1">RFC5922, Section
 * 7.1</a> and compares them with the rules from Section 7.2 and 7.3.
 * @see #PNAME_STRICT_RFC5922 for wildcard handling; the default is false
 *
 * @author Ingo Bauersachs
 */
public class RFC5922Matcher
    implements CertificateMatcher
{
    /**
     * When set to true, enables strict validation of the hostname according to
     * <a href="http://tools.ietf.org/html/rfc5922#section-7.2">RFC5922 Section
     * 7.2</a>
     */
    public final static String PNAME_STRICT_RFC5922 =
        "net.java.sip.communicator.sip.tls.STRICT_RFC5922";

    private ProtocolProviderServiceSipImpl provider;

    /**
     * Creates a new instance of this class.
     * @param provider The SIP Provider to which this matcher belongs.
     */
    public RFC5922Matcher(ProtocolProviderServiceSipImpl provider)
    {
        this.provider = provider;
    }

    /** Our class logger. */
    private static final Logger logger = Logger
        .getLogger(CertificateMatcher.class);

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.certificate.CertificateMatcher#verify
     * (java.lang.Iterable, java.security.cert.X509Certificate)
     */
    public void verify(Iterable<String> identitiesToTest, X509Certificate cert)
        throws CertificateException
    {
        boolean strict = SipActivator.getConfigurationService()
            .getBoolean(PNAME_STRICT_RFC5922, false);

        // if any of the identities is contained in the certificate we're good
        boolean oneMatched = false;
        Iterable<String> certIdentities = extractCertIdentities(cert);
        for (String identity : identitiesToTest)
        {
            // check if the intended hostname is contained in one of the
            // hostnames of the certificate according to
            // http://tools.ietf.org/html/rfc5922#section-7.2
            for(String dnsName : certIdentities)
            {
                try
                {
                    if(NetworkUtils.compareDnsNames(dnsName, identity) == 0)
                    {
                        // one of the hostnames matched, we're good to go
                        return;
                    }

                    if(!strict
                        // is a wildcard name
                        && dnsName.startsWith("*.")
                        // contains at least two dots (*.example.com)
                        && identity.indexOf(".") < identity.lastIndexOf(".")
                        // compare *.example.com stripped to example.com with
                        // - foo.example.com stripped to example.com
                        // - foo.bar.example.com to bar.example.com
                        && NetworkUtils.compareDnsNames(
                            dnsName.substring(2),
                            identity.substring(identity.indexOf(".")+1)) == 0)
                    {
                        // the wildcard matched, we're good to go
                        return;
                    }
                }
                catch (ParseException e)
                {} // we don't care - this hostname did not match
            }
        }
        if (!oneMatched)
            throw new CertificateException("None of <" + identitiesToTest
                + "> matched by the rules of RFC5922 to the cert with CN="
                + cert.getSubjectDN());
    }

    private Iterable<String> extractCertIdentities(X509Certificate cert)
    {
        List<String> certIdentities = new ArrayList<String>();
        Collection<List<?>> subjAltNames = null;
        try
        {
            subjAltNames = cert.getSubjectAlternativeNames();
        }
        catch (CertificateParsingException ex)
        {
            logger.error("Error parsing TLS certificate", ex);
        }
        // subjAltName types are defined in rfc2459
        final Integer dnsNameType = 2;
        final Integer uriNameType = 6;
        if (subjAltNames != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("found subjAltNames: " + subjAltNames);

            // First look for a URI in the subjectAltName field
            for (List<?> altName : subjAltNames)
            {
                // 0th position is the alt name type
                // 1st position is the alt name data
                if (altName.get(0).equals(uriNameType))
                {
                    SipURI altNameUri;
                    try
                    {
                        altNameUri =
                            provider.getAddressFactory().createSipURI(
                                (String) altName.get(1));
                        // only sip URIs are allowed
                        if (!"sip".equals(altNameUri.getScheme()))
                            continue;
                        // user certificates are not allowed
                        if (altNameUri.getUser() != null)
                            continue;
                        String altHostName = altNameUri.getHost();
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("found uri " + altName.get(1)
                                + ", hostName " + altHostName);
                        }
                        certIdentities.add(altHostName);
                    }
                    catch (ParseException e)
                    {
                        logger.error("certificate contains invalid uri: "
                            + altName.get(1));
                    }
                }

            }
            // DNS An implementation MUST accept a domain name system
            // identifier as a SIP domain identity if and only if no other
            // identity is found that matches the "sip" URI type described
            // above.
            if (certIdentities.isEmpty())
            {
                for (List<?> altName : subjAltNames)
                {
                    if (altName.get(0).equals(dnsNameType))
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("found dns " + altName.get(1));
                        certIdentities.add(altName.get(1).toString());
                    }
                }
            }
        }
        else
        {
            // If and only if the subjectAltName does not appear in the
            // certificate, the implementation MAY examine the CN field of the
            // certificate. If a valid DNS name is found there, the
            // implementation MAY accept this value as a SIP domain identity.
            String dname = cert.getSubjectDN().getName();
            String cname = "";
            try
            {
                Pattern EXTRACT_CN =
                    Pattern.compile(".*CN\\s*=\\s*([\\w*\\.]+).*");
                Matcher matcher = EXTRACT_CN.matcher(dname);
                if (matcher.matches())
                {
                    cname = matcher.group(1);
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("found CN: " + cname + " from DN: "
                            + dname);
                    }
                    certIdentities.add(cname);
                }
            }
            catch (Exception ex)
            {
                logger.error("exception while extracting CN", ex);
            }
        }
        return certIdentities;
    }
}
