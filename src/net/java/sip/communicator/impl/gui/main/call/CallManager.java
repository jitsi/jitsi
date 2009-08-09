/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.text.*;
import java.util.*;

import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>CallManager</tt> is the one that handles calls. It contains also
 * the "Call" and "Hangup" buttons panel. Here are handles incoming and outgoing
 * calls from and to the call operation set.
 *
 * @author Yana Stamcheva
 */
public class CallManager
{
    private static final Logger logger = Logger.getLogger(CallManager.class);

    /**
     * A table mapping protocol <tt>Call</tt> objects to the GUI dialogs
     * that are currently used to display them.
     */
    private static Hashtable<Call, CallDialog> activeCalls
                                            = new Hashtable<Call, CallDialog>();

    public static class GuiCallListener implements CallListener
    {
        /**
         * Implements CallListener.incomingCallReceived. When a call is received
         * creates a <tt>ReceivedCallDialog</tt> and plays the
         * ring phone sound to the user.
         */
        public void incomingCallReceived(CallEvent event)
        {
            Call sourceCall = event.getSourceCall();

            ReceivedCallDialog receivedCallDialog
                = new ReceivedCallDialog(sourceCall);

            receivedCallDialog.pack();
            receivedCallDialog.setVisible(true);

            // FIXME: I18N
            NotificationManager.fireNotification(
                NotificationManager.INCOMING_CALL,
                "",
                "Incoming call received from: "
                    + sourceCall.getCallPeers().next());
        }

        /**
         * Implements CallListener.callEnded. Stops sounds that are playing at
         * the moment if there're any. Removes the call panel and disables the
         * hangup button.
         */
        public void callEnded(CallEvent event)
        {
            Call sourceCall = event.getSourceCall();

            NotificationManager.stopSound(NotificationManager.BUSY_CALL);
            NotificationManager.stopSound(NotificationManager.INCOMING_CALL);
            NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);

            if (activeCalls.get(sourceCall) != null)
            {
                CallDialog callDialog = activeCalls.get(sourceCall);

                disposeCallDialogWait(callDialog);
            }
        }

        /**
         * Creates and opens a call dialog. Implements
         * CallListener.outGoingCallCreated. .
         */
        public void outgoingCallCreated(CallEvent event)
        {
            Call sourceCall = event.getSourceCall();

            CallDialog callDialog
                = CallManager.openCallDialog(sourceCall,
                    GuiCallPeerRecord.OUTGOING_CALL);

            activeCalls.put(sourceCall, callDialog);
        }
    }

    /**
     * Removes the given call panel tab.
     *
     * @param callDialog the CallDialog to remove
     */
    public static void disposeCallDialogWait(CallDialog callDialog)
    {
        Timer timer
            = new Timer(5000, new DisposeCallDialogListener(callDialog));

        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Removes the given CallPanel from the main tabbed pane.
     */
    private static class DisposeCallDialogListener
        implements ActionListener
    {
        private CallDialog callDialog;

        public DisposeCallDialogListener(CallDialog callDialog)
        {
            this.callDialog = callDialog;
        }

        public void actionPerformed(ActionEvent e)
        {
            callDialog.dispose();

            Call call = callDialog.getCall();

            if(call != null && activeCalls.containsKey(call))
            {
                activeCalls.remove(call);
            }
        }
    }

    /**
     * Answers the given call.
     *
     * @param call the call to answer
     */
    public static void answerCall(final Call call)
    {
        CallManager.openCallDialog(call,
            GuiCallPeerRecord.INCOMING_CALL);

        new AnswerCallThread(call).start();
    }

    /**
     * Hangups the given call.
     *
     * @param call the call to answer
     */
    public static void hangupCall(final Call call)
    {
        NotificationManager.stopSound(NotificationManager.BUSY_CALL);
        NotificationManager.stopSound(NotificationManager.INCOMING_CALL);
        NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);

        new HangupCallThread(call).start();
    }

    /**
     * Creates a call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     */
    public static void createCall(  ProtocolProviderService protocolProvider,
                                    String contact)
    {
        new CreateCallThread(protocolProvider, contact).start();
    }

    /**
     * Creates a call to the given list of contacts.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contacts the list of contacts to call to
     */
    public static void createCall(  ProtocolProviderService protocolProvider,
                                    List<Contact> contacts)
    {
        new CreateCallThread(protocolProvider, contacts).start();
    }

    /**
     * Opens a call dialog.
     *
     * @param call the call object to pass to the call dialog
     * @param callType the call type
     */
    public static CallDialog openCallDialog(Call call, String callType)
    {
        CallDialog callDialog = new CallDialog(call, callType);

        callDialog.pack();

        callDialog.setVisible(true);

        return callDialog;
    }

    /**
     * Creates a call from a given Contact or a given String.
     */
    private static class CreateCallThread
        extends Thread
    {
        List<Contact> contacts;

        String stringContact;

        final ProtocolProviderService protocolProvider;

        public CreateCallThread(ProtocolProviderService protocolProvider,
                                String contact)
        {
            this.protocolProvider = protocolProvider;
            this.stringContact = contact;
        }

        public CreateCallThread(ProtocolProviderService protocolProvider,
                                List<Contact> contacts)
        {
            this.protocolProvider = protocolProvider;
            this.contacts = contacts;
        }

        public void run()
        {
            OperationSetBasicTelephony telephonyOpSet
                = (OperationSetBasicTelephony) protocolProvider
                    .getOperationSet(OperationSetBasicTelephony.class);

            if (telephonyOpSet == null)
                return;

            // NOTE: The multi user call is not yet implemented!
            // We just get the first contact and create a call for him.
            try
            {
                if (contacts != null)
                {
                    Contact contact = contacts.get(0);

                    telephonyOpSet.createCall(contact);
                }
                else
                {
                    telephonyOpSet.createCall(stringContact);
                }
            }
            catch (OperationFailedException e)
            {
                logger.error("The call could not be created: " + e);

                new ErrorDialog(null,
                    GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                    e.getMessage(),
                    ErrorDialog.ERROR).showDialog();
            }
            catch (ParseException e)
            {
                logger.error("The call could not be created: " + e);

                new ErrorDialog(null,
                    GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                    e.getMessage(),
                    ErrorDialog.ERROR).showDialog();
            }
        }
    }


    /**
     * Answers all call participants in the given call.
     */
    private static class AnswerCallThread
        extends Thread
    {
        private final Call call;

        public AnswerCallThread(Call call)
        {
            this.call = call;
        }

        public void run()
        {
            ProtocolProviderService pps = call.getProtocolProvider();
            Iterator<CallPeer> participants = call.getCallPeers();

            while (participants.hasNext())
            {
                CallPeer participant = participants.next();
                OperationSetBasicTelephony telephony =
                    (OperationSetBasicTelephony) pps
                        .getOperationSet(OperationSetBasicTelephony.class);

                try
                {
                    telephony.answerCallPeer(participant);
                }
                catch (OperationFailedException e)
                {
                    logger.error("Could not answer to : " + participant
                        + " caused by the following exception: " + e);
                }
            }
        }
    }

    /**
     * Hangups all call participants in the given call.
     */
    private static class HangupCallThread
        extends Thread
    {
        private final Call call;

        public HangupCallThread(Call call)
        {
            this.call = call;
        }

        public void run()
        {
            ProtocolProviderService pps = call.getProtocolProvider();
            Iterator<CallPeer> participants = call.getCallPeers();

            while (participants.hasNext())
            {
                CallPeer participant = participants.next();
                OperationSetBasicTelephony telephony =
                    (OperationSetBasicTelephony) pps
                        .getOperationSet(OperationSetBasicTelephony.class);

                try
                {
                    telephony.hangupCallPeer(participant);
                }
                catch (OperationFailedException e)
                {
                    logger.error("Could not answer to : " + participant
                        + " caused by the following exception: " + e);
                }
            }
        }
    }
}
