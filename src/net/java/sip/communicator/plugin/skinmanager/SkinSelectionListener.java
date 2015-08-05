/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.skinmanager;

import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.zip.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.osgi.framework.*;

/**
 * Selection listener for the <tt>SkinSelector</tt>.
 * @author Adam Netocny
 */
public class SkinSelectionListener implements ActionListener
{
    /**
     * The object used for logging.
     */
    private Logger logger = Logger.getLogger(SkinSelectionListener.class);

    /**
     * Currently selected item.
     */
    private Object current = null;

    /**
     * Suppress value for events. If true the event will be ignored.
     */
    private boolean suppressed = false;

    /**
     * Invoked when an action occurs.
     * @param e <tt>ActionEvent</tt>
     */
    public void actionPerformed(ActionEvent e)
    {
        SkinSelector selector = (SkinSelector) e.getSource();

        if (current != null && current.equals(selector.getSelectedItem()))
            return;

        if(suppressed)
        {
            current = selector.getSelectedItem();
            return;
        }

        if(selector.getSelectedItem() instanceof String)
        {
            String selectedItem = (String) selector.getSelectedItem();
            if(selectedItem.equals(SkinSelector.ADD_TEXT))
            {
                selector.hidePopup();

                SipCommFileChooser chooser = createFileChooser();

                File newBundleFile = chooser.getFileFromDialog();
                if(newBundleFile != null)
                {
                    try
                    {
                        File jar = null;
                        try
                        {
                            jar = Resources.getResources()
                                .prepareSkinBundleFromZip(newBundleFile);
                        }
                        catch (Exception ex)
                        {
                            logger.info("Failed to load skin from zip.", ex);

                            SkinManagerActivator.getUIService().getPopupDialog()
                                .showMessagePopupDialog(ex.getClass() + ": "
                                    + ex.getMessage(), "Error",
                                    PopupDialog.ERROR_MESSAGE);
                        }

                        if (jar != null)
                        {
                            try
                            {
                                Bundle newBundle = SkinManagerActivator
                                    .bundleContext.installBundle(
                                        jar.toURI().toURL().toString());

                                selector.selectNoSkin();
                                newBundle.start();
                            }
                            catch (MalformedURLException ex)
                            {
                                logger.info("Failed to load skin from zip.", ex);
                            }
                        }
                    }
                    catch (BundleException ex)
                    {
                        logger.info("Failed to install bundle.", ex);
                        SkinManagerActivator.getUIService().getPopupDialog()
                            .showMessagePopupDialog(ex.getMessage(), "Error",
                                PopupDialog.ERROR_MESSAGE);
                    }
                    catch (Throwable ex)
                    {
                        logger.info("Failed to install bundle.", ex);
                    }
                }
                else
                {
                    if(current != null)
                    {
                        selector.setSelectedItem(current);
                    }
                    else
                    {
                        selector.setSelectedIndex(0);
                    }
                }
            }
            else if(selectedItem.equals(SkinSelector.DEFAULT_TEXT))
            {
                selector.selectNoSkin();
            }
            else if(selectedItem.equals(SkinSelectorRenderer.SEPARATOR))
            {
                if(current != null)
                {
                    selector.setSelectedItem(current);
                }
                else
                {
                    selector.setSelectedIndex(0);
                }
            }
        }
        else if(selector.getSelectedItem() instanceof Bundle)
        {
            Bundle select = (Bundle)selector.getSelectedItem();

            selector.selectNoSkin();

            try
            {
                select.start();
            }
            catch (BundleException ex)
            {
               // ex.printStackTrace();
            }
        }

        current = selector.getSelectedItem();
    }

    /**
     * Sets if the catched event should be ignored or not.
     * @param supp If true the event will be ignored.
     */
    public void suppressAction(boolean supp)
    {
        suppressed = supp;
    }

    /**
     * Creates the file chooser used to install a new skin.
     *
     * @return the created file chooser
     */
    private SipCommFileChooser createFileChooser()
    {
        SipCommFileChooser chooser = GenericFileDialog.create(
            null, "New bundle...",
            SipCommFileChooser.LOAD_FILE_OPERATION);

        chooser.addFilter(new SipCommFileFilter()
        {
            @Override
            public boolean accept(File f)
            {
                if (f.isDirectory())
                    return true;

                boolean good = true;
                try
                {
                    new ZipFile(f);
                }
                catch (IOException ex)
                {
                    good = false;
                }

                if (!f.getName().toLowerCase().endsWith(".zip"))
                {
                    good = false;
                }
                return good;
            }

            @Override
            public String getDescription()
            {
                return "Zip files (*.zip)";
            }
        });

        return chooser;
    }
}
