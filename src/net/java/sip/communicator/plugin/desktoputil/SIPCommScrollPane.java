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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.image.*;
import java.beans.*;
import java.lang.reflect.*;

import javax.swing.*;

import net.java.sip.communicator.util.skin.*;

import org.jitsi.util.*;

/**
 * The SCScrollPane is a JScrollPane with a custom viewport that allows to
 * set an image as a background. Depending on the
 * "impl.gui.IS_CONTACT_LIST_TEXTURE_BG_ENABLED" property we'll be setting a
 * single image or a texture of images.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommScrollPane
    extends JScrollPane
    implements Skinnable
{
    private static final long serialVersionUID = 0L;

    /**
     * Creates an <tt>SCSCrollPane</tt>.
     */
    public SIPCommScrollPane()
    {
        this.setBorder(BorderFactory.createMatteBorder(
            1, 0, 1, 0, Color.GRAY));

        this.setViewport(new SCViewport());

        this.getVerticalScrollBar().setUnitIncrement(100);
    }

    /**
     * Sets the view of this JViewport.
     *
     * @param view the view to set.
     */
    @Override
    public void setViewportView(Component view)
    {
        if (view instanceof JComponent)
        {
            JComponent viewAsJComponent = (JComponent) view;

            viewAsJComponent.setBorder(
                    BorderFactory.createEmptyBorder(3, 3, 3, 3));
            viewAsJComponent.setOpaque(false);
        }

        super.setViewportView(view);
    }

    /**
     * Reloads skin information in viewport.
     */
    public void loadSkin()
    {
        ((SCViewport) getViewport()).loadSkin();
    }

    /**
     * The <tt>SCViewport</tt> used as viewport in this scrollpane.
     */
    private static class SCViewport
        extends JViewport
        implements Skinnable
    {
        private static final long serialVersionUID = 1L;

        private BufferedImage bgImage;

        private Color color;

        private TexturePaint texture;

        /**
         * Creates the <tt>SCViewport</tt>.
         */
        public SCViewport()
        {
            this.setBackground(Color.WHITE);

            loadSkin();
        }

        /**
         * Returns the boolean value of the property given by <tt>key</tt>.
         * @param key the key of the property we look for
         * @return the boolean value of the searched property
         */
        private boolean getSettingsBoolean(String key)
        {
            return
                Boolean.parseBoolean(
                        DesktopUtilActivator.getResources().getSettingsString(key));
        }

        /**
         * Paints this viewport.
         * @param g the <tt>Graphics</tt> object used for painting
         */
        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();
            try
            {
                AntialiasingManager.activateAntialiasing(g);

                Graphics2D g2 = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();

                // paint the image
                if (bgImage != null)
                {
                    if (texture != null)
                    {
                        g2.setPaint(texture);

                        g2.fillRect(0, 0, width, height);
                    }
                    else
                    {
                        g.setColor(color);

                        // paint the background with the chosen color
                        g.fillRect(0, 0, width, height);

                        g2.drawImage(bgImage, width - bgImage.getWidth(),
                            height - bgImage.getHeight(), this);
                    }
                }
            }
            finally
            {
                g.dispose();
            }
        }

        /**
         * Reloads background.
         */
        public void loadSkin()
        {
            if(getSettingsBoolean("impl.gui.IS_CONTACT_LIST_IMG_BG_ENABLED"))
            {
                bgImage =
                    DesktopUtilActivator.getImage("service.gui.MAIN_WINDOW_BACKGROUND");

                if (getSettingsBoolean(
                    "impl.gui.IS_CONTACT_LIST_TEXTURE_BG_ENABLED")
                    && (bgImage != null))
                {
                    texture =
                        new TexturePaint(bgImage, new Rectangle(0, 0, bgImage
                            .getWidth(null), bgImage.getHeight(null)));

                    color = null;
                }
                else
                {
                    texture = null;
                    color =
                        new Color(DesktopUtilActivator.getResources().getColor(
                            "service.gui.CONTACT_LIST_BACKGROUND"));
                }
            }
            else
            {
                bgImage = null;
                texture = null;
                color = null;
            }
        }
    }

    /**
     * Releases the resources allocated by this instance throughout its lifetime
     * and prepares it for garbage collection.
     */
    public void dispose()
    {
        if(OSUtils.IS_MAC)
        {
            // Apple introduced a memory leak in JViewport class -
            // they add a PropertyChangeListeners to the CToolkit
            try
            {
                Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                PropertyChangeListener[] pcl
                    = defaultToolkit.getPropertyChangeListeners(
                            "apple.awt.contentScaleFactor");

                for(PropertyChangeListener pc : pcl)
                {
                    // find the reference to the object created the listener
                    Field f = pc.getClass().getDeclaredField("this$0");

                    f.setAccessible(true);
                    // If we are the parent, clean up.
                    if(f.get(pc).equals(this.getViewport()))
                    {
                        defaultToolkit.removePropertyChangeListener(
                                "apple.awt.contentScaleFactor",
                                pc);
                        break;
                    }
                }
            }
            catch(Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
            }
        }
    }
}
