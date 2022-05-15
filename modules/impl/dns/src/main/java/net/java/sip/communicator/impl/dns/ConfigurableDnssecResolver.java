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
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.apache.commons.lang3.StringUtils;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.xbill.DNS.*;
import org.xbill.DNS.dnssec.*;

/**
 * Resolver that wraps a DNSSEC capable resolver and handles validation
 * failures according to the user's settings.
 *
 * @author Ingo Bauersachs
 */
public class ConfigurableDnssecResolver
    implements CustomResolver
{
    private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigurableDnssecResolver.class);

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

    private final ConfigurationService config;
    private final ResourceManagementService R
        = DnsUtilActivator.getResources();
    private final Map<String, Date> lastNotifications = new HashMap<>();

    private final ValidatingResolver resolver;
    private final ExtendedResolver headResolver;

    /**
     * Creates a new instance of this class. Tries to use the system's
     * default forwarders.
     */
    public ConfigurableDnssecResolver(
        ConfigurationService configService,
        ExtendedResolver headResolver)
    {
        this.headResolver = headResolver;
        this.resolver = new ValidatingResolver(headResolver);
        this.config = configService;

        List<String> propNames
            = config.getPropertyNamesByPrefix("org.jitsi.dnssec", false);
        Properties config = new Properties();
        for (String propName : propNames)
        {
            String value = config.getProperty(propName);
            if (StringUtils.isNotEmpty(value))
            {
                config.put(propName, value);
            }
        }

        try
        {
            resolver.init(config);
        }
        catch (IOException e)
        {
            logger.error("Extended dnssec properties contained an error", e);
        }

        reset();
        Lookup.setDefaultResolver(this);

        DnsUtilActivator.getNotificationService().
            registerDefaultNotificationForEvent(
                ConfigurableDnssecResolver.EVENT_TYPE,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null, null);
    }

    @Override
    public void setPort(int port)
    {
        resolver.setPort(port);
    }

    @Override
    public void setTCP(boolean flag)
    {
        resolver.setTCP(flag);
    }

    @Override
    public void setIgnoreTruncation(boolean flag)
    {
        resolver.setIgnoreTruncation(flag);
    }

    @Override
    public void setEDNS(int version, int payloadSize, int flags, List<EDNSOption> options)
    {
        resolver.setEDNS(version, payloadSize, flags, options);
    }

    @Override
    public void setTSIGKey(TSIG key)
    {
        resolver.setTSIGKey(key);
    }

    @Override
    public void setTimeout(Duration timeout)
    {
        resolver.setTimeout(timeout);
    }

    /**
     * Inspects a DNS answer message and handles validation results according to
     * the user's preferences.
     *
     * @throws DnssecRuntimeException when the validation failed and the user
     *             did not choose to ignore it.
     */
    @Override
    public Message send(Message query)
        throws DnssecRuntimeException, IOException
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

        SecureMessage msg = new SecureMessage(resolver.send(query));
        String fqdn = msg.getQuestion().getName().toString();
        String type = Type.string(msg.getQuestion().getType());
        String propName = createPropNameUnsigned(fqdn);
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
            return msg;

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
            return msg;

        //c4
        if(pinned == SecureResolveMode.WarnIfBogus && !msg.isBogus())
            return msg;

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

        return msg;
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

    private String createPropNameUnsigned(String fqdn)
    {
        return PNAME_BASE_DNSSEC_PIN + "." + fqdn.replace(".", "__");
    }

    /**
     * Reloads the configuration of forwarders and trust anchors.
     */
    @Override
    public void reset()
    {
        String forwarders = config
            .getString(DnsUtilActivator.PNAME_DNSSEC_NAMESERVERS);
        if(StringUtils.isNotBlank(forwarders))
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Setting DNSSEC forwarders to: " + forwarders);
            }

            synchronized (Lookup.class)
            {
                Lookup.refreshDefault();
                String[] fwds = forwarders.split(",");
                Resolver[] rs = headResolver.getResolvers();
                for (Resolver r : rs)
                {
                    headResolver.deleteResolver(r);
                }

                for (String fwd : fwds)
                {
                    try
                    {
                        SimpleResolver sr = new SimpleResolver(fwd);

                        // these properties are normally set by the
                        // ValidatingResolver in the constructor
                        sr.setEDNS(0, 0, ExtendedFlags.DO);
                        sr.setIgnoreTruncation(false);
                        headResolver.addResolver(sr);
                    }
                    catch (UnknownHostException e)
                    {
                        logger.error("Invalid forwarder, ignoring", e);
                    }
                }

                Lookup.setDefaultResolver(this);
            }
        }

        StringBuilder sb = new StringBuilder();
        for(int i = 1;;i++)
        {
            String anchor = DnsUtilActivator.getResources().getSettingsString(
                "net.java.sip.communicator.util.dns.DS_ROOT." + i);
            if(anchor == null)
            {
                break;
            }

            sb.append(anchor);
            sb.append('\n');
        }

        try
        {
            resolver.loadTrustAnchors(new ByteArrayInputStream(
                sb.toString().getBytes(StandardCharsets.US_ASCII)));
        }
        catch (IOException e)
        {
            logger.error("Could not load the trust anchors", e);
        }

        logger.trace("Loaded trust anchors {}", sb);
    }
}
