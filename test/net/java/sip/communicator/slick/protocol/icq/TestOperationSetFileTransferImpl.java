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
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.slick.protocol.generic.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Implementation for generic file transfer.
 * @author Damian Minkov
 */
public class TestOperationSetFileTransferImpl
    extends TestOperationSetFileTransfer
{
    private static final Logger logger =
        Logger.getLogger(TestOperationSetFileTransferImpl.class);

    private static IcqSlickFixture fixture = null;

    private static OperationSetPresence opSetPresence1 = null;
    private static OperationSetPresence opSetPresence2 = null;

    private static OperationSetFileTransfer opSetFT1 = null;
    private static OperationSetFileTransfer opSetFT2 = null;

    private static Contact contact1 = null;
    private static Contact contact2 = null;

    private static ProtocolProviderFactory providerFactory = null;
    private static AccountID secondProviderAccount = null;

    public TestOperationSetFileTransferImpl(String name)
    {
        super(name);
    }

    @Override
    public Contact getContact1()
    {
        if(contact1 == null)
        {
            contact1 = opSetPresence1.findContactByID(IcqSlickFixture.testerAgent.getIcqUIN());
        }

        return contact1;
    }

    @Override
    public Contact getContact2()
    {
        if(contact2 == null)
        {
            contact2 = opSetPresence2.findContactByID(fixture.ourUserID);
        }

        return contact2;
    }

    @Override
    public void start()
        throws Exception
    {
        if(fixture != null)
            return;

        fixture = new IcqSlickFixture();

        /**
         * The lock that we wait on until registration is finalized.
         */
        final Object registrationLock = new Object();

        fixture.setUp();

        // make sure tester agent is turned off
        IcqSlickFixture.testerAgent.unregister();

        Map<String, OperationSet> supportedOperationSets1 =
            fixture.provider.getSupportedOperationSets();

        if ( supportedOperationSets1 == null
            || supportedOperationSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this implementation. ");

        //we also need the presence op set in order to retrieve contacts.
        opSetPresence1 =
            (OperationSetPresence)supportedOperationSets1.get(
                OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence1 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                + "implementation of at least one of the PresenceOperationSets");
        }

        opSetFT1 =
            (OperationSetFileTransfer)supportedOperationSets1.get(
                OperationSetFileTransfer.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetFT1 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                + "implementation of at least one of the FileTransferOperationSets");
        }

        // We will register new protocol provider for our tests
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.ICQ+")";
        try{
            serRefs = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol ICQ",
            (serRefs != null) && (serRefs.length >  0));

        BundleContext bc = IcqSlickFixture.bc;

        //Keep the reference for later usage.
        providerFactory = (ProtocolProviderFactory)
            bc.getService(serRefs[0]);

        // fisrt install the account
        String USER_ID = IcqSlickFixture.testerAgent.getIcqUIN();
        final String PASSWORD = System.getProperty(
            IcqProtocolProviderSlick.TESTED_IMPL_PWD_PROP_NAME, null);
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("USER_ID", USER_ID);
        props.put("PASSWORD", PASSWORD);
        try
        {
            providerFactory.installAccount(USER_ID, props);
        }
        catch (Exception e) // Exception if account exists
        {}

        String secondProviderID = IcqSlickFixture.testerAgent.getIcqUIN();

        //find the protocol provider service
        ServiceReference[] icqProviderRefs
            = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.ICQ+")"
                +"("+ProtocolProviderFactory.USER_ID+"="
                + secondProviderID +")"
                +")");

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for ICQ UIN:"+ secondProviderID,
                     icqProviderRefs);
        assertTrue("No Protocol Provider was found for ICQ UIN:"+ secondProviderID,
                     icqProviderRefs.length > 0);

        ProtocolProviderService provider2 =
            (ProtocolProviderService)bc.getService(icqProviderRefs[0]);

        secondProviderAccount = provider2.getAccountID();

        Map<String, OperationSet> supportedOperationSets2 =
            provider2.getSupportedOperationSets();

        if ( supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this implementation. ");

        opSetPresence2 =
            (OperationSetPresence) supportedOperationSets2.get(
                OperationSetPresence.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetPresence2 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                + "implementation of at least one of the PresenceOperationSets");
        }

        opSetFT2 =
            (OperationSetFileTransfer)supportedOperationSets2.get(
                OperationSetFileTransfer.class.getName());

        //if the op set is null show that we're not happy.
        if (opSetFT2 == null)
        {
            throw new NullPointerException(
                "An implementation of the service must provide an "
                + "implementation of at least one of the FileTransferOperationSets");
        }

        provider2.addRegistrationStateChangeListener(new RegistrationStateChangeListener() {

            public void registrationStateChanged(RegistrationStateChangeEvent evt)
            {
                if(evt.getNewState().equals( RegistrationState.REGISTERED))
                {
                    try
                    {
                        synchronized(registrationLock)
                        {
                            registrationLock.notifyAll();
                        }
                    }
                    catch (Exception e)
                    {
                        logger.error("Error creating contactlist", e);
                    }
                }
            }
        });

        provider2.register(new SecurityAuthority() {

            public UserCredentials obtainCredentials(
                String realm, UserCredentials defaultValues, int reasonCode)
            {
                return obtainCredentials(realm, defaultValues);
            }

            public UserCredentials obtainCredentials(
                String realm, UserCredentials defaultValues)
            {
                defaultValues.setPassword(PASSWORD.toCharArray());
                return defaultValues;
            }

            public void setUserNameEditable(boolean isUserNameEditable){}

            public boolean isUserNameEditable(){return false;}
        });

        synchronized(registrationLock)
        {
            logger.info("Waiting 1!");
            registrationLock.wait(60000);
            logger.info("Stop waiting!");
        }

        fixture.provider.addRegistrationStateChangeListener(new RegistrationStateChangeListener() {

            public void registrationStateChanged(RegistrationStateChangeEvent evt)
            {
                if(evt.getNewState().equals( RegistrationState.REGISTERED))
                {
                    try
                    {
                        prepareContactList();

                        synchronized(registrationLock)
                        {
                            registrationLock.notifyAll();
                        }
                    }
                    catch (Exception e)
                    {
                        logger.error("Error creating contactlist", e);
                    }
                }
            }
        });

        fixture.provider.register(new SecurityAuthority() {

            public UserCredentials obtainCredentials(
                String realm, UserCredentials defaultValues, int reasonCode)
            {
                return obtainCredentials(realm, defaultValues);
            }

            public UserCredentials obtainCredentials(
                String realm, UserCredentials defaultValues)
            {
                defaultValues.setPassword(
                    System.getProperty(
                        IcqProtocolProviderSlick.TESTED_IMPL_PWD_PROP_NAME, null).toCharArray());
                return defaultValues;
            }

            public void setUserNameEditable(boolean isUserNameEditable){}

            public boolean isUserNameEditable(){return false;}
        });

        synchronized(registrationLock)
        {
            logger.info("Waiting 2!");
            registrationLock.wait(60000);
            logger.info("Stop waiting!");
        }
    }

    /**
     * Creates a test suite containing all tests of this class followed by
     * method that we want executed last to clear used providers.
     * @return the Test suite to run
     */
    public static Test suite()
    {
        TestSuite suite =
            new TestSuite(TestOperationSetFileTransferImpl.class);

        suite.addTest(
            new TestOperationSetFileTransferImpl("clearProviders"));

        return suite;
    }

    public void clearProviders()
    {
        fixture.tearDown();
        providerFactory.uninstallAccount(secondProviderAccount);
    }

    @Override
    public void stop()
        throws Exception
    {
    }

    /**
     * Create the list to be sure that contacts exchanging messages
     * exists in each other lists
     * @throws Exception
     */
    public void prepareContactList()
        throws Exception
    {
        if(getContact1() == null)
        {
            Object o = new Object();
            synchronized(o)
            {
                o.wait(2000);
            }

            try
            {
                opSetPresence1.setAuthorizationHandler(new AuthHandler());
                opSetPresence1.subscribe(IcqSlickFixture.testerAgent.getIcqUIN());
            }
            catch (OperationFailedException ex)
            {
                // the contact already exist its OK
            }
        }

        if(getContact2() == null)
        {
            try
            {
                opSetPresence2.setAuthorizationHandler(new AuthHandler());
                opSetPresence2.subscribe(fixture.ourUserID);
            }
            catch (OperationFailedException ex1)
            {
                // the contact already exist its OK
            }

            logger.info("will wait till the list prepare is completed");
            Object o = new Object();
            synchronized(o)
            {
                o.wait(4000);
            }
        }
    }

    @Override
    public OperationSetFileTransfer getOpSetFilTransfer1()
    {
        return opSetFT1;
    }

    @Override
    public OperationSetFileTransfer getOpSetFilTransfer2()
    {
        return opSetFT2;
    }

    @Override
    public BundleContext getContext()
    {
        return IcqSlickFixture.bc;
    }

    @Override
    public boolean enableTestSendAndReceive()
    {
        return true;
    }

    @Override
    public boolean enableTestSenderCancelBeforeAccepted()
    {
        return true;
    }

    /**
     * Disabled cause when receiver declines a canceled event is fired.
     * But refused status changed must be fired.
     * Its canceled cause its the same event comming from the stack
     * as when while transfering one of the parties cancel the transfer.
     */
    @Override
    public boolean enableTestReceiverDecline()
    {
        return false;
    }

    @Override
    public boolean enableTestReceiverCancelsWhileTransfering()
    {
        return true;
    }

    @Override
    public boolean enableTestSenderCancelsWhileTransfering()
    {
        return true;
    }

    private class AuthHandler
        implements AuthorizationHandler
    {

        public AuthorizationResponse processAuthorisationRequest(AuthorizationRequest req, Contact sourceContact)
        {
            logger.trace("processAuthorisationRequest " + req + " " +
                             sourceContact);

            return new AuthorizationResponse(AuthorizationResponse.ACCEPT, "");
        }

        public AuthorizationRequest createAuthorizationRequest(Contact contact)
        {
            logger.trace("createAuthorizationRequest " + contact);
            return new AuthorizationRequest();
        }

        public void processAuthorizationResponse(AuthorizationResponse response, Contact sourceContact)
        {
            logger.debug("auth response from: " +
                sourceContact.getAddress() + " " +
                response.getResponseCode().getCode());
        }
    }
}
