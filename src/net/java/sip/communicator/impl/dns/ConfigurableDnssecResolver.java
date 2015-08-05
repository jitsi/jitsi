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
package net.java.sip.communicator.impl.dns;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.xbill.DNS.*;

/**
 * Resolver that wraps a DNSSEC capable resolver and handles validation
 * failures according to the user's settings.
 *
 * @author Ingo Bauersachs
 */
public class ConfigurableDnssecResolver
    extends UnboundResolver
{
    private final static Logger logger
        = Logger.getLogger(ConfigurableDnssecResolver.class);

    /**
     * Name of the property that defines the default DNSSEC validation
     * behavior.
     */
    public final static String PNAME_DNSSEC_VALIDATION_MODE
        = "net.java.sip.communicator.util.dns.DNSSEC_VALIDATION_MODE";

    /**
     * Default value of {@link #PNAME_DNSSEC_VALIDATION_MODE}
     */
    public final static String PNAME_BASE_DNSSEC_PIN
        = "net.java.sip.communicator.util.dns.pin";

    final static String EVENT_TYPE = "DNSSEC_NOTIFICATION";

    private ConfigurationService config
        = DnsUtilActivator.getConfigurationService();
    private ResourceManagementService R
        = DnsUtilActivator.getResources();
    private Map<String, Date> lastNotifications
        = new HashMap<String, Date>();

    /**
     * Creates a new instance of this class. Tries to use the system's
     * default forwarders.
     */
    public ConfigurableDnssecResolver()
    {
        super();
        reset();
        Lookup.setDefaultResolver(this);

        DnsUtilActivator.getNotificationService().
            registerDefaultNotificationForEvent(
                ConfigurableDnssecResolver.EVENT_TYPE,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null, null);
    }

    /**
     * Inspects a DNS answer message and handles validation results according to
     * the user's preferences.
     *
     * @throws DnssecRuntimeException when the validation failed and the user
     *             did not choose to ignore it.
     */
    @Override
    protected void validateMessage(SecureMessage msg)
        throws DnssecRuntimeException
    {
        //---------------------------------------------------------------------
        //               ||  1   |    2     |      3       |    4     |    5
        //---------------------------------------------------------------------
        //   Sec. | Bog. || Ign. | Sec.Only | Sec.Or.Unsig | Warn.Bog | WarnAll
        //---------------------------------------------------------------------
        //a)  1   |  0   ||  ok  |    ok    |      ok      |    ok    |    ok
        //b)  0   |  1   ||  ok  |   nok    |     nok      |   ask    |   ask
        //c)  0   |  0   ||  ok  |   nok    |      ok      |    ok    |   ask
        //---------------------------------------------------------------------

        String fqdn = msg.getQuestion().getName().toString();
        String type = Type.string(msg.getQuestion().getType());
        String propName = createPropNameUnsigned(fqdn, type);
        SecureResolveMode defaultAction = Enum.valueOf(SecureResolveMode.class,
            config.getString(
                PNAME_DNSSEC_VALIDATION_MODE,
                SecureResolveMode.WarnIfBogus.name()
            )
        );
        SecureResolveMode pinned = Enum.valueOf(SecureResolveMode.class,
            config.getString(
                propName,
                defaultAction.name()
            )
        );

        //create default entry
        if(pinned == defaultAction)
            config.setProperty(propName, pinned.name());

        //check domain policy

        //[abc]1, a[2-5]
        if(pinned == SecureResolveMode.IgnoreDnssec || msg.isSecure())
            return;

        if(
            //b2, c2
            (pinned == SecureResolveMode.SecureOnly && !msg.isSecure())
            ||
            //b3
            (pinned == SecureResolveMode.SecureOrUnsigned && msg.isBogus())
        )
        {
            String text = getExceptionMessage(msg);
            Date last = lastNotifications.get(text);
            if(last == null
                //wait at least 5 minutes before showing the same info again
                || last.before(new Date(new Date().getTime() - 1000*60*5)))
            {
                DnsUtilActivator.getNotificationService().fireNotification(
                        EVENT_TYPE,
                        R.getI18NString("util.dns.INSECURE_ANSWER_TITLE"),
                        text,
                        null);
                lastNotifications.put(text, new Date());
            }
            throw new DnssecRuntimeException(text);
        }

        //c3
        if(pinned == SecureResolveMode.SecureOrUnsigned && !msg.isBogus())
            return;

        //c4
        if(pinned == SecureResolveMode.WarnIfBogus && !msg.isBogus())
            return;

        //b4, b5, c5
        String reason = msg.isBogus()
                ? R.getI18NString("util.dns.DNSSEC_ADVANCED_REASON_BOGUS",
                    new String[]{fqdn, msg.getBogusReason()})
                : R.getI18NString("util.dns.DNSSEC_ADVANCED_REASON_UNSIGNED",
                    new String[]{type, fqdn});
        DnssecDialog dlg = new DnssecDialog(fqdn, reason);
        dlg.setVisible(true);
        DnssecDialogResult result = dlg.getResult();
        switch(result)
        {
            case Accept:
                break;
            case Deny:
                throw new DnssecRuntimeException(getExceptionMessage(msg));
            case AlwaysAccept:
                if(msg.isBogus())
                    config.setProperty(propName,
                        SecureResolveMode.IgnoreDnssec.name());
                else
                    config.setProperty(propName,
                        SecureResolveMode.WarnIfBogus.name());
                break;
            case AlwaysDeny:
                config.setProperty(propName, SecureResolveMode.SecureOnly);
                throw new DnssecRuntimeException(getExceptionMessage(msg));
        }
    }

    /**
     * Defines the return code from the DNSSEC verification dialog.
     */
    private enum DnssecDialogResult
    {
        /** The DNS result shall be accepted. */
        Accept,
        /** The result shall be rejected. */
        Deny,
        /** The result shall be accepted permanently. */
        AlwaysAccept,
        /**
         * The result shall be rejected automatically unless it is valid
         * according to DNSSEC.
         */
        AlwaysDeny
    }

    /**
     * Dialog to ask and warn the user if he wants to continue to accept an
     * invalid dnssec result.
     */
    private class DnssecDialog extends SIPCommDialog implements ActionListener
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        //UI controls
        private JPanel pnlAdvanced;
        private JPanel pnlStandard;
        private final String domain;
        private final String reason;
        private JButton cmdAck;
        private JButton cmdShowDetails;

        //state
        private DnssecDialogResult result = DnssecDialogResult.Deny;

        /**
         * Creates a new instance of this class.
         * @param domain The FQDN of the domain that failed.
         * @param reason String describing why the validation failed.
         */
        public DnssecDialog(String domain, String reason)
        {
            super(false);
            setModal(true);
            this.domain = domain;
            this.reason = reason;
            initComponents();
        }

        /**
         * Creates the UI controls
         */
        private void initComponents()
        {
            setLayout(new BorderLayout(15, 15));
            setTitle(R.getI18NString("util.dns.INSECURE_ANSWER_TITLE"));

            // warning text
            JLabel imgWarning =
                new JLabel(R.getImage("service.gui.icons.WARNING_ICON"));
            imgWarning.setBorder(BorderFactory
                .createEmptyBorder(10, 10, 10, 10));
            add(imgWarning, BorderLayout.WEST);
            JLabel lblWarning = new JLabel(
                R.getI18NString("util.dns.DNSSEC_WARNING", new String[]{
                    R.getSettingsString("service.gui.APPLICATION_NAME"),
                    domain
                })
            );
            add(lblWarning, BorderLayout.CENTER);

            //standard panel (deny option)
            cmdAck = new JButton(R.getI18NString("service.gui.OK"));
            cmdAck.addActionListener(this);

            cmdShowDetails = new JButton(
                R.getI18NString("util.dns.DNSSEC_ADVANCED_OPTIONS"));
            cmdShowDetails.addActionListener(this);

            pnlStandard = new TransparentPanel(new BorderLayout());
            pnlStandard.setBorder(BorderFactory
                .createEmptyBorder(10, 10, 10, 10));
            pnlStandard.add(cmdShowDetails, BorderLayout.WEST);
            pnlStandard.add(cmdAck, BorderLayout.EAST);
            add(pnlStandard, BorderLayout.SOUTH);

            //advanced panel
            pnlAdvanced = new TransparentPanel(new BorderLayout());
            JPanel pnlAdvancedButtons = new TransparentPanel(
                new FlowLayout(FlowLayout.RIGHT));
            pnlAdvancedButtons.setBorder(BorderFactory
                .createEmptyBorder(10, 10, 10, 10));
            pnlAdvanced.add(pnlAdvancedButtons, BorderLayout.EAST);
            for(DnssecDialogResult r : DnssecDialogResult.values())
            {
                JButton cmd = new JButton(R.getI18NString(
                    "net.java.sip.communicator.util.dns."
                    + "ConfigurableDnssecResolver$DnssecDialogResult."
                    + r.name()));
                cmd.setActionCommand(r.name());
                cmd.addActionListener(this);
                pnlAdvancedButtons.add(cmd);
            }
            JLabel lblReason = new JLabel(reason);
            lblReason.setBorder(BorderFactory
                .createEmptyBorder(10, 10, 10, 10));
            pnlAdvanced.add(lblReason, BorderLayout.NORTH);
        }

        /**
         * Handles the events coming from the buttons.
         */
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource() == cmdAck)
            {
                result = DnssecDialogResult.Deny;
                dispose();
            }
            else if(e.getSource() == cmdShowDetails)
            {
                getContentPane().remove(pnlStandard);
                add(pnlAdvanced, BorderLayout.SOUTH);
                pack();
            }
            else
            {
                result = Enum.valueOf(DnssecDialogResult.class,
                    e.getActionCommand());
                dispose();
            }
        }

        /**
         * Gets the option that user has chosen.
         * @return the option that user has chosen.
         */
        public DnssecDialogResult getResult()
        {
            return result;
        }
    }

    private String getExceptionMessage(SecureMessage msg)
    {
        return msg.getBogusReason() == null
            ? R.getI18NString(
                "util.dns.INSECURE_ANSWER_MESSAGE_NO_REASON",
                new String[]{msg.getQuestion().getName().toString()}
              )
            : R.getI18NString(
                "util.dns.INSECURE_ANSWER_MESSAGE_REASON",
                new String[]{msg.getQuestion().getName().toString(),
                    //TODO parse bogus reason text and translate
                    msg.getBogusReason()}
              );
    }

    private String createPropNameUnsigned(String fqdn, String type)
    {
        return PNAME_BASE_DNSSEC_PIN + "." + fqdn.replace(".", "__");
    }

    /**
     * Reloads the configuration of forwarders and trust anchors.
     */
    @Override
    public void reset()
    {
        String forwarders = DnsUtilActivator.getConfigurationService()
            .getString(DnsUtilActivator.PNAME_DNSSEC_NAMESERVERS);
        if(!StringUtils.isNullOrEmpty(forwarders, true))
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Setting DNSSEC forwarders to: "
                    + Arrays.toString(forwarders.split(",")));
            }
            super.setForwarders(forwarders.split(","));
        }

        for(int i = 1;;i++)
        {
            String anchor = DnsUtilActivator.getResources().getSettingsString(
                "net.java.sip.communicator.util.dns.DS_ROOT." + i);
            if(anchor == null)
                break;
            clearTrustAnchors();
            addTrustAnchor(anchor);
            if(logger.isTraceEnabled())
                logger.trace("Loaded trust anchor " + anchor);
        }
    }
}
