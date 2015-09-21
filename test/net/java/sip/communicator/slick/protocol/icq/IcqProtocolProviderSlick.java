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
package net.java.sip.communicator.slick.protocol.icq;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * @author Emil Ivov
 */
public class IcqProtocolProviderSlick
    extends TestSuite
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(getClass().getName());

    /**
     * The name of the system property that contains the id of the account
     * that will be used when signing the icq protocol provider on icq.
     */
    public static final String TESTED_IMPL_USER_ID_PROP_NAME =
        "accounts.icq.TESTED_IMPL_ACCOUNT_ID";

    /**
     * The name of the system property that contains the password for the
     * account that will be used when signing the icq protocol provider on icq.
     */
    public static final String TESTED_IMPL_PWD_PROP_NAME =
        "accounts.icq.TESTED_IMPL_PWD";

    /**
     * The name of the system property that contains the id of the account
     * that will be used by the SLICK itself when signing on icq
     */
    public static final String TESTING_IMPL_USER_ID_PROP_NAME =
        "accounts.icq.TESTING_IMPL_ACCOUNT_ID";

    /**
     * The name of the system property that contains the password for the
     * account that will be used when signing the icq protocol provider on icq.
     */
    public static final String TESTING_IMPL_PWD_PROP_NAME =
        "accounts.icq.TESTING_IMPL_PWD";

    /**
     * The name of the property the value of which is a formatted string that
     * contains the contact list that.
     */
    public static final String CONTACT_LIST_PROPERTY_NAME
        = "accounts.icq.CONTACT_LIST";

    /**
     * The name of the property that indicates whether the user would like to
     * only run the offline tests.
     */
    public static final String DISABLE_ONLINE_TESTS_PROPERTY_NAME
        = "accounts.icq.DISABLE_ONLINE_TESTING";

    /**
     * Start the ICQ Sevice Leveraging Implementation Compatibility Kit.
     *
     * @param bundleContext BundleContext
     * @throws Exception if anything goes wrong.
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        setName("IcqProtocolProviderSlick");
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        //store the bundle cache reference for usage by other others
        IcqSlickFixture.bc = bundleContext;

        // verify whether the user wants to avoid online testing
        String offlineMode = System.getProperty(
            DISABLE_ONLINE_TESTS_PROPERTY_NAME, null);

        if (offlineMode != null && offlineMode.equalsIgnoreCase("true"))
            IcqSlickFixture.onlineTestingDisabled = true;

        // identify our testing agent on icq - it MUST be defined.
        String icqTestAgentName = System.getProperty(
                TESTING_IMPL_USER_ID_PROP_NAME, null);

        // we can only set up the real icq test suites when the
        // accounts.properties file defines the two test accounts
        if ( icqTestAgentName != null )
        {
            //it is defined, so register our testing agent on icq.
            IcqSlickFixture.testerAgent =
                    new IcqTesterAgent(icqTestAgentName);

            // find out the password for the test agent on icq. It
            // probably exists because we found the properties file above
            String icqTestAgentPwd = System.getProperty(
                    TESTING_IMPL_PWD_PROP_NAME, null);

            // .. and try to register the icq test agent (online)
            if (IcqSlickFixture.testerAgent.register(icqTestAgentPwd))
            {

                if(!IcqSlickFixture.onlineTestingDisabled)
                {
                    IcqSlickFixture.testerAgent.setAuthorizationRequired();

                    //initialize the tested account's contact list so that
                    //it could be ready when testing starts.
                    try
                    {
                        initializeTestedContactList();
                    }
                    catch (Exception ex)
                    {
                        logger.error("Error initing of tester agent list", ex);
                    }

                    //As Tested account is not registered here we send him a
                    //message. Message will be delivered offline. receive test
                    //is in TestOperationSetBasicInstantMessaging
                    //                           .testReceiveOfflineMessages()
                    String offlineMsgBody =
                        "This is a Test Message. "
                        + "Supposed to be delivered as offline message!";
                    IcqSlickFixture.offlineMsgCollector =
                        new IcqSlickFixture.OfflineMsgCollector();
                    IcqSlickFixture.offlineMsgCollector.setMessageText(
                                                                offlineMsgBody);
                    IcqSlickFixture.testerAgent.sendOfflineMessage(
                        System.getProperty(TESTED_IMPL_USER_ID_PROP_NAME, null),
                        offlineMsgBody);
                }

                //First test account installation so that the service that has
                //been installed by it gets tested by the rest of the tests.
                addTest(TestAccountInstallation.suite());

                //This must remain second as that's where the protocol would be
                //made to login/authenticate/signon its service provider.
                addTest(TestProtocolProviderServiceIcqImpl.suite());

                addTest(TestOperationSetPresence.suite());

                //the following should only be run when we want online testing.
                if(!IcqSlickFixture.onlineTestingDisabled)
                {
                    addTest(TestOperationSetPersistentPresence.suite());

                    addTest(TestOperationSetBasicInstantMessaging.suite());

                    addTest(TestOperationSetTypingNotifications.suite());

//                    addTest(TestOperationSetServerStoredInfo.suite());

                    addTest(TestOperationSetFileTransferImpl.suite());
                }

                //This must remain after all other tests using the accounts
                //are done since it tests account uninstallation and the
                //accounts we use for testing won't be available after that.
                addTest(TestAccountUninstallation.suite());

                //This must remain last since it counts on the fact that
                //account uninstallation has already been executed and that.
                addTestSuite(TestAccountUninstallationPersistence.class);
            }
            else {
                // accounts.properties file exists - but register failed
                // so install a single test to fail in a meaningful way
                addTest(new TestAccountInvalidNotification(
                        "failIcqTesterAgentRegisterRejected"));
            }
        }
        else {
            // accounts.properties file probably missing - it defines the
            // two test accounts required for most unit tests
            // so install a single test to fail in a meaningful way
                addTest( new TestAccountInvalidNotification(
                                                "failIcqTesterAgentMissing"));

        }

        bundleContext.registerService(getClass().getName(), this, properties);

        logger.debug("Successfully registered " + getClass().getName());
    }

    /**
     * Signs the testerAgent off the icq servers
     *
     * @param bundleContext a valid OSGI bundle context.
     * @throws Exception in case anything goes wrong
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        if (IcqSlickFixture.testerAgent != null )
            IcqSlickFixture.testerAgent.unregister();
    }

    /**
     * The method would make a tester agent sign on icq, ERASE the contact list
     * of the account that is being used, fill it in with dummy data (stored
     * in the CONTACT_LIST property) that we will later fetch from the tested
     * implementation, and sign out.
     */
    private void initializeTestedContactList()
    {
        String contactList = System.getProperty(CONTACT_LIST_PROPERTY_NAME, null);

        logger.debug("The " + CONTACT_LIST_PROPERTY_NAME
                     + " property is set to=" +contactList);

        if(    contactList == null
            || contactList.trim().length() < 6)//at least 4 for a UIN, 1 for the
                                               // dot and 1 for the grp name
            throw new IllegalArgumentException(
                "The " + CONTACT_LIST_PROPERTY_NAME +
                " property did not contain a contact list.");
        StringTokenizer tokenizer = new StringTokenizer(contactList, " \n\t");

        logger.debug("tokens contained by the CL tokenized="
            +tokenizer.countTokens());

        Hashtable<String, List<String>> contactListToCreate
            = new Hashtable<String, List<String>>();

        //go over all group.uin tokens
        while (tokenizer.hasMoreTokens())
        {
            String groupUinToken = tokenizer.nextToken();
            int dotIndex = groupUinToken.indexOf(".");

            if ( dotIndex == -1 )
            {
                throw new IllegalArgumentException(groupUinToken
                    + " is not a valid Group.UIN token");
            }

            String groupName = groupUinToken.substring(0, dotIndex);
            String uin = groupUinToken.substring(dotIndex + 1);

            if(    groupName.trim().length() < 1
                || uin.trim().length() < 4 )
            {
                throw new IllegalArgumentException(
                    groupName + " or " + uin +
                    " are not a valid group name or ICQ UIN.");
            }

            //check if we've already seen this group and if not - add it
            List<String> uinInThisGroup = contactListToCreate.get(groupName);
            if (uinInThisGroup == null)
            {
                uinInThisGroup = new ArrayList<String>();
                contactListToCreate.put(groupName, uinInThisGroup);
            }

            uinInThisGroup.add(uin);
        }

        //we don't need to continue if online testing is disabled since
        //the following won't be of any use.
        if(IcqSlickFixture.onlineTestingDisabled)
            return;

        //Create a tester agent that would connect with the tested impl account
        //and initialize the contact list according to what we just parsed.
        IcqTesterAgent cListInitTesterAgent = new IcqTesterAgent(
                System.getProperty(TESTED_IMPL_USER_ID_PROP_NAME, null)
            );
        cListInitTesterAgent.register(
                System.getProperty(TESTED_IMPL_PWD_PROP_NAME, null)
            );
        cListInitTesterAgent.setAuthorizationRequired();

        cListInitTesterAgent.initializeBuddyList(contactListToCreate);

        cListInitTesterAgent.unregister();

        //store the created contact list for later reference
        IcqSlickFixture.preInstalledBuddyList = contactListToCreate;
    }
}
