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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;

/**
 * A jabber implementation of the password change operation set.
 *
 * @author Boris Grozev
 */
public class OperationSetChangePasswordJabberImpl
        implements OperationSetChangePassword
{
    /**
     * The <tt>ProtocolProviderService</tt> whose password we'll change.
     */
    private ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * The logger used by <tt>OperationSetChangePasswordJabberImpl</tt>.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetChangePasswordJabberImpl.class);

    /**
     * Sets the object protocolProvider to the one given.
     * @param protocolProvider the protocolProvider to use.
     */
    OperationSetChangePasswordJabberImpl (
                            ProtocolProviderServiceJabberImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Changes the jabber account password of protocolProvider to newPass.
     * @param newPass the new password.
     * @throws IllegalStateException if the account is not registered.
     * @throws OperationFailedException if the server does not support password
     * changes.
     */
    public void changePassword(String newPass)
            throws IllegalStateException, OperationFailedException
    {
        org.jivesoftware.smackx.iqregister.AccountManager accountManager
                = org.jivesoftware.smackx.iqregister.AccountManager.getInstance(
                                        protocolProvider.getConnection());

        try
        {
            accountManager.changePassword(newPass);
        }
        catch (XMPPException
            | InterruptedException
            | NoResponseException
            | NotConnectedException e)
        {
            if(logger.isInfoEnabled())
            {
                logger.info("Tried to change jabber password, but the server "
                        + "does not support inband password changes", e);
            }

            throw new OperationFailedException("In-band password changes not"
                    + " supported",
                    OperationFailedException.NOT_SUPPORTED_OPERATION,
                    e);
        }
    }

    /**
     * Returns true if the server supports password changes. Checks for
     * XEP-0077 (inband registrations) support via disco#info.
     *
     * @return True if the server supports password changes, false otherwise.
     */
    public boolean supportsPasswordChange()
    {
        try
        {
            Jid service = JidCreate.from(
                protocolProvider.getAccountID().getService());
            DiscoverInfo discoverInfo
                    = protocolProvider.getDiscoveryManager()
                        .discoverInfo(service);
            return discoverInfo.containsFeature(
                        ProtocolProviderServiceJabberImpl.URN_REGISTER);
        }
        catch(Exception e)
        {
            if(logger.isInfoEnabled())
                logger.info("Exception occurred while trying to find out if" +
                        " inband registrations are supported. Returning true" +
                        "anyway.");
            /* It makes sense to return true if something goes wrong, because
                failing later on is not fatal, and registrations are very
                likely to be supported.
             */
            return true;
        }

    }
}
