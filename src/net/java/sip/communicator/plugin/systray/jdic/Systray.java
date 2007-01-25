/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.systray.jdic;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;

import org.jdesktop.jdic.tray.*;

public class Systray
{   
    private UIService uiService;
    
    private TrayIcon trayIcon;
        
    public Systray(UIService s)
    {    
        this.uiService = s;
        
        SystemTray tray = SystemTray.getDefaultSystemTray();
        
        ImageIcon logoIcon = new ImageIcon(
            Systray.class.getResource("systrayIcon.png"));

        trayIcon = new TrayIcon(
            logoIcon, "SIP Communicator", null);

        trayIcon.setIconAutoSize(false);
        trayIcon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(uiService.isVisible())
                {
                    uiService.minimize();
                }
                else
                {
                    uiService.restore();
                }
            }
        });
                
        tray.addTrayIcon(trayIcon);                
    }
}
