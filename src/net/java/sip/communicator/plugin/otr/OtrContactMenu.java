/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.event.*;

import javax.swing.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * A special {@link JMenu} that holds the menu items for controlling the
 * Off-the-Record functionality for a specific contact.
 *
 * @author George Politis
 * @author Lubomir Marinov
 */
@SuppressWarnings("serial")
class OtrContactMenu
    implements ActionListener,
               ScOtrEngineListener,
               ScOtrKeyManagerListener
{
    private static final String ACTION_COMMAND_AUTHENTICATE_BUDDY =
        "AUTHENTICATE_BUDDY";

    private static final String ACTION_COMMAND_CB_AUTO = "CB_AUTO";

    private static final String ACTION_COMMAND_CB_ENABLE = "CB_ENABLE";

    private static final String ACTION_COMMAND_CB_REQUIRE = "CB_REQUIRE";

    private static final String ACTION_COMMAND_CB_RESET = "CB_RESET";

    private static final String ACTION_COMMAND_END_OTR = "END_OTR";

    private static final String ACTION_COMMAND_REFRESH_OTR = "REFRESH_OTR";

    private static final String ACTION_COMMAND_START_OTR = "START_OTR";

    private final Contact contact;

    /**
     * The indicator which determines whether this <tt>JMenu</tt> is displayed
     * in the Mac OS X screen menu bar and thus should work around the known
     * problem of PopupMenuListener not being invoked.
     */
    private final boolean inMacOSXScreenMenuBar;

    /**
     * We keep this variable so we can determine if the policy has changed
     * or not in {@link OtrContactMenu#setOtrPolicy(OtrPolicy)}.
     */
    private OtrPolicy otrPolicy;

    private SessionStatus sessionStatus;

    private final JMenu parentMenu;

    private final boolean isSeparateMenu;

    private SIPCommMenu separateMenu;

    /**
     * The OtrContactMenu constructor.
     *
     * @param contact the Contact this menu refers to.
     * @param inMacOSXScreenMenuBar <tt>true</tt> if the new menu is to be
     * displayed in the Mac OS X screen menu bar; <tt>false</tt>, otherwise
     * @param menu the parent menu
     */
    public OtrContactMenu(  Contact contact,
                            boolean inMacOSXScreenMenuBar,
                            JMenu menu,
                            boolean isSeparateMenu)
    {
        this.contact = contact;
        this.inMacOSXScreenMenuBar = inMacOSXScreenMenuBar;
        this.parentMenu = menu;
        this.isSeparateMenu = isSeparateMenu;

        if (isSeparateMenu)
        {
            separateMenu = new SIPCommMenu(contact.getDisplayName());
        }

        OtrActivator.scOtrEngine.addListener(this);
        OtrActivator.scOtrKeyManager.addListener(this);

        setSessionStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
        setOtrPolicy(OtrActivator.scOtrEngine.getContactPolicy(contact));

        buildMenu();
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
            // Launch auth buddy dialog.
            OtrActionHandlers.openAuthDialog(contact);

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
            setOtrPolicy(OtrActivator.scOtrEngine.getContactPolicy(contact));
    }

    /*
     * Implements ScOtrKeyManagerListener#contactVerificationStatusChanged(
     * Contact).
     */
    public void contactVerificationStatusChanged(Contact contact)
    {
        if (contact.equals(OtrContactMenu.this.contact))
            setSessionStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
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

    /*
     * Implements ScOtrEngineListener#globalPolicyChanged().
     */
    public void globalPolicyChanged()
    {
        setOtrPolicy(OtrActivator.scOtrEngine
            .getContactPolicy(OtrContactMenu.this.contact));
    }

    /**
     * Rebuilds own menuitems according to {@link OtrContactMenu#sessionStatus}
     * and the {@link OtrPolicy} for {@link OtrContactMenu#contact}.
     */
    private void buildMenu()
    {
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

        switch (this.sessionStatus)
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

            if (isSeparateMenu)
            {
                separateMenu.add(endOtr);
                separateMenu.add(refreshOtr);
                separateMenu.add(authBuddy);
            }
            else
            {
                parentMenu.add(endOtr);
                parentMenu.add(refreshOtr);
                parentMenu.add(authBuddy);
            }

            break;

        case FINISHED:
            if (isSeparateMenu)
            {
                separateMenu.add(endOtr);
                separateMenu.add(startOtr);
            }
            else
            {
                parentMenu.add(endOtr);
                parentMenu.add(startOtr);
            }
            break;

        case PLAINTEXT:
            if (isSeparateMenu)
                separateMenu.add(startOtr);
            else
                parentMenu.add(startOtr);

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

        if (isSeparateMenu)
        {
            separateMenu.addSeparator();
            separateMenu.add(cbEnable);
            separateMenu.add(cbAlways);
            separateMenu.add(cbRequire);
            separateMenu.addSeparator();
            separateMenu.add(cbReset);

            parentMenu.add(separateMenu);
        }
        else
        {
            parentMenu.addSeparator();
            parentMenu.add(cbEnable);
            parentMenu.add(cbAlways);
            parentMenu.add(cbRequire);
            parentMenu.addSeparator();
            parentMenu.add(cbReset);
        }
    }

    /*
     * Implements ScOtrEngineListener#sessionStatusChanged(Contact).
     */
    public void sessionStatusChanged(Contact contact)
    {
        if (contact.equals(OtrContactMenu.this.contact))
            setSessionStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
    }

    /**
     * Sets the {@link OtrContactMenu#sessionStatus} value, updates the menu
     * icon and, if necessary, rebuilds the menuitems to match the passed in
     * sessionStatus.
     *
     * @param sessionStatus the {@link SessionStatus}.
     */
    private void setSessionStatus(SessionStatus sessionStatus)
    {
        if (sessionStatus != this.sessionStatus)
        {
            this.sessionStatus = sessionStatus;

            if (separateMenu != null)
            {
                updateIcon();

                if (separateMenu.isPopupMenuVisible() || inMacOSXScreenMenuBar)
                    buildMenu();
            }
        }
    }

    /**
     * Sets the {@link OtrContactMenu#otrPolicy} and, if necessary, rebuilds the
     * menuitems to match the passed in otrPolicy.
     *
     * @param otrPolicy
     */
    private void setOtrPolicy(OtrPolicy otrPolicy)
    {
        if (!otrPolicy.equals(this.otrPolicy))
        {
            this.otrPolicy = otrPolicy;

            if (separateMenu != null
                && (separateMenu.isPopupMenuVisible() || inMacOSXScreenMenuBar))
                buildMenu();
        }
    }

    /**
     * Updates the menu icon based on {@link OtrContactMenu#sessionStatus}
     * value.
     */
    private void updateIcon()
    {
        if (!isSeparateMenu)
            return;

        String imageID;

        switch (sessionStatus)
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

        separateMenu.setIcon(OtrActivator.resourceService.getImage(imageID));
    }
}
