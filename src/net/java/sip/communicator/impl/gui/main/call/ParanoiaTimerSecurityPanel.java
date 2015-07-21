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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.text.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;

/**
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class ParanoiaTimerSecurityPanel<T extends SrtpControl>
    extends SecurityPanel<SrtpControl>
{
    /**
     * The security timer.
     */
    private Timer timer = new Timer(true);

    /**
     * Creates an instance of this <tt>ParanoiaTimerSecurityPanel</tt>.
     */
    ParanoiaTimerSecurityPanel(T securityControl)
    {
        super(securityControl);

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

        long initialSeconds = 0;

        if (zrtpControl != null)
            initialSeconds = zrtpControl.getTimeoutValue();

        c.setTimeInMillis(initialSeconds);

        counter.setText(format.format(c.getTime()));

        if (initialSeconds > 0)
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    if (c.getTimeInMillis() - 1000 > 0)
                    {
                        c.add(Calendar.SECOND, -1);
                        counter.setText(format.format(c.getTime()));
                    }
                }
            }, 1000, 1000);
    }

    /**
     * Cancels the security timer.
     *
     * @param evt the security event of which we're notified
     */
    @Override
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        timer.cancel();
    }

    /**
     * Nothing to do here.
     *
     * @param evt the security event of which we're notified
     */
    @Override
    public void securityOff(CallPeerSecurityOffEvent evt) {}

    /**
     * Indicates that the security is time-outed, is not supported by the
     * other end.
     * @param evt Details about the event that caused this message.
     */
    @Override
    public void securityTimeout(CallPeerSecurityTimeoutEvent evt)
    {
        timer.cancel();

        // fail peer, call
        if(evt.getSource() instanceof AbstractCallPeer)
        {
            try
            {
                CallPeer peer = (CallPeer) evt.getSource();
                OperationSetBasicTelephony<?> telephony
                    = peer.getProtocolProvider().getOperationSet(
                            OperationSetBasicTelephony.class);

                telephony.hangupCallPeer(
                    peer,
                    OperationSetBasicTelephony.HANGUP_REASON_ENCRYPTION_REQUIRED,
                    "Encryption Required!");
            }
            catch(OperationFailedException ex)
            {
                Logger.getLogger(getClass()).error("Failed to hangup peer", ex);
            }
        }
    }

    public void loadSkin()
    {
    }
}
