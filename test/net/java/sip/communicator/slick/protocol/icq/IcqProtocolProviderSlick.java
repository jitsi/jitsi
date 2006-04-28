package net.java.sip.communicator.slick.protocol.icq;

import junit.framework.*;
import org.osgi.framework.*;
import net.java.sip.communicator.util.*;
import java.util.*;
import java.text.*;

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
    public static final String TESTED_IMPL_ACCOUNT_ID_PROP_NAME =
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
    public static final String TESTING_IMPL_ACCOUNT_ID_PROP_NAME =
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
     * Start the Configuration Sevice Implementation Compatibility Kit.
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        setName("IcqProtocolProviderSlick");
        Hashtable properties = new Hashtable();
        properties.put("service.pid", getName());

        //store the bundle cache reference for usage by other others
        IcqSlickFixture.bc = bundleContext;

        //register our testing agent on icq.
        IcqSlickFixture.testerAgent =
            new IcqTesterAgent(System.getProperty(
                TESTING_IMPL_ACCOUNT_ID_PROP_NAME, null));
        if (!IcqSlickFixture.testerAgent.register(System.getProperty(
                TESTING_IMPL_PWD_PROP_NAME, null)))
            throw new Exception(
                "Registering the IcqTesterAgent on icq has failed.(Possible "
                +"reasons: authetification failed, or Connection rate limit "
                +"exceeded.)");

        //initialize the tested account's contact list so that it could be ready
        //when testing starts.
        initializeTestedContactList();


		//As Tested account is not registered here we send him a message.
		//Message will be delivered offline
        //receive test is in TestOperationSetBasicInstantMessaging.testReceiveOfflineMessages()
        String offlineMsgBody = "This is a Test Message. Supposed to be delivered as offline message!";
        IcqSlickFixture.offlineMsgCollector =
            new IcqSlickFixture.OfflineMsgCollector();
        IcqSlickFixture.offlineMsgCollector.setMessageText(offlineMsgBody);
		IcqSlickFixture.testerAgent.sendOfflineMessage(
			  System.getProperty(TESTED_IMPL_ACCOUNT_ID_PROP_NAME, null),
              offlineMsgBody
			);

        //First test account installation so that the service that has been
        //installed by it gets tested by the rest of the tests.
        addTestSuite(TestAccountInstallation.class);

        //This must remain second as that's where the protocol would be made
        //to login/authenticate/signon its service provider.
        addTest(TestProtocolProviderServiceIcqImpl.suite());

        addTest(TestOperationSetPresence.suite());

        addTest(TestOperationSetPersistentPresence.suite());

        addTest(TestOperationSetBasicInstantMessaging.suite());

        addTest(TestOperationSetTypingNotifications.suite());

        //This must remain last since it tests account uninstallation and
        //the accounts we use for testing won't be available after that.
        addTestSuite(TestAccountUninstallation.class);

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

        Hashtable contactListToCreate = new Hashtable();

        //go over all group.uin tokens
        while (tokenizer.hasMoreTokens())
        {
            String groupUinToken = tokenizer.nextToken();
            int dotIndex = groupUinToken.indexOf(".");

            if ( dotIndex == -1 ){
                throw new IllegalArgumentException(groupUinToken
                    + " is not a valid Group.UIN token");
            }

            String groupName = groupUinToken.substring(0, dotIndex);
            String uin = groupUinToken.substring(dotIndex + 1);

            if(    groupName.trim().length() < 1
                || uin.trim().length() < 4 ){
                throw new IllegalArgumentException(
                    groupName + " or " + uin +
                    " are not a valid group name or ICQ UIN.");
            }

            //check if we've already seen this group and if not - add it
            List uinInThisGroup = (List)contactListToCreate.get(groupName);
            if (uinInThisGroup == null){
                uinInThisGroup = new ArrayList();
                contactListToCreate.put(groupName, uinInThisGroup);
            }

            uinInThisGroup.add(uin);
        }

        //Create a tester agent that would connect with the tested impl account
        //and initialize the contact list according to what we just parsed.


        IcqTesterAgent cListInitTesterAgent = new IcqTesterAgent(
                System.getProperty(TESTED_IMPL_ACCOUNT_ID_PROP_NAME, null)
            );
        cListInitTesterAgent.register(
                System.getProperty(TESTED_IMPL_PWD_PROP_NAME, null)
            );

        cListInitTesterAgent.initializeBuddyList(contactListToCreate);

        cListInitTesterAgent.unregister();

        //store the created contact list for later reference
        IcqSlickFixture.preInstalledBuddyList = contactListToCreate;
    }
}
