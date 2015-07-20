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

import java.util.*;

import javax.swing.*;

import org.osgi.framework.*;

/**
 * <tt>SkinSelector</tt> is extending a <tt>JComboBox</tt> and represents
 * a selector for selecting of SIP Communicator skins.
 *
 * @author Adam Netocny
 * @author Yana Stamcheva
 */
public class SkinSelector
        extends JComboBox
        implements BundleListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Text for the default skin menu item.
     */
    public static final String DEFAULT_TEXT
        = Resources.getString("plugin.skinmanager.DEFAULT_SKIN");

    /**
     * Text for the default skin menu item description section.
     */
    public static final String DEFAULT_DESCRIPTION_TEXT
        = Resources.getString("plugin.skinmanager.DEFAULT_SKIN_DESCRIPTION");

    /**
     * Text for the add skin menu item.
     */
    public static final String ADD_TEXT
        = Resources.getString("plugin.skinmanager.ADD_NEW_SKIN");

    /**
     * Action listener for skin selection.
     */
    private SkinSelectionListener listener;

    /**
     * Thread lock.
     */
    private final Object lock = new Object();

    /**
     * Casts the data model to <tt>DefaultComboBoxModel</tt>.
     */
    private final DefaultComboBoxModel dataModel
        = (DefaultComboBoxModel) super.dataModel;

    /**
     * Constructor.
     */
    public SkinSelector()
    {
        SkinManagerActivator.bundleContext.addBundleListener(this);

        listener = new SkinSelectionListener();

        addActionListener(listener);

        setRenderer(new SkinSelectorRenderer());

        init();
    }

    /**
     * Reloads the skin list.
     */
    private void init()
    {
        if(listener != null)
            listener.suppressAction(true);

        synchronized(lock)
        {
            removeAllItems();

            addItem(DEFAULT_TEXT);
            addSkinBundles();
            addItem(SkinSelectorRenderer.SEPARATOR);
            addItem(ADD_TEXT);
        }

        selectActiveSkin();

        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        if(listener != null)
                            listener.suppressAction(false);
                    }
                });
    }

    /**
     * Selects the active skin.
     */
    public void selectActiveSkin()
    {
        // If we're not in the event dispatch thread should first move to it.
        if(!SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    selectActiveSkin();
                }
            });

        if(listener != null)
            listener.suppressAction(true);

        synchronized(lock)
        {
            boolean found = false;
            for (int i = 0; i < dataModel.getSize(); i++)
            {
                Object o = dataModel.getElementAt(i);

                if (!(o instanceof Bundle))
                    continue;

                Bundle b = (Bundle) o;

                if(b.getState() == Bundle.ACTIVE
                        || b.getState() == Bundle.STARTING)
                {
                    setSelectedItem(b);
                    found = true;
                }
            }

            if(!found)
                setSelectedIndex(0);
        }

        SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    if(listener != null)
                        listener.suppressAction(false);
                }
            });
    }

    /**
     * Selects the default skin(no-skin).
     */
    public void selectNoSkin()
    {
        // If we're not in the event dispatch thread should first move to it.
        if(!SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    selectNoSkin();
                }
            });

        setSelectedIndex(0);

        synchronized(lock)
        {
            for (int i = 0; i < getModel().getSize(); i++)
            {
                Object o = getModel().getElementAt(i);

                if (!(o instanceof Bundle))
                    continue;

                Bundle b = (Bundle) o;

                try
                {
                    if (b.getState() == Bundle.ACTIVE)
                    b.stop();
                }
                catch (BundleException ex)
                {
                    //ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Adds all selectable skin bundles into the list.
     */
    private void addSkinBundles()
    {
        Bundle[] list = SkinManagerActivator.bundleContext.getBundles();

        Arrays.sort(list, new BundleComparator());

        if (list != null && list.length != 0)
        {
            for (Bundle b : list)
            {
                if(isSkin(b))
                    addItem(b);
            }
        }
    }

    /**
     * Bundle listener implementation. Reloads the list.
     *
     * @param be <tt>BundleEvent</tt>.
     */
    public void bundleChanged(final BundleEvent be)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                Bundle b = be.getBundle();
                int eventType = be.getType();

                if (eventType == BundleEvent.INSTALLED && isSkin(b))
                {
                    insertItemAt(b, dataModel.getSize() - 2);
                    return;
                }
                else if (eventType == BundleEvent.UNINSTALLED && isSkin(b))
                {
                    if (getSelectedItem().equals(b))
                    {
                        selectNoSkin();
                        removeItem(b);
                    }

                    return;
                }

                // We're only interested in bundle events related to our
                // skin list.
                if (dataModel.getIndexOf(b) < 0)
                    return;

                if ((eventType == BundleEvent.STOPPING
                    || eventType == BundleEvent.STARTING)
                    && getSelectedItem().equals(b))
                {
                    selectNoSkin();
                }
                else if (eventType == BundleEvent.STARTED
                        && !getSelectedItem().equals(b))
                {
                    selectActiveSkin();
                }
            }
        });
    }

    /**
     * Checks if the given <tt>bundle</tt> is a skin.
     *
     * @param bundle the <tt>Bundle</tt> to check
     * @return <tt>true</tt> if the given <tt>bundle</tt> is a skin,
     * <tt>false</tt> otherwise
     */
    private boolean isSkin(Bundle bundle)
    {
        Dictionary<?, ?> headers = bundle.getHeaders();
        if (headers.get(Constants.BUNDLE_ACTIVATOR) != null)
        {
            if (headers.get(Constants.BUNDLE_ACTIVATOR).toString()
                .equals("net.java.sip.communicator.plugin." +
                        "skinresourcepack.SkinResourcePack"))
            {
                return true;
            }
        }

        return false;
    }
}
