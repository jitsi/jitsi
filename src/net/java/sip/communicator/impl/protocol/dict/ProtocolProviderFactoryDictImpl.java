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
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The Dict protocol provider factory creates instances of the Dict
 * protocol provider service. One Service instance corresponds to one account.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class ProtocolProviderFactoryDictImpl
    extends ProtocolProviderFactory
{
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderFactoryDictImpl.class);

    /**
     * Creates an instance of the ProtocolProviderFactoryDictImpl.
     */
    public ProtocolProviderFactoryDictImpl()
    {
        super(DictActivator.getBundleContext(), ProtocolNames.DICT);
    }

    /**
     * Initializaed and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr The user identifier uniquely representing the newly
     *   created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account.
     */
    @Override
    public AccountID installAccount( String userIDStr,
                                     Map<String, String> accountProperties)
    {
        BundleContext context = DictActivator.getBundleContext();
        if (context == null)
        {
            throw new NullPointerException("The specified BundleContext was null");
        }
        if (userIDStr == null)
        {
            throw new NullPointerException("The specified AccountID was null");
        }
        if (accountProperties == null)
        {
            throw new NullPointerException("The specified property map was null");
        }

        accountProperties.put(USER_ID, userIDStr);

        AccountID accountID = new DictAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if (registeredAccounts.containsKey(accountID))
        {
            throw new IllegalStateException("An account for id " + userIDStr + " was already installed!");
        }

        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (through the UI) a call to the
        //ProtocolProviderService.register() method and it needs to acces
        //the configuration service and check for a stored password.
        this.storeAccount(accountID, false);

        accountID = loadAccount(accountProperties);

        // Creates the dict contact group.
        this.createGroup();
        // Creates the default conatct for this dict server.
        this.createDefaultContact(accountID);

        return accountID;
    }

    @Override
    protected AccountID createAccountID(String userID, Map<String, String> accountProperties)
    {
        return new DictAccountID(userID, accountProperties);
    }

    @Override
    protected ProtocolProviderService createService(String userID,
        AccountID accountID)
    {
        ProtocolProviderServiceDictImpl service =
            new ProtocolProviderServiceDictImpl();

        service.initialize(userID, accountID);
        return service;
    }

    /**
     * Creates a group for the dict contacts
     */
    private void createGroup()
    {
        // Get MetaContactListService
        BundleContext bundleContext = getBundleContext();
        ServiceReference<MetaContactListService> mfcServiceRef
            = bundleContext.getServiceReference(MetaContactListService.class);

        MetaContactListService mcl = bundleContext.getService(mfcServiceRef);

        try
        {
            String groupName = DictActivator.getResources()
                .getI18NString("service.protocol.DICTIONARIES");

            mcl.createMetaContactGroup(mcl.getRoot(), groupName);
        }
        catch (MetaContactListException ex)
        {
            int errorCode = ex.getErrorCode();
            if (errorCode != MetaContactListException.CODE_GROUP_ALREADY_EXISTS_ERROR)
            {
                logger.error(ex);
            }
        }
    }

    /**
     * Creates a default contact for the new DICT server.
     * @param accountID The accountID of the dict protocol provider for which we
     * want to add a default contact.
     */
    private void createDefaultContact(AccountID accountID)
    {
        // Gets the MetaContactListService.
        BundleContext bundleContext = getBundleContext();
        ServiceReference<MetaContactListService> mfcServiceRef
            = bundleContext.getServiceReference(MetaContactListService.class);
        MetaContactListService mcl = bundleContext.getService(mfcServiceRef);

        // Gets the ProtocolProviderService.
        ServiceReference<ProtocolProviderService> serRef
            = getProviderForAccount(accountID);
        ProtocolProviderService protocolProvider
            = DictActivator.getBundleContext().getService(serRef);

        // Gets group name
        String groupName = DictActivator.getResources()
            .getI18NString("service.protocol.DICTIONARIES");

        // Gets contact name
        String contactName = DictActivator.getResources()
            .getI18NString("plugin.dictaccregwizz.ANY_DICTIONARY_FORM",
                new String[] {accountID.getUserID()});

        // Gets the MetaContactGroup for the "dictionaries" group.
        MetaContactGroup group = mcl.getRoot().getMetaContactSubgroup(groupName);

        // Sets the default contact identifier to "*" corresponding to "all the
        // dictionaries" available on the server (cf. RFC-2229).
        String dict_uin = "*";
        // Create the default contact.
        mcl.createMetaContact(protocolProvider, group, dict_uin);
        // Rename the default contact.
        mcl.renameMetaContact(
                group.getMetaContact(protocolProvider, dict_uin),
                contactName);
    }

    @Override
    public void modifyAccount(
            ProtocolProviderService protocolProvider,
            Map<String, String> accountProperties)
        throws NullPointerException
    {
        // TODO Auto-generated method stub
    }
}
