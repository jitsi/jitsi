/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

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
    extends AbstractExportedWindow<AddContactWizard>
{

    /**
     * The argument required by the <code>AddContactWizard</code> constructor.
     */
    private final MainFrame mainFrame;

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

    /*
     * Implements AbstractExportedWindow#createWindow().
     */
    protected AddContactWizard createWindow()
    {
        return new AddContactWizard(mainFrame);
    }

    public WindowID getIdentifier()
    {
        return ExportedWindow.ADD_CONTACT_WINDOW;
    }

    /**
     * Implements {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams)
    {
        String uin;
        if ((windowParams != null)
                && (windowParams.length > 0)
                && (windowParams[0] instanceof String))
            uin = (String) windowParams[0];
        else
            uin = "";

        getWindow().setUIN(uin);
    }
}
