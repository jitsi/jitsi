/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;
import org.jivesoftware.smack.*;

import javax.net.ssl.*;
import java.security.*;

/**
 * Implements anonymous login strategy for the purpose of some server side
 * technologies. This makes not much sense to be used with Jitsi directly.
 *
 * @see JabberAccountIDImpl#ANONYMOUS_AUTH
 *
 * @author Pawel Domas
 */
public class AnonymousLoginStrategy
    implements JabberLoginStrategy
{
    /**
     * <tt>UserCredentials</tt> used by accompanying services.
     */
    private final UserCredentials credentials;

    /**
     * Creates new anonymous login strategy instance.
     * @param login user login only for the purpose of returning
     *              <tt>UserCredentials</tt> that are used by accompanying
     *              services.
     */
    public AnonymousLoginStrategy(String login)
    {
        this.credentials = new UserCredentials();

        credentials.setUserName(login);

        //FIXME: consider including password for TURN authentication ?
        credentials.setPassword(new char[]{});
    }

    @Override
    public UserCredentials prepareLogin(SecurityAuthority authority,
                                        int reasonCode)
    {
        return credentials;
    }

    @Override
    public boolean loginPreparationSuccessful()
    {
        return true;
    }

    @Override
    public boolean login(XMPPConnection connection, String userName,
                         String resource)
        throws XMPPException
    {
        connection.loginAnonymously();

        return true;
    }

    @Override
    public boolean isTlsRequired()
    {
        return false;
    }

    @Override
    public SSLContext createSslContext(CertificateService certificateService,
                                       X509TrustManager trustManager)
        throws GeneralSecurityException
    {
        return null;
    }
}
