/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
 * @author George Politis
 */
public class OtrMetaContactButton
    extends AbstractPluginComponent
{
    private SIPCommButton button;

    private Contact contact;

    public OtrMetaContactButton(Container container)
    {
        super(container);

        OtrActivator.scOtrEngine.addListener(new ScOtrEngineListener()
        {
            public void sessionStatusChanged(Contact contact)
            {
                // OtrMetaContactButton.this.contact can be null.
                if (contact.equals(OtrMetaContactButton.this.contact))
                {
                    setStatus(OtrActivator.scOtrEngine
                        .getSessionStatus(contact));
                }
            }

            public void contactPolicyChanged(Contact contact)
            {
                // OtrMetaContactButton.this.contact can be null.
                if (contact.equals(OtrMetaContactButton.this.contact))
                {
                    setPolicy(OtrActivator.scOtrEngine
                        .getContactPolicy(contact));
                }
            }

            public void globalPolicyChanged()
            {
                if (OtrMetaContactButton.this.contact != null)
                    setPolicy(OtrActivator.scOtrEngine
                        .getContactPolicy(contact));
            }
        });

        OtrActivator.scOtrKeyManager.addListener(new ScOtrKeyManagerListener()
        {
            public void contactVerificationStatusChanged(Contact contact)
            {
                // OtrMetaContactButton.this.contact can be null.
                if (contact.equals(OtrMetaContactButton.this.contact))
                {
                    setStatus(OtrActivator.scOtrEngine
                        .getSessionStatus(contact));
                }
            }
        });
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

    public void setCurrentContact(Contact contact)
    {
        if (this.contact != contact)
        {
            this.contact = contact;

            this.setStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
            this.setPolicy(OtrActivator.scOtrEngine.getContactPolicy(contact));
        }
    }

    public void setCurrentContact(MetaContact metaContact)
    {
        /*
         * TODO What if metaContact is null? Does it mean that this.contact
         * should become null?
         */
        if (metaContact != null)
        {
            Contact defaultContact = metaContact.getDefaultContact();

            if (defaultContact != null)
                setCurrentContact(defaultContact);
        }
    }

    private void setPolicy(OtrPolicy contactPolicy)
    {
        getButton().setEnabled(contactPolicy.getEnableManual());
    }

    private void setStatus(SessionStatus status)
    {
        if (contact == null)
            return;

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
            getButton()
                .setImage(
                    ImageIO.read(
                        OtrActivator.resourceService.getImageURL(urlKey)));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
