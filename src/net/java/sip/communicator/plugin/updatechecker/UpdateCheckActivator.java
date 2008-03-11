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
        contentMessage.setText(
            MessageFormat.format(
                Resources.getLangString("dialogMessage"),
                ver.getApplicationName()) + 
            "<a href=\"" + Resources.getConfigString("destinationPath") + "\">" + 
            Resources.getConfigString("destinationPath") + "</a> </html>");

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
        String pkgName = Resources.getConfigString("pkgName") + "-";
        
        EditorKit kit = new HTMLEditorKit();
        Document doc = kit.createDefaultDocument();

        // The Document class does not yet 
        // handle charset's properly.
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        try 
        {
            // as some of the distribution has own mechanisums of 
            // updating so just check from one of the platforms
            // which is the last version
            
            // Create a reader on the HTML content.
            Reader rd = getReader(Resources.getConfigString("destinationPath") + "/windows");

            // Parse the HTML.
            kit.read(rd, doc, 0);

            // Iterate through the elements 
            // of the HTML document.
            ElementIterator it = new ElementIterator(doc);
            Element elem;
            while ((elem = it.next()) != null) 
            {
                SimpleAttributeSet s = (SimpleAttributeSet)
                elem.getAttributes().getAttribute(HTML.Tag.A);
                if (s != null) 
                {
                    String link = s.getAttribute(HTML.Attribute.HREF).toString();
                    
                    if(!link.startsWith(pkgName))
                        continue;
                    
                    link = link.replaceAll(pkgName, "");
                    
                    link = link.substring(0, link.lastIndexOf('.'));
                    
                    return link.compareTo(currentVersionStr) <= 0;
                }
            }
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        
        return false;
    }

    // Returns a reader on the HTML data. If 'uri' begins
    // with "http:", it's treated as a URL; otherwise,
    // it's assumed to be a local filename.
    static Reader getReader(String uri) 
        throws IOException 
    {
        if (uri.startsWith("http:")) 
        {
            // Retrieve from Internet.
            URLConnection conn = new URL(uri).openConnection();
            return new InputStreamReader(conn.getInputStream());
        } 
        else 
        {
            return new FileReader(uri);
        }
    }
}