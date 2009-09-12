/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.login;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;

/**
 * Implements <code>ExportedWindow</code> for <code>AuthenticationWindow</code>
 * in order to delay its creation and spare memory and execution time on statup.
 * 
 * @author Lubomir Marinov
 */
public class AuthenticationExportedWindow
    extends AbstractExportedWindow<AuthenticationWindow>
{

    /**
     * The argument required by the constructor of
     * <code>AuthenticationWindow</code>.
     */
    private final MainFrame mainFrame;

    public AuthenticationExportedWindow(AuthenticationWindow window)
    {
        this.window = window;
        this.mainFrame = null;
    }

    public AuthenticationExportedWindow(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;
    }

    /*
     * Implements AbstractExportedWindow#createWindow().
     */
    public AuthenticationWindow createWindow()
    {
        return new AuthenticationWindow(mainFrame);
    }

    /*
     * Implements ExportedWindow#getIdentifier().
     */
    public WindowID getIdentifier()
    {
         return ExportedWindow.AUTHENTICATION_WINDOW;
    }

    /*
     * Overrides AbstractExportedWindow#setParams(Object[]). Delegates to
     * AuthenticationWindow.
     */
    public void setParams(Object[] windowParams)
    {
        getWindow().setParams(windowParams);
    }
}
