/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import junit.framework.*;


/**
 * @author Emil Ivov
 */
public class TestCallParticipantState extends TestCase
{
    private CallParticipantState callParticipantState = null;

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testGetStateString()
    {
        String expectedReturn = CallParticipantState._BUSY;
        String actualReturn = CallParticipantState.BUSY.getStateString();

        assertEquals("the BUSY state has an invalid string repr."
                     ,expectedReturn, actualReturn);

        expectedReturn = CallParticipantState._INCOMING_CALL;
        actualReturn = CallParticipantState.INCOMING_CALL.getStateString();
        assertEquals("the CALLING state has an invalid string repr."
                     ,expectedReturn, actualReturn);

        expectedReturn = CallParticipantState._UNKNOWN;
        actualReturn = CallParticipantState.UNKNOWN.getStateString();
        assertEquals("the UNKNOWN state has an invalid string repr."
                     ,expectedReturn, actualReturn);

        expectedReturn = CallParticipantState._CONNECTED;
        actualReturn = CallParticipantState.CONNECTED.getStateString();
        assertEquals("the CONNECTED state has an invalid string repr."
                     ,expectedReturn, actualReturn);

        expectedReturn = CallParticipantState._CONNECTING;
        actualReturn = CallParticipantState.CONNECTING.getStateString();
        assertEquals("the CONNECTING state has an invalid string repr."
                     ,expectedReturn, actualReturn);

        expectedReturn = CallParticipantState._DISCONNECTED;
        actualReturn = CallParticipantState.DISCONNECTED.getStateString();
        assertEquals("the DISCONNECTED state has an invalid string repr."
                     ,expectedReturn, actualReturn);

        expectedReturn = CallParticipantState._FAILED;
        actualReturn = CallParticipantState.FAILED.getStateString();
        assertEquals("the FAILED state has an invalid string repr."
                     ,expectedReturn, actualReturn);

        expectedReturn = CallParticipantState._ALERTING_REMOTE_SIDE;
        actualReturn = CallParticipantState.ALERTING_REMOTE_SIDE.getStateString();
        assertEquals("the RINGING state has an invalid string repr."
                     ,expectedReturn, actualReturn);

        expectedReturn = CallParticipantState._ON_HOLD;
        actualReturn = CallParticipantState.ON_HOLD.getStateString();
        assertEquals("the ON_HOLD state has an invalid string repr."
                     ,expectedReturn, actualReturn);

    }
}
