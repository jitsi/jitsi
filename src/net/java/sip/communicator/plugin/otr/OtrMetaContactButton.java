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
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * 
 * @author George Politis
 * 
 */
@SuppressWarnings("serial")
public class OtrMetaContactButton
    extends SIPCommButton
    implements PluginComponent
{

    private Container container;

    public OtrMetaContactButton(Container container)
    {
        super(null, null);
        this.setEnabled(false);
        this.setPreferredSize(new Dimension(25, 25));
        this.container = container;

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
        
        this.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (contact == null)
                    return;

                switch (OtrActivator.scOtrEngine.getSessionStatus(contact))
                {
                case ENCRYPTED:
                case FINISHED:
                    // Default action for finished and encrypted sessions is end session.
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

    public Object getComponent()
    {
        return this;
    }

    public String getConstraints()
    {
        return null;
    }

    public Container getContainer()
    {
        return this.container;
    }

    public int getPositionIndex()
    {
        return -1;
    }

    public boolean isNativeComponent()
    {
        return false;
    }

    public void setCurrentContact(MetaContact metaContact)
    {
        contact = metaContact.getDefaultContact();
        this.setStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
        this.setPolicy(OtrActivator.scOtrEngine.getContactPolicy(contact));
    }

    private void setPolicy(OtrPolicy contactPolicy)
    {
        this.setEnabled(contactPolicy.getEnableManual());
    }

    private Contact contact;

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {

    }

    private void setStatus(SessionStatus status)
    {
        if (contact == null)
            return;

        switch (status)
        {
        case ENCRYPTED:
            try
            {
                this
                    .setImage(ImageIO
                        .read(OtrActivator.resourceService
                            .getImageURL((OtrActivator.scOtrKeyManager
                                .isVerified(contact))
                                ? "plugin.otr.ENCRYPTED_ICON_22x22"
                                : "plugin.otr.ENCRYPTED_UNVERIFIED_ICON_22x22")));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            break;
        case FINISHED:
            try
            {
                this.setImage(ImageIO.read(OtrActivator.resourceService
                    .getImageURL("plugin.otr.FINISHED_ICON_22x22")));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            break;
        case PLAINTEXT:
            try
            {
                this.setImage(ImageIO.read(OtrActivator.resourceService
                    .getImageURL("plugin.otr.PLAINTEXT_ICON_22x22")));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            break;
        }
    }

}
