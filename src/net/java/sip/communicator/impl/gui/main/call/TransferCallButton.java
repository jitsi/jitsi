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
 * Represents an UI means to transfer (the <code>Call</code> of) an associated
 * <code>CallPariticant</code>.
 *
 * @author Lubomir Marinov
 */
public class TransferCallButton
    extends SIPCommButton
{
    private static final Logger logger =
        Logger.getLogger(TransferCallButton.class);

    /**
     * The <code>CallPeer</code> (whose <code>Call</code> is) to be
     * transfered.
     */
    private final CallPeer callPeer;

    /**
     * Initializes a new <code>TransferCallButton</code> instance which is to
     * transfer (the <code>Call</code> of) a specific
     * <code>CallPeer</code>.
     *
     * @param callPeer the <code>CallPeer</code> to be associated
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
             * @param evt the <code>ActionEvent</code> instance containing the
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
     * <code>ActionListener</code>.
     *
     * @param listener the <code>ActionListener</code> notified about the
     *            performing of the action
     * @param evt the <code>ActionEvent</code> containing the data associated
     *            with the action and the act of its performing
     */
    private void actionPerformed(ActionListener listener, ActionEvent evt)
    {
        final Call call = callPeer.getCall();

        if (call != null)
        {
            OperationSetAdvancedTelephony telephony =
                (OperationSetAdvancedTelephony) call.getProtocolProvider()
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
                     * (non-Javadoc)
                     *
                     * @see net.java.sip.communicator.service.protocol.event.
                     * CallChangeAdapter
                     * #callStateChanged(net.java.sip.communicator
                     * .service.protocol.event.CallChangeEvent)
                     */
                    public void callStateChanged(CallChangeEvent evt)
                    {
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
                        CallPeer targetPeer =
                            findCallPeer(target);

                        if (targetPeer == null)
                        {
                            telephony.transfer(callPeer, target);
                        }
                        else
                        {
                            telephony.transfer(callPeer,
                                targetPeer);
                        }
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
     * Returns the first <code>CallPeer</code> known to a specific
     * <code>OperationSetBasicTelephony</code> to have a specific address.
     *
     * @param telephony the <code>OperationSetBasicTelephony</code> to have its
     *            <code>CallPeer</code>s examined in search for one which
     *            has a specific address
     * @param address the address to locate the associated
     *            <code>CallPeer</code> of
     */
    @SuppressWarnings("unchecked") //pfff ... well we can't make OpSetTelephony
    //return a non-raw type iterator as it would break descendants that are using
    //it with Call extensions. Someone would have to explain to me one day
    //why java complains in such cases.
    private CallPeer findCallPeer(
        OperationSetBasicTelephony telephony, String address)
    {
        for (Iterator<Call> callIter = telephony.getActiveCalls();
             callIter.hasNext();)
        {
            Call call = callIter.next();

            for (Iterator<CallPeer> peerIter =
                call.getCallPeers(); peerIter.hasNext();)
            {
                CallPeer peer = peerIter.next();

                if (address.equals(peer.getAddress()))
                {
                    return peer;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first <code>CallPeer</code> among all existing ones
     * who has a specific address.
     *
     * @param address the address of the <code>CallPeer</code> to be
     *            located
     * @return the first <code>CallPeer</code> among all existing ones
     *         who has the specified <code>address</code>
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
            OperationSetBasicTelephony telephony = (OperationSetBasicTelephony)
                service.getOperationSet(telephonyClass);

            if (telephony != null)
            {
                peer = findCallPeer(telephony, address);
                if (peer != null)
                {
                    break;
                }
            }
        }
        return peer;
    }

    /**
     * Gets the first <code>Frame</code> in the ancestor <code>Component</code>
     * hierarchy of a specific <code>Component</code>.
     * <p>
     * The located <code>Frame</code> (if any) is often used as the owner of
     * <code>Dialog</code>s opened by the specified <code>Component</code> in
     * order to provide natural <code>Frame</code> ownership.
     * </p>
     *
     * @return the first <code>Frame</code> in the ancestor
     *         <code>Component</code> hierarchy of the specified
     *         <code>Component</code>; <tt>null</tt>, if no such
     *         <code>Frame</code> was located
     */
    public static Frame getFrame(Component component)
    {
        while (component != null)
        {
            Container container = component.getParent();

            if (container instanceof Frame)
            {
                return (Frame) container;
            }
            component = container;
        }
        return null;
    }
}
