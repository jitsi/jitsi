/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.Window;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;

/**
 * Implements <code>ExportedWindow</code> for the purposes of delaying the
 * initialization of an associated <code>AddContactWizard</code> instance
 * because it is slow.
 * 
 * @author Lubomir Marinov
 */
public class AddContactWizardExportedWindow
    implements ExportedWindow
{

    /**
     * The argument required by the <code>AddContactWizard</code> constructor.
     */
    private final MainFrame mainFrame;

    /**
     * The <code>AddContactWizard</code> adapted by this instance.
     */
    private Window wizard;

    /**
     * Initializes a new <code>AddContactWizardExportedWindow</code> which is to
     * delay the initialization of a <code>AddContactWizard</code> with a
     * specific <code>MainFrame</code>.
     * 
     * @param mainFrame the argument required by the
     *            <code>AddContactWizard</code> constructor
     */
    public AddContactWizardExportedWindow(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;
    }

    public void bringToFront()
    {
        getWizard().toFront();
    }

    public WindowID getIdentifier()
    {
        return ExportedWindow.ADD_CONTACT_WINDOW;
    }

    public Object getSource()
    {
        return getWizard();
    }

    /**
     * Gets the <code>AddContactWizard</code> being adapted by this instance and
     * creates it if it hasn't already been created.
     * 
     * @return the <code>AddContactWizard</code> adapted by this instance
     */
    private Window getWizard()
    {
        if (wizard == null)
        {
            wizard = new AddContactWizard(mainFrame);
        }
        return wizard;
    }

    public boolean isFocused()
    {
        return getWizard().isFocused();
    }

    public boolean isVisible()
    {
        return getWizard().isVisible();
    }

    /**
     * Does nothing because the dialog associated with this instance doesn't
     * support maximizing.
     */
    public void maximize()
    {
        // The dialog cannot be maximized.
    }

    /**
     * Does nothing because the dialog associated with this instance doesn't
     * support minimizing.
     */
    public void minimize()
    {
        // The dialog cannot be minimized.
    }

    public void setLocation(int x, int y)
    {
        getWizard().setLocation(x, y);
    }

    public void setSize(int width, int height)
    {
        getWizard().setSize(width, height);
    }

    public void setVisible(boolean isVisible)
    {
        getWizard().setVisible(isVisible);
    }

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams) {}
}
