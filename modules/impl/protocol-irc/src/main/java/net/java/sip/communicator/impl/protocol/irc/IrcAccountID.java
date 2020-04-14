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
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The IRC implementation of a sip-communicator AccountID.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Danny van Heumen
 */
public class IrcAccountID
    extends AccountID
{
    /**
     * IRC account server host.
     */
    private final String host;

    /**
     * IRC account server port.
     */
    private final int port;

    /**
     * Creates an account id from the specified id and account properties.
     *
     * @param userID the user identifier corresponding to this account
     * @param host IRC server host
     * @param port IRC server port
     * @param accountProperties any other properties necessary for the account.
     */
    IrcAccountID(final String userID, final String host, final String port,
        final Map<String, String> accountProperties)
    {
        super(userID, accountProperties, ProtocolNames.IRC,
            getServiceName(accountProperties));
        if (host == null)
        {
            throw new IllegalArgumentException("host cannot be null");
        }
        this.host = host;
        if (port == null)
        {
            throw new IllegalArgumentException("port cannot be null");
        }
        this.port = Integer.parseInt(port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + host.hashCode();
        result = prime * result + port;
        return result;
    }

    /**
     * Equality extended with checking IRC server host and port, since different
     * IRC networks can have users with similar names.
     *
     * @param obj other object
     * @return returns true if equal or false otherwise
     */
    @Override
    public boolean equals(final Object obj)
    {
        // TODO if available, base equality on NETWORK=<identifier> in
        // RPL_ISUPPORT.

        return super.equals(obj)
            && this.host.equals(((IrcAccountID) obj).host)
            && this.port == ((IrcAccountID) obj).port;
    }

    /**
     * Get the IRC server host.
     *
     * @return returns IRC server host
     */
    public String getHost()
    {
        return this.host;
    }

    /**
     * Get the IRC server port.
     *
     * @return returns IRC server port
     */
    public int getPort()
    {
        return this.port;
    }

    /**
     * Returns the service name - the server we are logging to if it is null
     * which is not supposed to be - we return for compatibility the string we
     * used in the first release for creating AccountID (Using this string is
     * wrong, but used for compatibility for now).
     *
     * @param accountProperties Map the properties table configuring the account
     * @return String the service name
     */
    private static String getServiceName(
        final Map<String, String> accountProperties)
    {
        String serviceName =
            accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);
        if (serviceName != null)
        {
            final String port =
                accountProperties.get(ProtocolProviderFactory.SERVER_PORT);
            if (port != null)
            {
                serviceName += ":" + port;
            }
        }
        return (serviceName == null) ? ProtocolNames.IRC : serviceName;
    }

    /**
     * Get display name for this account instance.
     *
     * @return returns the display name for this AccountID instance
     */
    @Override
    public String getDisplayName()
    {
        // If the ACCOUNT_DISPLAY_NAME property has been set for this account
        // we'll be using it as a display name.
        String key = ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME;
        String accountDisplayName = accountProperties.get(key);
        if (accountDisplayName != null && !accountDisplayName.isEmpty())
        {
            return accountDisplayName;
        }
        // Construct our own account display name.
        return String.format("%s@%s:%s (%s)", this.getUserID(), this.host,
            this.port, this.getProtocolName());
    }
}
