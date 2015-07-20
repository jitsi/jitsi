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

/*
 * The following code borrowed from David Bismut, davidou@mageos.com Intern,
 * SETLabs, Infosys Technologies Ltd. May 2004 - Jul 2004 Ecole des Mines de
 * Nantes, France
 */
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.util.skin.*;

/**
 * A JTabbedPane with some added UI functionalities. A close and max/detach
 * icons are added to every tab, typically to let the user close or detach the
 * tab by clicking on these icons.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommTabbedPane
    extends JTabbedPane
    implements  ChangeListener,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private int overTabIndex = -1;

    private int lastSelectedIndex;

    public SIPCommTabbedPane()
    {
        this(false, false);
    }

    /**
     * Creates the <code>CloseAndMaxTabbedPane</code> with an enhanced UI if
     * <code>enhancedUI</code> parameter is set to <code>true</code>.
     *
     * @param closingTabs support for closable tabs
     * @param maximizingTabs support for maximisable tabs
     */
    public SIPCommTabbedPane(boolean closingTabs, boolean maximizingTabs)
    {
        super.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        UIManager.getDefaults()
            .put("TabbedPane.tabAreaInsets", new Insets(0, 5, 0, 0));

        UIManager.getDefaults()
            .put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));

        this.setForeground(
            new Color(DesktopUtilActivator.getResources()
                .getColor("service.gui.TAB_TITLE")));

        this.setUI(new SIPCommTabbedPaneEnhancedUI());

        if(closingTabs)
            this.setCloseIcon(true);

        if(maximizingTabs)
            this.setMaxIcon(true);

        this.addChangeListener(this);
    }

    /**
     * Returns the index of the last tab on which the mouse did an action.
     *
     * @return
     */
    public int getOverTabIndex()
    {
        return overTabIndex;
    }

    /**
     * Returns <code>true</code> if the close icon is enabled.
     *
     * @return
     */
    public boolean isCloseEnabled()
    {
        SIPCommTabbedPaneUI ui = (SIPCommTabbedPaneUI) this.getUI();
        return ui.isCloseEnabled();
    }

    /**
     * Returns <code>true</code> if the max/detach icon is enabled.
     *
     * @return
     */
    public boolean isMaxEnabled()
    {
        return ((SIPCommTabbedPaneUI) getUI()).isMaxEnabled();
    }

    /**
     * Override JTabbedPane method. Does nothing.
     * @param tabLayoutPolicy The tab layout policy.
     */
    @Override
    public void setTabLayoutPolicy(int tabLayoutPolicy)
    {
    }

    /**
     * Override JTabbedPane method. Does nothing.
     * @param tabPlacement The tab placement.
     */
    @Override
    public void setTabPlacement(int tabPlacement)
    {
    }

    /**
     * Sets whether the tabbedPane should have a close icon or not.
     *
     * @param b whether the tabbedPane should have a close icon or not
     */
    public void setCloseIcon(boolean b)
    {
        ((SIPCommTabbedPaneUI) getUI()).setCloseIcon(b);
    }

    /**
     * Sets whether the tabbedPane should have a max/detach icon or not.
     *
     * @param b whether the tabbedPane should have a max/detach icon or not
     */
    public void setMaxIcon(boolean b)
    {
        ((SIPCommTabbedPaneUI) getUI()).setMaxIcon(b);
    }

    /**
     * Detaches the <code>index</code> tab in a separate frame. When the frame
     * is closed, the tab is automatically reinserted into the tabbedPane.
     *
     * @param index index of the tabbedPane to be detached
     */
    public void detachTab(int index)
    {
        if (index < 0 || index >= getTabCount())
            return;

        final int tabIndex = index;
        final JComponent c = (JComponent) getComponentAt(tabIndex);

        final Icon icon = getIconAt(tabIndex);
        final String title = getTitleAt(tabIndex);
        final String toolTip = getToolTipTextAt(tabIndex);
        final Border border = c.getBorder();

        final JFrame frame = new SIPCommFrame()
        {
            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 0L;

            @Override
            protected void close(boolean isEscaped)
            {
                if (isEscaped)
                    return;

                dispose();

                insertTab(title, icon, c, toolTip, Math.min(tabIndex,
                    getTabCount()));

                c.setBorder(border);
                setSelectedComponent(c);
            }
        };

        Window parentWindow = SwingUtilities.windowForComponent(this);

        removeTabAt(index);

        c.setPreferredSize(c.getSize());

        frame.setTitle(title);
        frame.getContentPane().add(c);
        frame.setLocation(parentWindow.getLocation());
        frame.pack();

        WindowFocusListener windowFocusListener = new WindowFocusListener() {
            long start;

            long end;

            public void windowGainedFocus(WindowEvent e) {
                start = System.currentTimeMillis();
            }

            public void windowLostFocus(WindowEvent e) {
                end = System.currentTimeMillis();
                long elapsed = end - start;

                if (elapsed < 100)
                    frame.toFront();

                frame.removeWindowFocusListener(this);
            }
        };

        /*
         * This is a small hack to avoid Windows GUI bug, that prevent a new
         * window from stealing focus (without this windowFocusListener, most of
         * the time the new frame would just blink from foreground to
         * background). A windowFocusListener is added to the frame, and if the
         * time between the frame beeing in foreground and the frame beeing in
         * background is less that 100ms, it just brings the windows to the
         * front once again. Then it removes the windowFocusListener. Note that
         * this hack would not be required on Linux or UNIX based systems.
         */

        frame.addWindowFocusListener(windowFocusListener);

        // frame.show();
        frame.setVisible(true);
        frame.toFront();

    }

    /**
     * Adds a <code>CloseListener</code> to the tabbedPane.
     *
     * @param l the <code>CloseListener</code> to add
     * @see #fireCloseTabEvent
     * @see #removeCloseListener
     */
    public synchronized void addCloseListener(CloseListener l)
    {
        listenerList.add(CloseListener.class, l);
    }

    /**
     * Adds a <code>MaxListener</code> to the tabbedPane.
     *
     * @param l the <code>MaxListener</code> to add
     * @see #fireMaxTabEvent
     * @see #removeMaxListener
     */
    public synchronized void addMaxListener(MaxListener l)
    {
        listenerList.add(MaxListener.class, l);
    }

    /**
     * Adds a <code>DoubleClickListener</code> to the tabbedPane.
     *
     * @param l the <code>DoubleClickListener</code> to add
     * @see #fireDoubleClickTabEvent
     * @see #removeDoubleClickListener
     */
    public synchronized void addDoubleClickListener(DoubleClickListener l)
    {
        listenerList.add(DoubleClickListener.class, l);
    }

    /**
     * Adds a <code>PopupOutsideListener</code> to the tabbedPane.
     *
     * @param l the <code>PopupOutsideListener</code> to add
     * @see #firePopupOutsideTabEvent
     * @see #removePopupOutsideListener
     */
    public synchronized void addPopupOutsideListener(PopupOutsideListener l)
    {
        listenerList.add(PopupOutsideListener.class, l);
    }

    /**
     * Removes a <code>CloseListener</code> from this tabbedPane.
     *
     * @param l the <code>CloseListener</code> to remove
     * @see #fireCloseTabEvent
     * @see #addCloseListener
     */
    public synchronized void removeCloseListener(CloseListener l)
    {
        listenerList.remove(CloseListener.class, l);
    }

    /**
     * Removes a <code>MaxListener</code> from this tabbedPane.
     *
     * @param l the <code>MaxListener</code> to remove
     * @see #fireMaxTabEvent
     * @see #addMaxListener
     */
    public synchronized void removeMaxListener(MaxListener l)
    {
        listenerList.remove(MaxListener.class, l);
    }

    /**
     * Removes a <code>DoubleClickListener</code> from this tabbedPane.
     *
     * @param l
     *            the <code>DoubleClickListener</code> to remove
     * @see #fireDoubleClickTabEvent
     * @see #addDoubleClickListener
     */
    public synchronized void removeDoubleClickListener(DoubleClickListener l)
    {
        listenerList.remove(DoubleClickListener.class, l);
    }

    /**
     * Removes a <code>PopupOutsideListener</code> from this tabbedPane.
     *
     * @param l
     *            the <code>PopupOutsideListener</code> to remove
     * @see #firePopupOutsideTabEvent
     * @see #addPopupOutsideListener
     */
    public synchronized void removePopupOutsideListener(
                                        PopupOutsideListener l)
    {
        listenerList.remove(PopupOutsideListener.class, l);
    }

    /**
     * Sends a <code>MouseEvent</code>, whose source is this tabbedpane, to
     * every <code>CloseListener</code>. The method also updates the
     * <code>overTabIndex</code> of the tabbedPane with a value coming from
     * the UI. This method method is called each time a <code>MouseEvent</code>
     * is received from the UI when the user clicks on the close icon of the tab
     * which index is <code>overTabIndex</code>.
     *
     * @param e
     *            the <code>MouseEvent</code> to be sent
     * @param overTabIndex
     *            the index of a tab, usually the tab over which the mouse is
     *
     * @see #addCloseListener
     */
    public void fireCloseTabEvent(MouseEvent e, int overTabIndex)
    {
        this.overTabIndex = overTabIndex;

        EventListener[] closeListeners = getListeners(CloseListener.class);
        for (int i = 0; i < closeListeners.length; i++)
        {
            ((CloseListener) closeListeners[i]).closeOperation(e);
        }
    }

    /**
     * Sends a <code>MouseEvent</code>, whose source is this tabbedpane, to
     * every <code>MaxListener</code>. The method also updates the
     * <code>overTabIndex</code> of the tabbedPane with a value coming from
     * the UI. This method method is called each time a <code>MouseEvent</code>
     * is received from the UI when the user clicks on the max icon of the tab
     * which index is <code>overTabIndex</code>.
     *
     * @param e
     *            the <code>MouseEvent</code> to be sent
     * @param overTabIndex
     *            the index of a tab, usually the tab over which the mouse is
     *
     * @see #addMaxListener
     */
    public void fireMaxTabEvent(MouseEvent e, int overTabIndex)
    {
        this.overTabIndex = overTabIndex;

        EventListener[] maxListeners = getListeners(MaxListener.class);
        for (int i = 0; i < maxListeners.length; i++)
        {
            ((MaxListener) maxListeners[i]).maxOperation(e);
        }
    }

    /**
     * Sends a <code>MouseEvent</code>, whose source is this tabbedpane, to
     * every <code>DoubleClickListener</code>. The method also updates the
     * <code>overTabIndex</code> of the tabbedPane with a value coming from
     * the UI. This method method is called each time a <code>MouseEvent</code>
     * is received from the UI when the user double-clicks on the tab which
     * index is <code>overTabIndex</code>.
     *
     * @param e
     *            the <code>MouseEvent</code> to be sent
     * @param overTabIndex
     *            the index of a tab, usually the tab over which the mouse is
     *
     * @see #addDoubleClickListener
     */
    public void fireDoubleClickTabEvent(MouseEvent e, int overTabIndex)
    {
        this.overTabIndex = overTabIndex;

        EventListener[] dClickListeners
            = getListeners(DoubleClickListener.class);
        for (int i = 0; i < dClickListeners.length; i++)
        {
            ((DoubleClickListener) dClickListeners[i]).doubleClickOperation(e);
        }
    }

    /**
     * Sends a <code>MouseEvent</code>, whose source is this tabbedpane, to
     * every <code>PopupOutsideListener</code>. The method also sets the
     * <code>overTabIndex</code> to -1. This method method is called each time
     * a <code>MouseEvent</code> is received from the UI when the user
     * right-clicks on the inactive part of a tabbedPane.
     *
     * @param e
     *            the <code>MouseEvent</code> to be sent
     *
     * @see #addPopupOutsideListener
     */
    public void firePopupOutsideTabEvent(MouseEvent e)
    {
        this.overTabIndex = -1;

        EventListener[] popupListeners
            = getListeners(PopupOutsideListener.class);
        for (int i = 0; i < popupListeners.length; i++)
        {
            ((PopupOutsideListener) popupListeners[i]).popupOutsideOperation(e);
        }
    }

    /**
     * Overrides setSelectedIndex in JTabbedPane in order to remove the
     * highlight if the tab which is selected.
     * @param tabIndex The index of the tab to be selected.
     */
    @Override
    public void setSelectedIndex(int tabIndex)
    {
        SIPCommTabbedPaneEnhancedUI ui
            = (SIPCommTabbedPaneEnhancedUI) this.getUI();
        if (ui.isTabHighlighted(tabIndex))
        {
            ui.tabRemoveHighlight(tabIndex);
        }
        super.setSelectedIndex(tabIndex);
    }

    /**
     * Highlights the tab with the given index.
     *
     * @param tabIndex The tab index.
     */
    public void highlightTab(int tabIndex)
    {
        SIPCommTabbedPaneEnhancedUI ui
            = (SIPCommTabbedPaneEnhancedUI) this.getUI();

        if (!ui.isTabHighlighted(tabIndex)
                && this.getSelectedIndex() != tabIndex)
            ui.tabAddHightlight(tabIndex);

        this.repaint();
    }

    @Override
    public void removeTabAt(int index)
    {
        if (index < lastSelectedIndex)
        {
            this.setSelectedIndex(lastSelectedIndex - 1);
        }
        else if (index > lastSelectedIndex)
        {
            this.setSelectedIndex(lastSelectedIndex);
        }

        super.removeTabAt(index);
    }

    public void stateChanged(ChangeEvent e)
    {
        lastSelectedIndex = this.getSelectedIndex();
    }

    /**
     * Reloads skin information.
     */
    public void loadSkin()
    {
        this.setForeground(
            new Color(DesktopUtilActivator.getResources()
                .getColor("service.gui.TAB_TITLE")));

        ((SIPCommTabbedPaneEnhancedUI)this.getUI()).loadSkin();
    }
}
