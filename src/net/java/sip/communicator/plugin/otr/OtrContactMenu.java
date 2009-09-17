/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * @author George Politis
 * @author Lubomir Marinov
 */
@SuppressWarnings("serial")
class OtrContactMenu
    extends JMenu
    implements ActionListener,
               PopupMenuListener,
               ScOtrEngineListener,
               ScOtrKeyManagerListener
{
    private static final String ACTION_COMMAND_AUTHENTICATE_BUDDY
        = "AUTHENTICATE_BUDDY";

    private static final String ACTION_COMMAND_CB_AUTO = "CB_AUTO";

    private static final String ACTION_COMMAND_CB_ENABLE = "CB_ENABLE";

    private static final String ACTION_COMMAND_CB_REQUIRE = "CB_REQUIRE";

    private static final String ACTION_COMMAND_CB_RESET = "CB_RESET";

    private static final String ACTION_COMMAND_END_OTR = "END_OTR";

    private static final String ACTION_COMMAND_REFRESH_OTR = "REFRESH_OTR";

    private static final String ACTION_COMMAND_START_OTR = "START_OTR";

    private final Contact contact;

    private OtrPolicy otrPolicy;

    private SessionStatus sessionStatus;

    public OtrContactMenu(Contact contact)
    {
        this.contact = contact;

        this.setText(contact.getDisplayName());

        /*
         * Setup populating this JMenu on demand because it's not always
         * necessary.
         */
        getPopupMenu().addPopupMenuListener(this);

        OtrActivator.scOtrEngine.addListener(this);
        OtrActivator.scOtrKeyManager.addListener(this);
        
        setSessionStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
        setOtrPolicy(OtrActivator.scOtrEngine.getContactPolicy(contact));
    }

    /*
     * Implements ActionListener#actionPerformed(ActionEvent).
     */
    public void actionPerformed(ActionEvent e)
    {
        String actionCommand = e.getActionCommand();

        if (ACTION_COMMAND_END_OTR.equals(actionCommand))
            // End session.
            OtrActivator.scOtrEngine.endSession(contact);

        else if (ACTION_COMMAND_START_OTR.equals(actionCommand))
            // Start session.
            OtrActivator.scOtrEngine.startSession(contact);

        else if (ACTION_COMMAND_REFRESH_OTR.equals(actionCommand))
            // Refresh session.
            OtrActivator.scOtrEngine.refreshSession(contact);

        else if (ACTION_COMMAND_AUTHENTICATE_BUDDY.equals(actionCommand))
        {
            // Launch auth buddy dialog.
            OtrBuddyAuthenticationDialog authenticateBuddyDialog
                = new OtrBuddyAuthenticationDialog(contact);
            Dimension screenSize
                = Toolkit.getDefaultToolkit().getScreenSize();

            authenticateBuddyDialog.setLocation(
                    screenSize.width / 2
                        - authenticateBuddyDialog.getWidth() / 2,
                    screenSize.height / 2
                        - authenticateBuddyDialog.getHeight() / 2);
            authenticateBuddyDialog.setVisible(true);
        }

        else if (ACTION_COMMAND_CB_ENABLE.equals(actionCommand))
        {
            OtrPolicy policy =
                OtrActivator.scOtrEngine.getContactPolicy(contact);
            boolean state = ((JCheckBoxMenuItem) e.getSource()).getState();

            policy.setEnableManual(state);
            OtrActivator.scOtrEngine.setContactPolicy(contact, policy);
        }

        else if (ACTION_COMMAND_CB_AUTO.equals(actionCommand))
        {
            OtrPolicy policy =
                OtrActivator.scOtrEngine.getContactPolicy(contact);
            boolean state = ((JCheckBoxMenuItem) e.getSource()).getState();

            policy.setEnableAlways(state);
            OtrActivator.scOtrEngine.setContactPolicy(contact, policy);
        }

        else if (ACTION_COMMAND_CB_REQUIRE.equals(actionCommand))
        {
            OtrPolicy policy =
                OtrActivator.scOtrEngine.getContactPolicy(contact);
            boolean state = ((JCheckBoxMenuItem) e.getSource()).getState();

            policy.setRequireEncryption(state);
            OtrActivator.scOtrEngine.setContactPolicy(contact, policy);
        }
        else if (ACTION_COMMAND_CB_RESET.equals(actionCommand))
            OtrActivator.scOtrEngine.setContactPolicy(contact, null);
    }

    /*
     * Implements ScOtrEngineListener#contactPolicyChanged(Contact).
     */
    public void contactPolicyChanged(Contact contact)
    {
        // Update the corresponding to the contact menu.
        if (contact.equals(OtrContactMenu.this.contact))
            setOtrPolicy(
                OtrActivator.scOtrEngine.getContactPolicy(contact));
    }

    /*
     * Implements ScOtrKeyManagerListener#contactVerificationStatusChanged(
     * Contact).
     */
    public void contactVerificationStatusChanged(Contact contact)
    {
        if (contact.equals(OtrContactMenu.this.contact))
            setSessionStatus(
                OtrActivator.scOtrEngine.getSessionStatus(contact));
    }

    /**
     * Disposes of this instance by making it available for garage collection
     * e.g. removes the listeners it has installed on global instances such as
     * <tt>OtrActivator#scOtrEngine</tt> and
     * <tt>OtrActivator#scOtrKeyManager</tt>.
     */
    void dispose()
    {
        OtrActivator.scOtrEngine.removeListener(this);
        OtrActivator.scOtrKeyManager.removeListener(this);
    }

    private SessionStatus getSessionStatus()
    {
        return sessionStatus;
    }

    /*
     * Implements ScOtrEngineListener#globalPolicyChanged().
     */
    public void globalPolicyChanged()
    {
        setOtrPolicy(
            OtrActivator.scOtrEngine
                    .getContactPolicy(OtrContactMenu.this.contact));
    }

    /*
     * Implements PopupMenuListener#popupMenuCanceled(PopupMenuEvent).
     */
    public void popupMenuCanceled(PopupMenuEvent e)
    {
    }

    /*
     * Implements PopupMenuListener#popupMenuWillBecomeInvisible(
     * PopupMenuEvent).
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
        popupMenuCanceled(e);
    }

    /*
     * Implements PopupMenuListener#popupMenuWillBecomeVisible(PopupMenuEvent).
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
    {
        rebuildMenu();
    }

    private void rebuildMenu()
    {
        this.removeAll();

        OtrPolicy policy = OtrActivator.scOtrEngine.getContactPolicy(contact);

        JMenuItem endOtr = new JMenuItem();
        endOtr.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.END_OTR"));
        endOtr.setActionCommand(ACTION_COMMAND_END_OTR);
        endOtr.addActionListener(this);

        JMenuItem startOtr = new JMenuItem();
        startOtr.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.START_OTR"));
        startOtr.setEnabled(policy.getEnableManual());
        startOtr.setActionCommand(ACTION_COMMAND_START_OTR);
        startOtr.addActionListener(this);

        switch (getSessionStatus())
        {
        case ENCRYPTED:
            JMenuItem refreshOtr = new JMenuItem();
            refreshOtr.setText(OtrActivator.resourceService
                .getI18NString("plugin.otr.menu.REFRESH_OTR"));
            refreshOtr.setEnabled(policy.getEnableManual());
            refreshOtr.setActionCommand(ACTION_COMMAND_REFRESH_OTR);
            refreshOtr.addActionListener(this);

            JMenuItem authBuddy = new JMenuItem();
            authBuddy.setText(OtrActivator.resourceService
                .getI18NString("plugin.otr.menu.AUTHENTICATE_BUDDY"));
            authBuddy.setActionCommand(ACTION_COMMAND_AUTHENTICATE_BUDDY);
            authBuddy.addActionListener(this);

            this.add(endOtr);
            this.add(refreshOtr);
            this.add(authBuddy);
            break;

        case FINISHED:
            this.add(endOtr);
            this.add(startOtr);
            break;

        case PLAINTEXT:
            this.add(startOtr);
            break;
        }

        JCheckBoxMenuItem cbEnable = new JCheckBoxMenuItem();
        cbEnable.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.CB_ENABLE"));
        cbEnable.setState(policy.getEnableManual());
        cbEnable.setActionCommand(ACTION_COMMAND_CB_ENABLE);
        cbEnable.addActionListener(this);

        JCheckBoxMenuItem cbAlways = new JCheckBoxMenuItem();
        cbAlways.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.CB_AUTO"));
        cbAlways.setEnabled(policy.getEnableManual());
        cbAlways.setState(policy.getEnableAlways());
        cbAlways.setActionCommand(ACTION_COMMAND_CB_AUTO);
        cbAlways.addActionListener(this);

        JCheckBoxMenuItem cbRequire = new JCheckBoxMenuItem();
        cbRequire.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.CB_REQUIRE"));
        cbRequire.setEnabled(policy.getEnableManual());
        cbRequire.setState(policy.getRequireEncryption());
        cbRequire.setActionCommand(ACTION_COMMAND_CB_REQUIRE);
        cbRequire.addActionListener(this);

        JMenuItem cbReset = new JMenuItem();
        cbReset.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.CB_RESET"));
        cbReset.setActionCommand(ACTION_COMMAND_CB_RESET);
        cbReset.addActionListener(this);

        this.addSeparator();
        this.add(cbEnable);
        this.add(cbAlways);
        this.add(cbRequire);
        this.addSeparator();
        this.add(cbReset);
    }

    /*
     * Implements ScOtrEngineListener#sessionStatusChanged(Contact).
     */
    public void sessionStatusChanged(Contact contact)
    {
        if (contact.equals(OtrContactMenu.this.contact))
            setSessionStatus(
                OtrActivator.scOtrEngine.getSessionStatus(contact));
    }

    private void setSessionStatus(SessionStatus sessionStatus)
    {
        if (sessionStatus != this.sessionStatus)
        {
            this.sessionStatus = sessionStatus;

            updateIcon();
            if (isPopupMenuVisible())
                rebuildMenu();
        }
    }

    private void setOtrPolicy(OtrPolicy otrPolicy)
    {
        if (!otrPolicy.equals(this.otrPolicy))
        {
            this.otrPolicy = otrPolicy;

            if (isPopupMenuVisible())
                rebuildMenu();
        }
    }

    private void updateIcon()
    {
        String imageID;

        switch (getSessionStatus())
        {
        case ENCRYPTED:
            imageID
                = OtrActivator.scOtrKeyManager.isVerified(contact)
                        ? "plugin.otr.ENCRYPTED_ICON_16x16"
                        : "plugin.otr.ENCRYPTED_UNVERIFIED_ICON_16x16";
            break;

        case FINISHED:
            imageID = "plugin.otr.FINISHED_ICON_16x16";
            break;

        case PLAINTEXT:
            imageID = "plugin.otr.PLAINTEXT_ICON_16x16";
            break;

        default:
            return;
        }

        setIcon(OtrActivator.resourceService.getImage(imageID));
    }
}
