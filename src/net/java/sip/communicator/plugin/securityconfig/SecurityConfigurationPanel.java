/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.securityconfig;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.securityconfig.call.*;
import net.java.sip.communicator.plugin.securityconfig.chat.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The main security configuration form panel.
 *
 * @author Yana Stamcheva
 */
public class SecurityConfigurationPanel
    extends TransparentPanel
{
    /**
     * Creates the <tt>SecurityConfigurationPanel</tt>.
     */
    public SecurityConfigurationPanel()
    {
        super(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab(SecurityConfigActivator.getResources()
            .getI18NString("service.gui.CHAT"), new OtrConfigurationPanel());
        tabbedPane.addTab(SecurityConfigActivator.getResources()
            .getI18NString("service.gui.CALL"), new ZrtpConfigurePanel());

        add(tabbedPane);
    }
}