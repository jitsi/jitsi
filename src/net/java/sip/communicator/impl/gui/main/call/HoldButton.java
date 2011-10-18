/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Represents an UI means to put an associated <tt>CallPariticant</tt> on/off
 * hold.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 * @author Dmitri Melnikov
 */
public class HoldButton
    extends AbstractCallToggleButton
{
    /**
     * The <tt>Logger</tt> used by the <tt>HoldButton</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(HoldButton.class);

    /**
     * The serialization-related version of the <tt>HoldButton</tt> class
     * explicitly defined to silence a related warning (e.g. in Eclipse IDE)
     * since the <tt>HoldButton</tt> class does not add instance fields.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>Call</tt> on/off hold.
     *
     * @param call the <tt>Call</tt> to be associated with the new instance and
     * to be put on/off hold upon performing its action
     */
    public HoldButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>CallPeer</tt> on/off hold.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * to be put on/off hold upon performing its action
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public HoldButton(Call call, boolean fullScreen, boolean selected)
    {
        super(  call,
                fullScreen,
                true,
                selected,
                ImageLoader.HOLD_BUTTON,
                ImageLoader.HOLD_BUTTON_PRESSED,
                "service.gui.HOLD_BUTTON_TOOL_TIP");
    }

    /**
     * Holds on or off the associated <tt>Call</tt> when this button is clicked.
     */
    public void buttonPressed()
    {
        if (call != null)
        {
            Iterator<? extends CallPeer> peers = call.getCallPeers();
            boolean on = isSelected();
            OperationSetBasicTelephony<?> telephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            while (peers.hasNext())
            {
                CallPeer callPeer = peers.next();

                try
                {
                    if (on)
                        telephony.putOnHold(callPeer);
                    else
                        telephony.putOffHold(callPeer);
                }
                catch (OperationFailedException ofex)
                {
                    logger.error(
                            "Failed to put " + (on ? "on" : "off") + " hold.",
                            ofex);
                }
            }
        }
    }
}
