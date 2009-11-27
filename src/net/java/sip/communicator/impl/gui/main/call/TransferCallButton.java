/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Represents an UI means to transfer (the <tt>Call</tt> of) an associated
 * <tt>CallPariticant</tt>.
 *
 * @author Lubomir Marinov
 */
public class TransferCallButton
    extends SIPCommButton
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(TransferCallButton.class);

    /**
     * The <tt>CallPeer</tt> (whose <tt>Call</tt> is) to be
     * transfered.
     */
    private final CallPeer callPeer;

    /**
     * Initializes a new <tt>TransferCallButton</tt> instance which is to
     * transfer (the <tt>Call</tt> of) a specific
     * <tt>CallPeer</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> to be associated
     *            with the new instance and to be transfered
     */
    public TransferCallButton(CallPeer callPeer)
    {
        super(ImageLoader.getImage(ImageLoader.TRANSFER_CALL_BUTTON));

        this.callPeer = callPeer;

        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.TRANSFER_BUTTON_TOOL_TIP"));
        addActionListener(new ActionListener()
        {

            /**
             * Invoked when an action occurs.
             *
             * @param evt the <tt>ActionEvent</tt> instance containing the
             *            data associated with the action and the act of its
             *            performing
             */
            public void actionPerformed(ActionEvent evt)
            {
                TransferCallButton.this.actionPerformed(this, evt);
            }
        });
    }

    /**
     * Handles actions performed on this button on behalf of a specific
     * <tt>ActionListener</tt>.
     *
     * @param listener the <tt>ActionListener</tt> notified about the
     *            performing of the action
     * @param evt the <tt>ActionEvent</tt> containing the data associated
     *            with the action and the act of its performing
     */
    private void actionPerformed(ActionListener listener, ActionEvent evt)
    {
        final Call call = callPeer.getCall();

        if (call != null)
        {
            OperationSetAdvancedTelephony telephony =
                call.getProtocolProvider()
                    .getOperationSet(OperationSetAdvancedTelephony.class);

            if (telephony != null)
            {
                final TransferCallDialog dialog =
                    new TransferCallDialog(getFrame(this));

                /*
                 * Transferring a call works only when the call is in progress
                 * so close the dialog (if it's not already closed, of course)
                 * once the dialog ends.
                 */
                CallChangeListener callChangeListener = new CallChangeAdapter()
                {

                    /*
                     * Implements
                     * CallChangeAdapter#callStateChanged(CallChangeEvent).
                     */
                    public void callStateChanged(CallChangeEvent evt)
                    {
                        // we are interested only in CALL_STATE_CHANGEs
                        if(!evt.getEventType().equals(
                                CallChangeEvent.CALL_STATE_CHANGE))
                            return;

                        if (!CallState.CALL_IN_PROGRESS.equals(call
                            .getCallState()))
                        {
                            dialog.setVisible(false);
                            dialog.dispose();
                        }
                    }
                };
                call.addCallChangeListener(callChangeListener);
                try
                {
                    dialog.setModal(true);
                    dialog.pack();
                    dialog.setVisible(true);
                }
                finally
                {
                    call.removeCallChangeListener(callChangeListener);
                }

                String target = dialog.getTarget();
                if ((target != null) && (target.length() > 0))
                {
                    try
                    {
                        CallPeer targetPeer = findCallPeer(target);

                        if (targetPeer == null)
                            telephony.transfer(callPeer, target);
                        else
                            telephony.transfer(callPeer, targetPeer);
                    }
                    catch (OperationFailedException ex)
                    {
                        logger.error("Failed to transfer call " + call + " to "
                            + target, ex);
                    }
                }
            }
        }
    }

    /**
     * Returns the first <tt>CallPeer</tt> known to a specific
     * <tt>OperationSetBasicTelephony</tt> to have a specific address.
     *
     * @param telephony the <tt>OperationSetBasicTelephony</tt> to have its
     * <tt>CallPeer</tt>s examined in search for one which has a specific
     * address
     * @param address the address to locate the associated <tt>CallPeer</tt> of
     * @return the first <tt>CallPeer</tt> known to the specified
     * <tt>OperationSetBasicTelephony</tt> to have the specified address
     */
    private CallPeer findCallPeer(
        OperationSetBasicTelephony telephony, String address)
    {
        for (Iterator<? extends Call> callIter = telephony.getActiveCalls();
                callIter.hasNext();)
        {
            Call call = callIter.next();

            for (Iterator<? extends CallPeer> peerIter = call.getCallPeers();
                    peerIter.hasNext();)
            {
                CallPeer peer = peerIter.next();

                if (address.equals(peer.getAddress()))
                    return peer;
            }
        }
        return null;
    }

    /**
     * Returns the first <tt>CallPeer</tt> among all existing ones
     * who has a specific address.
     *
     * @param address the address of the <tt>CallPeer</tt> to be located
     * @return the first <tt>CallPeer</tt> among all existing ones
     * who has the specified <tt>address</tt>
     *
     * @throws OperationFailedException in case we fail retrieving a reference
     * to <tt>ProtocolProviderService</tt>s
     */
    private CallPeer findCallPeer(String address)
        throws OperationFailedException
    {
        BundleContext bundleContext = GuiActivator.bundleContext;
        ServiceReference[] serviceReferences;

        try
        {
            serviceReferences =
                bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            throw new OperationFailedException(
                "Failed to retrieve ProtocolProviderService references.",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        Class<OperationSetBasicTelephony> telephonyClass
            = OperationSetBasicTelephony.class;
        CallPeer peer = null;

        for (ServiceReference serviceReference : serviceReferences)
        {
            ProtocolProviderService service = (ProtocolProviderService)
                bundleContext.getService(serviceReference);
            OperationSetBasicTelephony telephony =
                service.getOperationSet(telephonyClass);

            if (telephony != null)
            {
                peer = findCallPeer(telephony, address);
                if (peer != null)
                    break;
            }
        }
        return peer;
    }

    /**
     * Gets the first <tt>Frame</tt> in the ancestor <tt>Component</tt>
     * hierarchy of a specific <tt>Component</tt>.
     * <p>
     * The located <tt>Frame</tt> (if any) is often used as the owner of
     * <tt>Dialog</tt>s opened by the specified <tt>Component</tt> in
     * order to provide natural <tt>Frame</tt> ownership.
     *
     * @param component the <tt>Component</tt> which is to have its
     * <tt>Component</tt> hierarchy examined for <tt>Frame</tt>
     * @return the first <tt>Frame</tt> in the ancestor
     * <tt>Component</tt> hierarchy of the specified <tt>Component</tt>;
     * <tt>null</tt>, if no such <tt>Frame</tt> was located
     */
    public static Frame getFrame(Component component)
    {
        while (component != null)
        {
            Container container = component.getParent();

            if (container instanceof Frame)
                return (Frame) container;

            component = container;
        }
        return null;
    }
}
