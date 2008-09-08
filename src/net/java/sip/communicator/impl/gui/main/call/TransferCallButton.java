/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents an UI means to transfer (the <code>Call</code> of) an associated
 * <code>CallPariticant</code>.
 * 
 * @author Lubomir Marinov
 */
public class TransferCallButton
    extends JButton
{
    private static final Logger logger =
        Logger.getLogger(TransferCallButton.class);

    /**
     * The <code>CallParticipant</code> (whose <code>Call</code> is) to be
     * transfered.
     */
    private final CallParticipant callParticipant;

    /**
     * Initializes a new <code>TransferCallButton</code> instance which is to
     * transfer (the <code>Call</code> of) a specific
     * <code>CallParticipant</code>.
     * 
     * @param callParticipant the <code>CallParticipant</code> to be associated
     *            with the new instance and to be transfered
     */
    public TransferCallButton(CallParticipant callParticipant)
    {
        super(new ImageIcon(ImageLoader
            .getImage(ImageLoader.TRANSFER_CALL_BUTTON)));

        this.callParticipant = callParticipant;

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
        final Call call = callParticipant.getCall();

        if (call != null)
        {
            OperationSetAdvancedTelephony telephony =
                (OperationSetAdvancedTelephony) call.getProtocolProvider()
                    .getOperationSet(OperationSetAdvancedTelephony.class);

            if (telephony != null)
            {
                final TransferCallDialog dialog =
                    new TransferCallDialog(getFrame());

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
                        telephony.transfer(callParticipant, target);
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
     * Gets the first <code>Frame</code> in the ancestor <code>Component</code>
     * hierarchy of this button.
     * <p>
     * The located <code>Frame</code> (if any) is used as the owner of
     * <code>Dialog</code>s opened by this button in order to provide natural
     * <code>Frame</code> ownership.
     * </p>
     * 
     * @return the first <code>Frame</code> in the ancestor
     *         <code>Component</code> hierarchy of this button; <tt>null</tt>,
     *         if no such <code>Frame</code> was located
     */
    private Frame getFrame()
    {
        for (Component component = this; component != null;)
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
