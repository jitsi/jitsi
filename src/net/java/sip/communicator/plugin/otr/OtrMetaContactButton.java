/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.imageio.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * A {@link AbstractPluginComponent} that registers the Off-the-Record button in
 * the main chat toolbar.
 * 
 * @author George Politis
 */
public class OtrMetaContactButton
    extends AbstractPluginComponent
{
    private SIPCommButton button;

    private Contact contact;

    private final ScOtrEngineListener scOtrEngineListener =
        new ScOtrEngineListener()
        {
            public void sessionStatusChanged(Contact contact)
            {
                // OtrMetaContactButton.this.contact can be null.
                if (contact.equals(OtrMetaContactButton.this.contact))
                {
                    setStatus(
                        OtrActivator.scOtrEngine.getSessionStatus(contact));
                }
            }

            public void contactPolicyChanged(Contact contact)
            {
                // OtrMetaContactButton.this.contact can be null.
                if (contact.equals(OtrMetaContactButton.this.contact))
                {
                    setPolicy(
                        OtrActivator.scOtrEngine.getContactPolicy(contact));
                }
            }

            public void globalPolicyChanged()
            {
                if (OtrMetaContactButton.this.contact != null)
                    setPolicy(
                        OtrActivator.scOtrEngine.getContactPolicy(contact));
            }
        };

    private final ScOtrKeyManagerListener scOtrKeyManagerListener =
        new ScOtrKeyManagerListener()
        {
            public void contactVerificationStatusChanged(Contact contact)
            {
                // OtrMetaContactButton.this.contact can be null.
                if (contact.equals(OtrMetaContactButton.this.contact))
                {
                    setStatus(
                        OtrActivator.scOtrEngine.getSessionStatus(contact));
                }
            }
        };

    public OtrMetaContactButton(Container container)
    {
        super(container);

        OtrActivator.scOtrEngine.addListener(scOtrEngineListener);
        OtrActivator.scOtrKeyManager.addListener(scOtrKeyManagerListener);
    }

    void dispose()
    {
        OtrActivator.scOtrEngine.removeListener(scOtrEngineListener);
        OtrActivator.scOtrKeyManager.removeListener(scOtrKeyManagerListener);
    }

    /**
     * Gets the <code>SIPCommButton</code> which is the component of this
     * plugin. If the button doesn't exist, it's created.
     * 
     * @return the <code>SIPCommButton</code> which is the component of this
     *         plugin
     */
    private SIPCommButton getButton()
    {
        if (button == null)
        {
            button = new SIPCommButton(null, null);
            button.setEnabled(false);
            button.setPreferredSize(new Dimension(25, 25));

            button.setToolTipText(OtrActivator.resourceService.getI18NString(
                "plugin.otr.menu.OTR_TOOLTIP"));

            button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (contact == null)
                        return;

                    switch (OtrActivator.scOtrEngine.getSessionStatus(contact))
                    {
                    case ENCRYPTED:
                    case FINISHED:
                        // Default action for finished and encrypted sessions is
                        // end session.
                        OtrActivator.scOtrEngine.endSession(contact);
                        break;
                    case PLAINTEXT:
                        // Default action for finished and plaintext sessions is
                        // start session.
                        OtrActivator.scOtrEngine.startSession(contact);
                        break;
                    }
                }
            });
        }
        return button;
    }

    /*
     * Implements PluginComponent#getComponent(). Returns the SIPCommButton
     * which is the component of this plugin creating it first if it doesn't
     * exist.
     */
    public Object getComponent()
    {
        return getButton();
    }

    /*
     * Implements PluginComponent#getName().
     */
    public String getName()
    {
        return "";
    }

    /*
     * Implements PluginComponent#setCurrentContact(Contact).
     */
    public void setCurrentContact(Contact contact)
    {
        if (this.contact == contact)
            return;

        this.contact = contact;
        if (contact != null)
        {
            this.setStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
            this.setPolicy(OtrActivator.scOtrEngine.getContactPolicy(contact));
        }
        else
        {
            this.setStatus(SessionStatus.PLAINTEXT);
            this.setPolicy(null);
        }
    }

    /*
     * Implements PluginComponent#setCurrentContact(MetaContact).
     */
    public void setCurrentContact(MetaContact metaContact)
    {
        setCurrentContact((metaContact == null) ? null : metaContact
            .getDefaultContact());
    }

    /**
     * Sets the button enabled status according to the passed in
     * {@link OtrPolicy}.
     * 
     * @param otrPolicy the {@link OtrPolicy}.
     */
    private void setPolicy(OtrPolicy contactPolicy)
    {
        getButton().setEnabled(
            contactPolicy != null && contactPolicy.getEnableManual());
    }

    /**
     * Sets the button icon according to the passed in {@link SessionStatus}.
     * 
     * @param otrPolicy the {@link SessionStatus}.
     */
    private void setStatus(SessionStatus status)
    {
        String urlKey;
        switch (status)
        {
        case ENCRYPTED:
            urlKey
                = OtrActivator.scOtrKeyManager.isVerified(contact)
                    ? "plugin.otr.ENCRYPTED_ICON_22x22"
                    : "plugin.otr.ENCRYPTED_UNVERIFIED_ICON_22x22";
            break;
        case FINISHED:
            urlKey = "plugin.otr.FINISHED_ICON_22x22";
            break;
        case PLAINTEXT:
            urlKey = "plugin.otr.PLAINTEXT_ICON_22x22";
            break;
        default:
            return;
        }

        try
        {
            getButton().setImage(
                ImageIO.read(OtrActivator.resourceService.getImageURL(urlKey)));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
