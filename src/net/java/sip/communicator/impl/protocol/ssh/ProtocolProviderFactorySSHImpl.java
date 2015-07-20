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
package net.java.sip.communicator.impl.protocol.ssh;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The SSH protocol provider factory creates instances of the SSH
 * protocol provider service. One Service instance corresponds to one account.
 *
 * @author Shobhit Jindal
 */
public class ProtocolProviderFactorySSHImpl
        extends ProtocolProviderFactorySSH
{

    /**
     * Creates an instance of the ProtocolProviderFactorySSHImpl.
     */
    public ProtocolProviderFactorySSHImpl()
    {
        super(SSHActivator.getBundleContext(), ProtocolNames.SSH);
    }

    /**
     * Initializaed and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr tha/a user identifier uniquely representing the newly
     *   created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account.
     */
    @Override
    public AccountID installAccount(
            String userIDStr,
            Map<String, String> accountProperties)
    {
        BundleContext context = SSHActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException("The specified BundleContext was " +
                    "null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        if (accountProperties == null)
            throw new NullPointerException("The specified property map was" +
                    " null");

        accountProperties.put(USER_ID, userIDStr);

        AccountID accountID = new SSHAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if (registeredAccounts.containsKey(accountID))
            throw new IllegalStateException(
                    "An account for id " + userIDStr + " was already" +
                            " installed!");

        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (through the UI) a call to the
        //ProtocolProviderService.register() method and it needs to acces
        //the configuration service and check for a stored password.
        this.storeAccount(accountID, false);

        accountID = loadAccount(accountProperties);

/*        ServiceReference ppServiceRef = context
                .getServiceReference(ProtocolProviderService.class.getName());

        ProtocolProviderService ppService = (ProtocolProviderService)
        context.getService(ppServiceRef);

        OperationSetPersistentPresence operationSetPersistentPresence =
            (OperationSetPersistentPresence) ppService.getOperationSet(
                OperationSetPersistentPresence.class);

        try
        {
            // The below should never fail for SSH accounts
            operationSetPersistentPresence.subscribe(userIDStr);

        }
        catch(OperationFailedException ex)
        {
            ex.printStackTrace();
        }
*/
        return accountID;
    }

    @Override
    protected AccountID createAccountID(String userID, Map<String, String> accountProperties)
    {
        return new SSHAccountID(userID, accountProperties);
    }

    @Override
    protected ProtocolProviderService createService(String userID,
        AccountID accountID)
    {
        ProtocolProviderServiceSSHImpl service =
            new ProtocolProviderServiceSSHImpl();

        service.initialize(userID, accountID);
        return service;
    }

//    /**
//     * Saves the password for the specified account after scrambling it a bit
//     * so that it is not visible from first sight (Method remains highly
//     * insecure).
//     *
//     * @param accountID the AccountID for the account whose password we're
//     * storing.
//     * @param passwd the password itself.
//     *
//     * @throws java.lang.IllegalArgumentException if no account corresponding
//     * to <tt>accountID</tt> has been previously stored.
//     */
//    public void storePassword(AccountID accountID, String passwd)
//    throws IllegalArgumentException
//    {
//        super.storePassword(SSHActivator.getBundleContext(),
//                accountID,
//                String.valueOf(Base64.encode(passwd.getBytes())));
//    }
//
//    /**
//     * Returns the password last saved for the specified account.
//     *
//     * @param accountID the AccountID for the account whose password we're
//     * looking for..
//     *
//     * @return a String containing the password for the specified accountID.
//     *
//     * @throws java.lang.IllegalArgumentException if no account corresponding
//     * to <tt>accountID</tt> has been previously stored.
//     */
//    public String loadPassword(AccountID accountID)
//    throws IllegalArgumentException
//    {
//        String password =  super.loadPassword(SSHActivator.getBundleContext()
//        , accountID );
//        return(String.valueOf(Base64.decode(password)));
//    }

    @Override
    public void modifyAccount(  ProtocolProviderService protocolProvider,
                                Map<String, String> accountProperties)
        throws NullPointerException
    {
        // TODO Auto-generated method stub

    }

}
