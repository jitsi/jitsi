/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import junit.framework.*;
import net.java.sip.communicator.service.gui.event.*;
import java.util.*;
import net.java.sip.communicator.service.protocol.event.*;


public class TestDefaultCallParticipant extends TestCase
{
    private DefaultCallParticipant defaultCallParticipant = null;
    private CallParticipantChangeEvent lastCpChangeEvent = null;
    private class CpAdapter implements CallParticipantListener{

        public void participantChange(CallParticipantChangeEvent evt){
            lastCpChangeEvent = evt;
        }
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        defaultCallParticipant = new DefaultCallParticipant();
    }

    protected void tearDown() throws Exception
    {
        defaultCallParticipant = null;
        lastCpChangeEvent = null;
        super.tearDown();
    }

    /**
     * Test whether the call state is properly changed, the currentStateStartDate
     * is properly updated, and the corresponding event properly dispatched.
     */
    public void testEnterStateAndEventDispatch()
    {

        CallParticipantState oldState = defaultCallParticipant.getState();
        CallParticipantState newState = CallParticipantState.CONNECTED;

        CpAdapter adapter = new CpAdapter();
        defaultCallParticipant.addCallParticipantListener(adapter);

        long timeBeforeChange = System.currentTimeMillis();
        defaultCallParticipant.enterState(newState);
        long timeAfterChange = System.currentTimeMillis();

        //check status change
        assertEquals(newState, defaultCallParticipant.getState());

        //check event dispatch
        assertNotNull(lastCpChangeEvent);
        assertEquals(lastCpChangeEvent.getOldValue(), oldState);
        assertEquals(lastCpChangeEvent.getNewValue(), newState);

        //check the date
        long currentStateStartDate =
            defaultCallParticipant.getCurrentStateStartDate().getTime();
        assertTrue("currentStateStart date is not properly updated",
                   currentStateStartDate >= timeBeforeChange
                   && currentStateStartDate <= timeAfterChange);

        //remove the listener
        defaultCallParticipant.removeCallParticipantListener(adapter);
        lastCpChangeEvent = null;
        defaultCallParticipant.enterState(CallParticipantState.DISCONNECTED);

        assertNull("a listener was not properly removed", lastCpChangeEvent);

    }


    public void testSetAddress()
    {
        String expectedReturn = "sip:abc@def.ghi";
        String secondChange = "sip:jkl@mno.pqr";
        defaultCallParticipant.setAddress(expectedReturn);
        defaultCallParticipant.addCallParticipantListener(new CpAdapter());
        String actualReturn = defaultCallParticipant.getAddress();
        assertEquals("address getter or setter fails", expectedReturn, actualReturn);

        //check event dispatch
        defaultCallParticipant.setAddress(secondChange);
        assertNotNull("setAddress did not trigger an event", lastCpChangeEvent);

        assertEquals("setAddress triggered an event with the wrong type",
            lastCpChangeEvent.getEventType(),
            CallParticipantChangeEvent.CALL_PARTICIPANT_ADDRESS_CHANGE
            );

        assertEquals("event triggerred by setAddress had a bad oldValue",
                     lastCpChangeEvent.getOldValue(),
                     expectedReturn
            );

        assertEquals("event triggerred by setAddress had a bad newValue",
                     lastCpChangeEvent.getNewValue(),
                     secondChange
            );
    }

    public void testSetDisplayName()
    {
        String expectedReturn = "Rachel Weisz";
        String secondChange = "Keanu Reeves";
        defaultCallParticipant.setDisplayName(expectedReturn);
        String actualReturn = defaultCallParticipant.getDisplayName();
        assertEquals("DisplayName setter or getter failed", expectedReturn, actualReturn);

        //check event dispatch
        defaultCallParticipant.addCallParticipantListener(new CpAdapter());
        defaultCallParticipant.setDisplayName(secondChange);
        assertNotNull("setDisplayName did not trigger an event", lastCpChangeEvent);

        assertEquals("setDisplayName triggered an event with the wrong type",
            lastCpChangeEvent.getEventType(),
            CallParticipantChangeEvent.CALL_PARTICIPANT_DISPLAY_NAME_CHANGE
            );

        assertEquals("event triggerred by setDisplayName had a bad oldValue",
                     lastCpChangeEvent.getOldValue(),
                     expectedReturn
            );

        assertEquals("event triggerred by setDisplayName had a bad newValue",
                     lastCpChangeEvent.getNewValue(),
                     secondChange
            );
    }
//
//    private class DummyCall extends Call
//    {
//        public DummyCall()
//        {
//            super("identifier0123456789");
//        }
//    };

    public void testSetCallID()
    {
//        Call expectedReturn = new DummyCall();
//        defaultCallParticipant.setCall(expectedReturn);
//        Call actualReturn = defaultCallParticipant.getCall();
//        assertEquals("Call setter or getter (or Call.equals() method) failed",
//                     expectedReturn, actualReturn);

    }

    public void testSetImage()
    {
        byte[] expectedReturn = new byte[]{0, 1, 2, 3, 4, 5};
        defaultCallParticipant.setImage(expectedReturn);
        byte[] actualReturn = defaultCallParticipant.getImage();
        assertEquals("image gettter or setter failed", expectedReturn, actualReturn);

        //check event dispatch
        defaultCallParticipant.addCallParticipantListener(new CpAdapter());
        byte[] secondChange = new byte[]{5, 6, 7, 8, 9};
        defaultCallParticipant.setImage(secondChange);
        assertNotNull("setImage did not trigger an event", lastCpChangeEvent);

        assertEquals("setImage triggered an event with the wrong type",
            lastCpChangeEvent.getEventType(),
            CallParticipantChangeEvent.CALL_PARTICIPANT_IMAGE_CHANGE
            );

        assertEquals("event triggerred by setImage had a bad oldValue",
                     lastCpChangeEvent.getOldValue(),
                     expectedReturn
            );

        assertEquals("event triggerred by setImage had a bad newValue",
                     lastCpChangeEvent.getNewValue(),
                     secondChange
            );
    }

    public void testGetParticipantID()
    {
        String expectedReturn = "1234567890";
        defaultCallParticipant.setParticipantID(expectedReturn);
        String actualReturn = defaultCallParticipant.getParticipantID();
        assertEquals("participantID getter or setter failed", expectedReturn, actualReturn);
    }

    public void testIsCaller()
    {
        boolean expectedReturn = true;
        defaultCallParticipant.setIsCaller(expectedReturn);
        boolean actualReturn = defaultCallParticipant.isCaller();
        assertEquals("isCaller getter or setter failed", expectedReturn, actualReturn);
    }

    public void testToString()
    {
        String expectedReturn = "Emil Ivov <sip:emcho@sip.com>;status=Connected";
        DefaultCallParticipant dcp = new DefaultCallParticipant();
        dcp.setDisplayName("Emil Ivov");
        dcp.setAddress("sip:emcho@sip.com");
        dcp.enterState(CallParticipantState.CONNECTED);
        assertEquals("toString failed", expectedReturn, dcp.toString());
    }

}
