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
package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A mock protocol provider implementation that comes with a single operation
 * set (OperationSetPersistentPresence) for use by the MetaContactListSlcik.
 *
 * @author Emil Ivov
 */
public class MockProvider
    implements ProtocolProviderService
{
    /**
     * The SIP Communicator Mock Protocol name.
     */
    private static final String PROTO_NAME = "TheSipCommMockProtocol";

    /**
     * The operation sets that our mock provider supports.
     */
    private final Map<String, OperationSet> supportedOperationSets =
        new Hashtable<String, OperationSet>();

    /**
     * The presence operation set supported by the mock provider.
     */
    private MockPersistentPresenceOperationSet mockPresOpSet = null;

    /**
     * The identifier of the account.
     */
    private AccountID accountID = null;

    /**
     * Creates an instance of this mockprovider with a <tt>supportedOperationSet-s</tt>
     * map set to contain a single persistent presence operation set.
     *
     * @param userName an almost ignorable string (any value is accepted) that
     * should be used when constructing account id's
     */
    public MockProvider(String userName)
    {
        accountID = new MockAccountID(userName);

        mockPresOpSet = new MockPersistentPresenceOperationSet(this);
        MockBasicInstantMessaging mockBImOpSet =
            new MockBasicInstantMessaging(this, mockPresOpSet);

        this.supportedOperationSets.put(
                OperationSetPersistentPresence.class.getName(),
                mockPresOpSet);

        this.supportedOperationSets.put(
                OperationSetPresence.class.getName(),
                mockPresOpSet);

        this.supportedOperationSets.put(
                OperationSetBasicInstantMessaging.class.getName(),
                mockBImOpSet);

        this.supportedOperationSets.put(
                OperationSetMultiUserChat.class.getName(),
                new MockMultiUserChat(this));

        MockOperationSetBasicTelephony mockTelphonyOpSet =
            new MockOperationSetBasicTelephony(this);

        this.supportedOperationSets.put(
                OperationSetBasicTelephony.class.getName(),
                mockTelphonyOpSet);

        MockOperationSetFileTransfer mockFileTransferOpSet =
            new MockOperationSetFileTransfer(this);

        this.supportedOperationSets.put(
                OperationSetFileTransfer.class.getName(),
                mockFileTransferOpSet);
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @param listener a param.
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @return a String describing this mock protocol.
     */
    public String getProtocolName()
    {
        return PROTO_NAME;
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @return a String describing this mock protocol.
     */
    public String getProtocolDisplayName()
    {
        return PROTO_NAME;
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @return a Registered RegistrationState.
     */
    public RegistrationState getRegistrationState()
    {
        return RegistrationState.REGISTERED;
    }

    /**
     * Returns an array containing all operation sets supported by the
     * current implementation.
     *
     * @return a java.util.Map containing instance of all supported
     *   operation sets mapped against their class names (e.g.
     *   OperationSetPresence.class.getName()) .
     */
    public Map<String, OperationSet> getSupportedOperationSets()
    {
        return this.supportedOperationSets;
    }

    /**
     * Returns a collection containing all operation sets classes supported by
     * the current implementation. When querying this method users must be
     * prepared to receive any subset of the OperationSet-s defined by this
     * service. They MUST ignore any OperationSet-s that they are not aware of
     * and that may be defined by future versions of this service. Such
     * "unknown" OperationSet-s though not encouraged, may also be defined by
     * service implementors.
     *
     * @return a {@link Collection} containing instances of all supported
     * operation set classes (e.g. <tt>OperationSetPresence.class</tt>.
     */
    @SuppressWarnings("unchecked")
    public Collection<Class<? extends OperationSet>>
                                            getSupportedOperationSetClasses()
    {
        Collection<Class<? extends OperationSet>> opSetClasses
            = new ArrayList<Class<? extends OperationSet>>();

        Iterator<String> opSets
            = getSupportedOperationSets().keySet().iterator();

        while (opSets.hasNext())
        {
            String opSetClassName = opSets.next();
            try
            {
                opSetClasses.add(
                    (Class<? extends OperationSet>) getSupportedOperationSets()
                        .get(opSetClassName).getClass().getClassLoader()
                            .loadClass(opSetClassName));
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        return opSetClasses;
    }

    /**
     * Returns the operation set corresponding to the specified class or null
     * if this operation set is not supported by the provider implementation.
     *
     * @param opsetClass the <tt>Class</tt>  of the operation set that we're
     * looking for.
     * @return returns an OperationSet of the specified <tt>Class</tt> if the
     * undelying implementation supports it or null otherwise.
     */
    @SuppressWarnings("unchecked")
    public <T extends OperationSet> T getOperationSet(Class<T> opsetClass)
    {
        return (T) getSupportedOperationSets().get(opsetClass.getName());
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @return always true.
     */
    public boolean isRegistered()
    {
        return true;
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @param authority a dummy param
     */
    public void register(SecurityAuthority authority)
    {

    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @param listener a dummy param.
     */
    public void removeRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {

    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     */
    public void shutdown()
    {
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     */
    public void unregister()
    {
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     */
    public void unregister(boolean userRequest)
        throws OperationFailedException
    {
        this.unregister();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.ProtocolProviderService#
     * isSignallingTransportSecure()
     */
    public boolean isSignalingTransportSecure()
    {
        return false;
    }

    /**
     * Returns the "transport" protocol of this instance used to carry the
     * control channel for the current protocol service.
     *
     * @return The "transport" protocol of this instance: UNKNOWN.
     */
    public TransportProtocol getTransportProtocol()
    {
        return TransportProtocol.UNKNOWN;
    }

    /**
     * Returns the AccountID that uniquely identifies the account represented by
     * this instance of the ProtocolProviderService.
     * @return the id of the account represented by this provider.
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     * We have no icon corresponding to this protocol provider.
     */
    public ProtocolIcon getProtocolIcon()
    {
        return null;
    }
}
