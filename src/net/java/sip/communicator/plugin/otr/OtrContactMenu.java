/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author George Politis
 * 
 */
@SuppressWarnings("serial")
class OtrContactMenu
    extends JMenu
{
    public OtrContactMenu(Contact contact)
    {
        this.contact = contact;
        this.setText(contact.getDisplayName());

        OtrActivator.scOtrEngine.addListener(new ScOtrEngineListener()
        {
            public void sessionStatusChanged(Contact contact)
            {
                SessionStatus status =
                    OtrActivator.scOtrEngine.getSessionStatus(contact);

                if (contact.equals(OtrContactMenu.this.contact))
                    setSessionStatus(status);
            }

            public void contactPolicyChanged(Contact contact)
            {
                // Update the corresponding to the contact menu.
                OtrPolicy policy =
                    OtrActivator.scOtrEngine.getContactPolicy(contact);

                if (contact.equals(OtrContactMenu.this.contact))
                    setOtrPolicy(policy);
            }

            public void globalPolicyChanged()
            {
                OtrPolicy policy =
                    OtrActivator.scOtrEngine
                        .getContactPolicy(OtrContactMenu.this.contact);

                setOtrPolicy(policy);
            }

            public void contactVerificationStatusChanged(Contact contact)
            {
                SessionStatus status =
                    OtrActivator.scOtrEngine.getSessionStatus(contact);

                if (contact.equals(OtrContactMenu.this.contact))
                    setSessionStatus(status);
            }
        });

        setSessionStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
        setOtrPolicy(OtrActivator.scOtrEngine.getContactPolicy(contact));
    }

    private SessionStatus sessionStatus;

    private OtrPolicy otrPolicy;

    public void rebuildMenu()
    {
        this.removeAll();

        OtrPolicy policy = OtrActivator.scOtrEngine.getContactPolicy(contact);

        JMenuItem endOtr = new JMenuItem();
        endOtr.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.END_OTR"));
        endOtr.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                // End session.
                OtrActivator.scOtrEngine.endSession(contact);
            }
        });

        JMenuItem startOtr = new JMenuItem();
        startOtr.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.START_OTR"));
        startOtr.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                // Start session.
                OtrActivator.scOtrEngine.startSession(contact);
            }
        });
        startOtr.setEnabled(policy.getEnableManual());

        switch (this.getSessionStatus())
        {
        case ENCRYPTED:
            this
                .setIcon(OtrActivator.resourceService
                    .getImage((OtrActivator.scOtrEngine
                        .isContactVerified(contact))
                        ? "plugin.otr.ENCRYPTED_ICON_16x16"
                        : "plugin.otr.ENCRYPTED_UNVERIFIED_ICON_16x16"));

            this.add(endOtr);

            JMenuItem refreshOtr = new JMenuItem();
            refreshOtr.setText(OtrActivator.resourceService
                .getI18NString("plugin.otr.menu.REFRESH_OTR"));
            refreshOtr.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    // Refresh session.
                    OtrActivator.scOtrEngine.refreshSession(contact);
                }
            });
            refreshOtr.setEnabled(policy.getEnableManual());
            this.add(refreshOtr);

            JMenuItem authBuddy = new JMenuItem();
            authBuddy.setText(OtrActivator.resourceService
                .getI18NString("plugin.otr.menu.AUTHENTICATE_BUDDY"));
            authBuddy.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    // Launch auth buddy dialog.
                    OtrBuddyAuthenticationDialog authenticateBuddyDialog =
                        new OtrBuddyAuthenticationDialog(contact);

                    authenticateBuddyDialog.setLocation(Toolkit
                        .getDefaultToolkit().getScreenSize().width
                        / 2 - authenticateBuddyDialog.getWidth() / 2, Toolkit
                        .getDefaultToolkit().getScreenSize().height
                        / 2 - authenticateBuddyDialog.getHeight() / 2);

                    authenticateBuddyDialog.setVisible(true);
                }
            });
            this.add(authBuddy);
            break;
        case FINISHED:
            this.setIcon(OtrActivator.resourceService
                .getImage("plugin.otr.FINISHED_ICON_16x16"));

            this.add(endOtr);
            this.add(startOtr);
            break;
        case PLAINTEXT:
            this.setIcon(OtrActivator.resourceService
                .getImage("plugin.otr.PLAINTEXT_ICON_16x16"));

            this.add(startOtr);
            break;
        }

        this.addSeparator();

        JCheckBoxMenuItem cbEnable = new JCheckBoxMenuItem();
        cbEnable.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.CB_ENABLE"));

        cbEnable.setState(policy.getEnableManual());

        cbEnable.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                OtrPolicy policy =
                    OtrActivator.scOtrEngine.getContactPolicy(contact);

                boolean state = ((JCheckBoxMenuItem) e.getSource()).getState();
                policy.setEnableManual(state);
                OtrActivator.scOtrEngine.setContactPolicy(contact, policy);
            }
        });
        this.add(cbEnable);

        JCheckBoxMenuItem cbAlways = new JCheckBoxMenuItem();
        cbAlways.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.CB_AUTO"));

        cbAlways.setEnabled(policy.getEnableManual());
        cbAlways.setState(policy.getEnableAlways());

        cbAlways.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                OtrPolicy policy =
                    OtrActivator.scOtrEngine.getContactPolicy(contact);

                boolean state = ((JCheckBoxMenuItem) e.getSource()).getState();
                policy.setEnableAlways(state);
                OtrActivator.scOtrEngine.setContactPolicy(contact, policy);
            }
        });
        this.add(cbAlways);

        JCheckBoxMenuItem cbRequire = new JCheckBoxMenuItem();
        cbRequire.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.CB_REQUIRE"));

        cbRequire.setEnabled(policy.getEnableManual());
        cbRequire.setState(policy.getRequireEncryption());

        cbRequire.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                OtrPolicy policy =
                    OtrActivator.scOtrEngine.getContactPolicy(contact);

                boolean state = ((JCheckBoxMenuItem) e.getSource()).getState();
                policy.setEnableAlways(state);
                OtrActivator.scOtrEngine.setContactPolicy(contact, policy);
            }
        });
        this.add(cbRequire);

        this.addSeparator();

        JCheckBoxMenuItem cbReset = new JCheckBoxMenuItem();
        cbReset.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.CB_RESET"));

        cbReset.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                OtrActivator.scOtrEngine.setContactPolicy(contact, null);
            }
        });
        this.add(cbReset);
    }

    public void setSessionStatus(SessionStatus sessionStatus)
    {
        if (sessionStatus == this.sessionStatus)
            return;

        this.sessionStatus = sessionStatus;
        this.rebuildMenu();
    }

    public SessionStatus getSessionStatus()
    {
        return sessionStatus;
    }

    public void setOtrPolicy(OtrPolicy otrPolicy)
    {
        if (otrPolicy.equals(this.otrPolicy))
            return;

        this.otrPolicy = otrPolicy;
        this.rebuildMenu();
    }

    public OtrPolicy getOtrPolicy()
    {
        return otrPolicy;
    }

    public Contact contact;
}
