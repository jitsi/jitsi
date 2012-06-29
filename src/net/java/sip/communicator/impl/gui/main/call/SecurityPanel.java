/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.service.neomedia.*;

/**
 * Base class for security panels that show encryption specific UI controls.
 * 
 * @author Ingo Bauersachs
 */
public abstract class SecurityPanel<T extends SrtpControl>
    extends FadeInBalloonPanel
    implements Skinnable
{
    /**
     * The currently used security control.
     */
    protected T securityControl;

    /**
     * Create security panel using the security control.
     * @param securityControl
     */
    public SecurityPanel(T securityControl)
    {
        this.securityControl = securityControl;
    }

    /**
     * The currently used security control.
     * @return
     */
    public T getSecurityControl()
    {
        return securityControl;
    }

    /**
     * The currently used security control.
     * @return
     */
    public void setSecurityControl(T securityControl)
    {
        this.securityControl = securityControl;
    }

    /**
     * Creates the security panel depending on the concrete implementation of
     * the passed security controller.
     * 
     * @param srtpControl the security controller that provides the information
     *            to be shown on the UI
     * @return An instance of a {@link SecurityPanel} for the security
     *         controller or an {@link TransparentPanel} if the controller is
     *         unknown or does not have any controls to show.
     */
    public static SecurityPanel create( CallPeerRenderer peerRenderer,
                                        CallPeer callPeer,
                                        SrtpControl srtpControl)
    {
        if(srtpControl instanceof ZrtpControl)
            return new ZrtpSecurityPanel(   peerRenderer,
                                            callPeer,
                                            (ZrtpControl)srtpControl);

        return new SecurityPanel<SrtpControl>(srtpControl)
        {
            public void loadSkin()
            {}

            public void securityOn(CallPeerSecurityOnEvent evt)
            {}

            public void securityOff(CallPeerSecurityOffEvent evt)
            {}

            public void securityTimeout(CallPeerSecurityTimeoutEvent evt)
            {}
        };
    }

    /**
     * Indicates that the security is turned on.
     *
     * @param evt details about the event that caused this message.
     */
    public abstract void securityOn(CallPeerSecurityOnEvent evt);

    /**
     * Indicates that the security is turned off.
     *
     * @param evt details about the event that caused this message.
     */
    public abstract void securityOff(CallPeerSecurityOffEvent evt);
    
    /**
     * Indicates that the security is timeouted, is not supported by the
     * other end.
     * @param evt Details about the event that caused this message.
     */
    public abstract void securityTimeout(CallPeerSecurityTimeoutEvent evt);
}
