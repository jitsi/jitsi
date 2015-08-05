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

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.osgi.framework.*;

/**
 * Render class for the <tt>SkinSelector</tt> class.
 *
 * @author Adam Netocny
 * @author Yana Stamcheva
 */
public class SkinSelectorRenderer
        extends TransparentPanel
        implements ListCellRenderer
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Separator string. Will be replaced with a <tt>JSeparator</tt>.
     */
    public static final String SEPARATOR = "separator";

    /**
     * The name label.
     */
    private JLabel nameLabel = new JLabel();

    /**
     * The description label.
     */
    private JLabel descriptionLabel = new JLabel();

    /**
     * Separator
     */
    JSeparator separator = new JSeparator();

    /**
     * Constructor
     */
    public SkinSelectorRenderer()
    {
        super(new BorderLayout(5, 5));
    }

    /**
     * Return a component that has been configured to display the specified
     * value. That component's <code>paint</code> method is then called to
     * "render" the cell.  If it is necessary to compute the dimensions
     * of a list because the list cells do not have a fixed size, this method
     * is called to generate a component on which <code>getPreferredSize</code>
     * can be invoked.
     *
     * @param list The JList we're painting.
     * @param value The value returned by list.getModel().getElementAt(index).
     * @param index The cells index.
     * @param isSelected True if the specified cell was selected.
     * @param cellHasFocus True if the specified cell has the focus.
     * @return A component whose paint() method will render the specified value.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        this.removeAll();

        if(value.equals(SEPARATOR))//Separator
        {
            add(separator, BorderLayout.CENTER);
            this.setBackground(list.getBackground());
            this.setForeground(list.getForeground());

            return this;
        }
        else if(value.equals(SkinSelector.ADD_TEXT))
        {
            add(new JLabel(SkinSelector.ADD_TEXT));
        }
        else if (value.equals(SkinSelector.DEFAULT_TEXT))
        {
            initBundleView();

            nameLabel.setText(SkinSelector.DEFAULT_TEXT);
            descriptionLabel.setText(SkinSelector.DEFAULT_DESCRIPTION_TEXT);
        }
        else if(value instanceof Bundle)
        {
            initBundleView();

            Bundle bundle = (Bundle) value;

            URL res;
            try
            {
                res = bundle.getResource("info.properties");
            }
            catch(Throwable ex)
            {
                res = null;
            }

            String bundleName = "unknown";
            String bundleDescription = "";

            if (res != null)
            {
                Properties props = new Properties();
                try
                {
                    props.load(res.openStream());
                    String disp = props.getProperty("display_name");
                    if (disp != null)
                    {
                        bundleName = disp;
                    }

                    disp = props.getProperty("version");
                    if (disp != null)
                    {
                        bundleName += " " + disp;
                    }

                    disp = props.getProperty("author");
                    String desc = props.getProperty("description");
                    String bundString = "";
                    if (disp != null)
                    {
                        bundString = disp;
                    }
                    if (desc != null)
                    {
                        if (disp != null)
                        {
                            bundString += " - ";
                        }
                        bundString += desc;
                    }

                    if(!bundString.equals(""))
                    {
                        bundleDescription = bundString;
                    }
                } catch (IOException ex) {
                }
            }

            this.nameLabel.setText(bundleName.toString());
            this.descriptionLabel.setText(bundleDescription);
        }

        if (isSelected)
        {
            this.setBackground(list.getSelectionBackground());
            this.setForeground(list.getSelectionForeground());
        }
        else
        {
            this.setBackground(list.getBackground());
            this.setForeground(list.getForeground());
        }

        return this;
    }

    /**
     * Adds necessary components to render bundle informations.
     */
    private void initBundleView()
    {
        JPanel mainPanel = new JPanel(new BorderLayout());

        this.setBackground(Color.WHITE);

        this.setOpaque(true);

        mainPanel.setOpaque(false);

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.nameLabel.setIconTextGap(2);

        this.nameLabel.setFont(this.getFont().deriveFont(Font.BOLD));

        mainPanel.add(nameLabel, BorderLayout.NORTH);
        mainPanel.add(descriptionLabel, BorderLayout.SOUTH);

        this.add(mainPanel, BorderLayout.NORTH);
    }
}
