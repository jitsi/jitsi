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
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>OperationSetCusaxUtilsSipImpl</tt> provides utility methods
 * related to the SIP CUSAX implementation.
 *
 * @author Damian Minkov
 */
public class OperationSetCusaxUtilsSipImpl
    implements OperationSetCusaxUtils
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetCusaxUtilsSipImpl.class);

    /**
     * The parent jabber protocol provider.
     */
    private final ProtocolProviderServiceSipImpl provider;

    /**
     * Constructs this operation set.
     * @param provider the parent provider.
     */
    public OperationSetCusaxUtilsSipImpl(
        ProtocolProviderServiceSipImpl provider)
    {
        this.provider = provider;
    }

    /**
     * Checks if the given <tt>detailAddress</tt> exists in the given
     * <tt>contact</tt> details.
     *
     * @param contact the <tt>Contact</tt>, which details to check
     * @param detailAddress the detail address we're looking for
     * @return <tt>true</tt> if the given <tt>detailAdress</tt> exists in the
     * details of the given <tt>contact</tt>
     */
    public boolean doesDetailBelong(Contact contact, String detailAddress)
    {
        return false;
    }

    /**
     * Returns the linked CUSAX provider for this protocol provider.
     *
     * @return the linked CUSAX provider for this protocol provider or null
     * if such isn't specified
     */
    public ProtocolProviderService getLinkedCusaxProvider()
    {
        String cusaxProviderID = provider.getAccountID()
            .getAccountPropertyString(
                ProtocolProviderFactory.CUSAX_PROVIDER_ACCOUNT_PROP);

        if (cusaxProviderID == null)
            return null;

        AccountID acc
            = ProtocolProviderActivator.getAccountManager()
                .findAccountID(cusaxProviderID);

        if(acc == null)
        {
            logger.warn("No connected cusax account found for "
                + cusaxProviderID);
            return null;
        }
        else
        {
            for (ProtocolProviderService pProvider :
              ProtocolProviderActivator.getProtocolProviders())
            {
                if(pProvider.getAccountID().equals(acc))
                {
                    return pProvider;
                }
            }
        }

        return null;
    }
}
