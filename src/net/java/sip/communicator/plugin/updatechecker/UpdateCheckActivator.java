/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.updatechecker;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;

import javax.swing.*;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import javax.swing.text.html.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.version.VersionService;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activates the UpdateCheck plugin
 * @author Damian Minkov
**/
public class UpdateCheckActivator
    implements BundleActivator
{

    private static Logger logger = Logger.getLogger(UpdateCheckActivator.class);

    private static BundleContext bundleContext = null;

    private static BrowserLauncherService browserLauncherService;
    
    private String downloadLink = null;

    /**
     * Starts this bundle 
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        try
        {
            logger.logEntry();
            this.bundleContext = bundleContext;
        }
        finally
        {
            logger.logExit();
        }
        
        ServiceReference serviceReference = bundleContext
            .getServiceReference(
                net.java.sip.communicator.service.version.VersionService.class.getName());

        VersionService verService = (VersionService) bundleContext
                .getService(serviceReference);
        
        net.java.sip.communicator.service.version.Version 
            ver = verService.getCurrentVersion();
        
        if(isNewestVersion(ver.toString()))
            return;
        
        final JDialog dialog = new JDialog();
        dialog.setTitle(Resources.getLangString("dialogTitle"));

        JEditorPane contentMessage = new JEditorPane();
        contentMessage.setContentType("text/html");
        contentMessage.setOpaque(false);
        contentMessage.setEditable(false);
        
        String dialogMsg = MessageFormat.format(
                Resources.getLangString("dialogMessage1"),
                ver.getApplicationName());
        
        if(downloadLink != null)
        {
            dialogMsg += Resources.getLangString("dialogMessage2") + 
                "<a href=\"" + downloadLink + "\">" + 
                downloadLink + "</a> </html>";
        }
        
        contentMessage.setText(dialogMsg);

        contentMessage.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e)
            {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                {
                    getBrowserLauncher().openURL(e.getDescription());
                }
            }
        });

        JPanel contentPane = new JPanel(new BorderLayout(5,5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(contentMessage, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                dialog.setVisible(false);
            }
        });
        
        buttonPanel.add(okButton);
        
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(contentPane);
        
        dialog.pack();
        
        dialog.setLocation(
            Toolkit.getDefaultToolkit().getScreenSize().width/2
                - dialog.getWidth()/2,
            Toolkit.getDefaultToolkit().getScreenSize().height/2
                - dialog.getHeight()/2
        );
        
        dialog.setVisible(true);
    }

    /**
    * stop the bundle
    */
    public void stop(BundleContext bundleContext) throws Exception
    {
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(BrowserLauncherService.class.getName());

            browserLauncherService = (BrowserLauncherService) bundleContext
                .getService(serviceReference);
        }

        return browserLauncherService;
    }
    
    /**
     * Check the first link as files on the web are sorted by date
     * @param currentVersionStr
     * @return
     */
    private boolean isNewestVersion(String currentVersionStr)
    {
        try
        {
            String osName = System.getProperty("os.name");
            String osDir = null;
            
            if (osName.startsWith("Mac"))
               osDir = "/macosx";
            else if (osName.startsWith("Linux"))
               osDir = "/linux";
            else if (osName.startsWith("Windows"))
                osDir = "/windows";
            
            URL url = new URL(Resources.getConfigString("destinationPath") + 
                osDir + "/versionupdate.properties");
    
            Properties props = new Properties();
            props.load(url.openStream());
            
            String lastVersion = props.getProperty("last_version");
            downloadLink = props.getProperty("download_link");
            
            return lastVersion.compareTo(currentVersionStr) <= 0;
        }
        catch (Exception e) 
        {
            logger.error("Cannot get and compare versions!", e);
        }
        
        return false;
    }
}