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
package net.java.sip.communicator.slick.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
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

    private JabberSlickFixture fixture = new JabberSlickFixture();

    private OperationSetPresence opSetPresence1 = null;
    private OperationSetPresence opSetPresence2 = null;

    private OperationSetFileTransfer opSetFT1 = null;
    private OperationSetFileTransfer opSetFT2 = null;

    private static Contact contact1 = null;
    private static Contact contact2 = null;

    @Override
    public Contact getContact1()
    {
        if(contact1 == null)
        {
            contact1 = opSetPresence1.findContactByID(fixture.userID2);
        }

        return contact1;
    }

    @Override
    public Contact getContact2()
    {
        if(contact2 == null)
        {
            contact2 = opSetPresence2.findContactByID(fixture.userID1);
        }

        return contact2;
    }

    @Override
    public void start()
        throws Exception
    {
        fixture.setUp();

        Map<String, OperationSet> supportedOperationSets1 =
            fixture.provider1.getSupportedOperationSets();

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

        Map<String, OperationSet> supportedOperationSets2 =
            fixture.provider2.getSupportedOperationSets();

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

        prepareContactList();
    }

    @Override
    public void stop()
        throws Exception
    {
        fixture.tearDown();
    }

    /**
     * Create the list to be sure that contacts exchanging messages
     * exists in each other lists
     * @throws Exception
     */
    public void prepareContactList()
        throws Exception
    {
        // clear the provider the first time we run a filetransfer list
        if(getContact1() == null && getContact2() == null)
            fixture.clearProvidersLists();

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
                opSetPresence1.subscribe(fixture.userID2);
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
                opSetPresence2.subscribe(fixture.userID1);
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
        return JabberSlickFixture.bc;
    }

    @Override
    public boolean enableTestSendAndReceive()
    {
        return true;
    }

    /**
     * Its not implemented and its not available in jabber.
     * @return
     */
    @Override
    public boolean enableTestSenderCancelBeforeAccepted()
    {
        return false;
    }

    @Override
    public boolean enableTestReceiverDecline()
    {
        return true;
    }

    /**
     * It deleivers Failed event to receiver but it must deliver
     * canceled.
     * @return
     */
    @Override
    public boolean enableTestReceiverCancelsWhileTransfering()
    {
        return false;
    }

    @Override
    public boolean enableTestSenderCancelsWhileTransfering()
    {
        return true;
    }
}
