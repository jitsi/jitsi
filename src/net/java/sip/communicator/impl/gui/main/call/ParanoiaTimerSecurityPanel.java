/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import javax.swing.*;
import java.awt.*;
import java.text.*;
import java.util.*;
import java.util.Timer;

/**
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class ParanoiaTimerSecurityPanel
    extends SecurityPanel<SrtpControl>
{
    /**
     * The security timer.
     */
    private Timer timer = new Timer(true);

    /**
     * Creates an instance of this <tt>ParanoiaTimerSecurityPanel</tt>.
     */
    ParanoiaTimerSecurityPanel()
    {
        super(null);

        initComponents();
    }

    /**
     * Initializes contained components.
     */
    private void initComponents()
    {
        final SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        final Calendar c = Calendar.getInstance();
        final JLabel counter = new JLabel();

        counter.setForeground(Color.red);
        counter.setFont(counter.getFont().deriveFont(
            (float)(counter.getFont().getSize() + 5)));

        setLayout(new GridBagLayout());
        setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints constraints = new GridBagConstraints();

        JLabel messageLabel = new JLabel(GuiActivator.getResources()
            .getI18NString("service.gui.security.SECURITY_ALERT"));

        messageLabel.setForeground(Color.WHITE);

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(messageLabel, constraints);

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 1;
        add(counter, constraints);

        ZrtpControl zrtpControl = null;
        if (securityControl instanceof ZrtpControl)
            zrtpControl = (ZrtpControl) securityControl;

        int initialSeconds = 0;

        if (zrtpControl != null)
            initialSeconds = (int) zrtpControl.getTimeoutValue()/1000;

        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, initialSeconds);

        counter.setText(format.format(c.getTime()));

        if (initialSeconds > 0)
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    c.add(Calendar.SECOND, -1);
                    counter.setText(format.format(c.getTime()));
                }
            }, 0, 1000);
    }

    /**
     * Cancels the security timer.
     *
     * @param evt the security event of which we're notified
     */
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        timer.cancel();
    }

    /**
     * Nothing to do here.
     *
     * @param evt the security event of which we're notified
     */
    public void securityOff(CallPeerSecurityOffEvent evt) {}

    /**
     * Indicates that the security is time-outed, is not supported by the
     * other end.
     * @param evt Details about the event that caused this message.
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent evt)
    {
        timer.cancel();

        // fail peer, call
        if(evt.getSource() instanceof AbstractCallPeer)
        {
            AbstractCallPeer peer = (AbstractCallPeer)evt.getSource();
            peer.setState(CallPeerState.FAILED, "Encryption Required!");
        }
    }

    public void loadSkin()
    {
    }
}
