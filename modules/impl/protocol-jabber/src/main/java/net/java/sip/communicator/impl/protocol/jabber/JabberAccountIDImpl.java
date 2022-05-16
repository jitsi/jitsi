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

import java.util.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabber.*;

/**
 * The Jabber implementation of a sip-communicator AccountID
 *
 * @author Damian Minkov
 * @author Sebastien Vincent
 */
public class JabberAccountIDImpl
    extends JabberAccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     * @param id the id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    JabberAccountIDImpl(String id, Map<String, String> accountProperties)
    {
        super( id, accountProperties );
    }

    /**
     * Returns the list of STUN servers that this account is currently
     * configured to use.
     *
     * @return the list of STUN servers that this account is currently
     * configured to use.
     */
    public List<StunServerDescriptor> getStunServers()
    {
        Map<String, String> accountProperties = getAccountProperties();
        List<StunServerDescriptor> serList
            = new ArrayList<StunServerDescriptor>();

        for (int i = 0; i < StunServerDescriptor.MAX_STUN_SERVER_COUNT; i ++)
        {
            StunServerDescriptor stunServer
                = StunServerDescriptor.loadDescriptor(
                        accountProperties,
                        ProtocolProviderFactory.STUN_PREFIX + i);

            // If we don't find a stun server with the given index, it means
            // that there're no more servers left in the table so we've nothing
            // more to do here.
            if (stunServer == null)
                break;

            String password = this.loadStunPassword(
                ProtocolProviderFactory.STUN_PREFIX + i);

            if(password != null)
                stunServer.setPassword(password);

            serList.add(stunServer);
        }

        return serList;
    }

    /**
     * Load password for this STUN descriptor.
     *
     * @param namePrefix name prefix
     * @return password or null if empty
     */
    private String loadStunPassword(String namePrefix)
    {
        String password = null;
        String className = ProtocolProviderServiceJabberImpl.class.getName();
        String packageSourceName = className.substring(0,
                className.lastIndexOf('.'));

        String accountPrefix = ProtocolProviderFactory.findAccountPrefix(
                JabberActivator.bundleContext,
                this, packageSourceName);

        CredentialsStorageService credentialsService =
            JabberActivator.getCredentialsStorageService();

        try
        {
            password = credentialsService.
                loadPassword(accountPrefix + "." + namePrefix);
        }
        catch(Exception e)
        {
            return null;
        }

        return password;
    }
}
