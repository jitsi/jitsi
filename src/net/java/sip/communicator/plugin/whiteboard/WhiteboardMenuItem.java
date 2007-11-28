/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.whiteboard;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;

/**
 * WhiteboardMenuItem
 * 
 * @author Julien Waechter
 */
public class WhiteboardMenuItem
    extends JMenuItem
    implements  ContactAwareComponent,
                ActionListener
{
    /**
     * The current meta contact
     */
    private MetaContact metaContact;

    /**
     * The Whiteboard session manager
     */
    private WhiteboardSessionManager session;

    /**
     * WhiteboardMenuItem constructor.
     *
     * @param session the whiteboard session manager
     */
    public WhiteboardMenuItem (WhiteboardSessionManager session)
    {
        super ("Whiteboard plugin");
        this.session = session;
        this.addActionListener (this);
        this.setIcon (Resources.getImage ("mpenIcon"));
    }

    /**
     * Sets the current meta contact.
     *
     * @param metaContact the current meta contact
     */
    public void setCurrentContact (MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }

    /**
     * Sets the current meta group.
     *
     * @param metaGroup the current meta contact group
     */
    public void setCurrentContactGroup (MetaContactGroup metaGroup)
    {
    }

    /**
     * Invoked when an action occurs: user start a whiteboard session.
     * 
     * @param e event
     */
    public void actionPerformed (ActionEvent e)
    {
        session.initWhiteboard (this.metaContact);
    }
}