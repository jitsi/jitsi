/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.msn;

import java.util.*;

import junit.framework.*;

import net.java.sip.communicator.service.protocol.*;

import net.java.sip.communicator.slick.protocol.generic.*;

/**
 * Tests for the MSN ad-hoc multi-user chat operation set.
 * 
 * @author Valentin Martinet
 */
public class TestOperationSetAdHocMultiUserChatMsnImpl 
extends TestOperationSetAdHocMultiUserChat  
{
    /**
     * Creates the test with the specified method name.
     * 
     * @param name the name of the method to execute.
     */
    public TestOperationSetAdHocMultiUserChatMsnImpl(String name)
    {
        super(name);
    }

    /**
     * Creates a test suite containing tests of this class in a specific order.
     *
     * @return Test a tests suite containing all tests to execute.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(new TestOperationSetAdHocMultiUserChatMsnImpl(
            "testRegisterAccount3"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatMsnImpl(
        "prepareContactList"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatMsnImpl(
        "testCreateRoom"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatMsnImpl(
        "testPeerJoined"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatMsnImpl(
        "testSendIM"));
        suite.addTest(new TestOperationSetAdHocMultiUserChatMsnImpl(
        "testPeerLeaved"));

        return suite;
    }

    public void testRegisterAccount3() throws OperationFailedException
    {
            fixture.provider3.register(
                    new SecurityAuthorityImpl(
                        System.getProperty(
                            MsnProtocolProviderServiceLick.ACCOUNT_3_PREFIX
                            + ProtocolProviderFactory.PASSWORD).toCharArray()));
            
            assertEquals(fixture.provider3.getRegistrationState(), 
                RegistrationState.REGISTERED);
    }
    
    public void start() throws Exception 
    {
        fixture = new MsnSlickFixture();
        fixture.setUp();

        // Supported operation sets by each protocol provider.
        Map<String, OperationSet> 
        supportedOpSets1, supportedOpSets2, supportedOpSets3;

        supportedOpSets1 = fixture.provider1.getSupportedOperationSets();
        supportedOpSets2 = fixture.provider2.getSupportedOperationSets();
        supportedOpSets3 = fixture.provider3.getSupportedOperationSets();

        //
        // Initialization of operation sets for the first testing account:
        //

        if (supportedOpSets1 == null || supportedOpSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by " +
            "this implementation. ");

        opSetAHMUC1 = (OperationSetAdHocMultiUserChat) supportedOpSets1.get(
            OperationSetAdHocMultiUserChat.class.getName());

        if (opSetAHMUC1 == null)
            throw new NullPointerException(
            "No implementation for multi user chat was found");

        opSetPresence1 = (OperationSetPresence) supportedOpSets1.get(
            OperationSetPresence.class.getName());

        if (opSetPresence1 == null)
            throw new NullPointerException(
                "An implementation of the service must provide an " + 
            "implementation of at least one of the PresenceOperationSets");


        //
        // Initialization of operation sets for the second testing account:
        //

        if (supportedOpSets2 == null || supportedOpSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by " + 
            "this implementation. ");

        opSetAHMUC2 = (OperationSetAdHocMultiUserChat) supportedOpSets2.get(
            OperationSetAdHocMultiUserChat.class.getName());

        if (opSetAHMUC2 == null)
            throw new NullPointerException(
            "No implementation for ad hoc multi user chat was found");

        opSetPresence2 = (OperationSetPresence) supportedOpSets2.get(
            OperationSetPresence.class.getName());

        if (opSetPresence2 == null)
            throw new NullPointerException(
                "An implementation of the service must provide an " + 
            "implementation of at least one of the PresenceOperationSets");


        //
        // Initialization of operation sets for the third testing account:
        //

        if (supportedOpSets3 == null || supportedOpSets3.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by " + 
            "this implementation. ");

        opSetAHMUC3 = (OperationSetAdHocMultiUserChat) supportedOpSets3.get(
            OperationSetAdHocMultiUserChat.class.getName());

        if (opSetAHMUC3 == null)
            throw new NullPointerException(
            "No implementation for ad hoc multi user chat was found");

        opSetPresence3 = (OperationSetPresence) supportedOpSets3.get(
            OperationSetPresence.class.getName());

        if (opSetPresence3 == null)
            throw new NullPointerException(
                "An implementation of the service must provide an " + 
            "implementation of at least one of the PresenceOperationSets");
    }
    
    /**
     * A very simple straight forward implementation of a security authority
     * that would always return the same password (the one specified upon
     * construction) when asked for credentials.
     */
    public class SecurityAuthorityImpl
        implements SecurityAuthority
    {
        /**
         * The password to return when asked for credentials
         */
        private char[] passwd = null;

        private boolean isUserNameEditable = false;

        /**
         * Creates an instance of this class that would always return "passwd"
         * when asked for credentials.
         *
         * @param passwd the password that this class should return when
         * asked for credentials.
         */
        public SecurityAuthorityImpl(char[] passwd)
        {
            this.passwd = passwd;
        }

        /**
         * Returns a Credentials object associated with the specified realm.
         * <p>
         * @param realm The realm that the credentials are needed for.
         * @param defaultValues the values to propose the user by default
         * @param reasonCode the reason for which we're obtaining the
         * credentials.
         * @return The credentials associated with the specified realm or null
         * if none could be obtained.
         */
        public UserCredentials obtainCredentials(String          realm,
                                                 UserCredentials defaultValues,
                                                 int reasonCode)
        {
            return obtainCredentials(realm, defaultValues);
        }

        /**
         * Returns a Credentials object associated with the specified realm.
         * <p>
         * @param realm The realm that the credentials are needed for.
         * @param defaultValues the values to propose the user by default
         * @return The credentials associated with the specified realm or null
         * if none could be obtained.
         */
        public UserCredentials obtainCredentials(String          realm,
                                                 UserCredentials defaultValues)
        {
            defaultValues.setPassword(passwd);
            return defaultValues;
        }

        /**
         * Sets the userNameEditable property, which should indicate if the
         * user name could be changed by user or not.
         * 
         * @param isUserNameEditable indicates if the user name could be changed
         */
        public void setUserNameEditable(boolean isUserNameEditable)
        {
            this.isUserNameEditable = isUserNameEditable;
        }
        
        /**
         * Indicates if the user name is currently editable, i.e. could be changed
         * by user or not.
         * 
         * @return <code>true</code> if the user name could be changed,
         * <code>false</code> - otherwise.
         */
        public boolean isUserNameEditable()
        {
            return isUserNameEditable;
        }
    }
}
