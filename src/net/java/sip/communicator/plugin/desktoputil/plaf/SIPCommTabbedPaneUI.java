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
package net.java.sip.communicator.plugin.desktoputil.plaf;

/*
 * The content of this file was based on code borrowed from David Bismut,
 * davidou@mageos.com Intern, SETLabs, Infosys Technologies Ltd. May 2004 - Jul
 * 2004 Ecole des Mines de Nantes, France
 */
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

/**
 * SIPCommTabbedPaneUI implementation.
 */
public class SIPCommTabbedPaneUI
    extends BasicTabbedPaneUI
    implements Skinnable
{
    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint a close
     * button on a tab.
     */
    private static final String CLOSE_TAB_ICON =
        "service.gui.lookandfeel.CLOSE_TAB_ICON";

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint a rollover
     * close button on a tab.
     */
    //private static final String CLOSE_TAB_SELECTED_ICON =
    //    "service.gui.lookandfeel.CLOSE_TAB_SELECTED_ICON";

    // Instance variables initialized at installation
    private ContainerListener containerListener;

    private Vector<View> htmlViews;

    private Map<Integer, Integer> mnemonicToIndexMap;

    /**
     * InputMap used for mnemonics. Only non-null if the JTabbedPane has
     * mnemonics associated with it. Lazily created in initMnemonics.
     */
    private InputMap mnemonicInputMap;

    // For use when tabLayoutPolicy = SCROLL_TAB_LAYOUT
    protected ScrollableTabSupport tabScroller;

    private int tabCount;

    protected MyMouseMotionListener motionListener;

    // UI creation

    private static final int INACTIVE = 0;

    private static final int OVER = 1;

    private static final int PRESSED = 2;

    public static final int BUTTONSIZE = 15;

    public static final int WIDTHDELTA = 1;

    private static final Border PRESSEDBORDER = new SoftBevelBorder(
            SoftBevelBorder.LOWERED);

    private static final Border OVERBORDER = new SoftBevelBorder(
            SoftBevelBorder.RAISED);

    //private Image closeImgB;

    //private BufferedImage maxImgB;

    private Image closeImgI;

    private BufferedImage maxImgI;

    private int overTabIndex = -1;

    private int closeIndexStatus = INACTIVE;

    private int maxIndexStatus = INACTIVE;

    private boolean mousePressed = false;

    private boolean isCloseButtonEnabled = false;

    private boolean isMaxButtonEnabled = false;

    protected JPopupMenu actionPopupMenu;

    protected JMenuItem maxItem;

    protected JMenuItem closeItem;

    public SIPCommTabbedPaneUI()
    {
        //closeImgB = SwingSwingUtilActivator.getImage(CLOSE_TAB_SELECTED_ICON);

        //maxImgB = new BufferedImage(BUTTONSIZE, BUTTONSIZE,
        //        BufferedImage.TYPE_4BYTE_ABGR);

        loadSkin();

        actionPopupMenu = new JPopupMenu();

        maxItem = new JMenuItem("Detach");
        closeItem = new JMenuItem("Close");

        maxItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ((SIPCommTabbedPane) tabPane).fireMaxTabEvent(null, tabPane
                        .getSelectedIndex());
            }
        });

        closeItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ((SIPCommTabbedPane) tabPane).fireCloseTabEvent(null, tabPane
                        .getSelectedIndex());
            }
        });

        setPopupMenu();
    }

    protected boolean isOneActionButtonEnabled()
    {
        return isCloseButtonEnabled || isMaxButtonEnabled;
    }

    public boolean isCloseEnabled()
    {
        return isCloseButtonEnabled;
    }

    public boolean isMaxEnabled()
    {
        return isMaxButtonEnabled;
    }

    public void setCloseIcon(boolean b)
    {
        isCloseButtonEnabled = b;
        setPopupMenu();
    }

    public void setMaxIcon(boolean b)
    {
        isMaxButtonEnabled = b;
        setPopupMenu();
    }

    private void setPopupMenu()
    {
        actionPopupMenu.removeAll();
        if (isMaxButtonEnabled)
            actionPopupMenu.add(maxItem);
        if (isMaxButtonEnabled && isCloseButtonEnabled)
            actionPopupMenu.addSeparator();
        if (isCloseButtonEnabled)
            actionPopupMenu.add(closeItem);
    }

    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex,
            FontMetrics metrics)
    {
        int delta = 0;

        Insets tabInsets = getTabInsets(tabPlacement, tabIndex);

        if (isOneActionButtonEnabled())
        {
            tabInsets.right = 0;

            if (isCloseButtonEnabled)
                delta += BUTTONSIZE;
            if (isMaxButtonEnabled)
                delta += BUTTONSIZE;
        }

        return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + delta;
    }

    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex,
            int fontHeight)
    {
        return super.calculateTabHeight(tabPlacement, tabIndex, fontHeight + 4);
    }

    @Override
    protected void layoutLabel(int tabPlacement, FontMetrics metrics,
            int tabIndex, String title, Icon icon, Rectangle tabRect,
            Rectangle iconRect, Rectangle textRect, boolean isSelected)
    {
        textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            tabPane.putClientProperty("html", v);
        }

        SwingUtilities.layoutCompoundLabel(tabPane,
                                            metrics,
                                            title,
                                            icon,
                                            SwingUtilities.CENTER,
                                            SwingUtilities.LEFT,
                                            SwingUtilities.CENTER,
                                            SwingUtilities.CENTER,
                                            tabRect,
                                            iconRect,
                                            textRect,
                                            0);

        tabPane.putClientProperty("html", null);

        if (icon != null)
        {
            iconRect.y = iconRect.y + 2;
            iconRect.x = tabRect.x + 7;
        }

        textRect.y = textRect.y + 2;

        if (icon != null)
            textRect.x = iconRect.x + iconRect.width + 5;
        else
            textRect.x = textRect.x + 8;
    }

    @Override
    protected MouseListener createMouseListener()
    {
        return new MyMouseHandler();
    }

    protected ScrollableTabButton createScrollableTabButton(int direction)
    {
        return new ScrollableTabButton(direction);
    }

    protected Rectangle newCloseRect(Rectangle rect)
    {
        int dx = rect.x + rect.width - BUTTONSIZE - WIDTHDELTA;
        int dy = rect.y + (rect.height - BUTTONSIZE) / 2 + 2;

        return new Rectangle(dx, dy, BUTTONSIZE, BUTTONSIZE);
    }

    protected Rectangle newMaxRect(Rectangle rect)
    {
        int dx = rect.x + rect.width - BUTTONSIZE - WIDTHDELTA;
        int dy = rect.y + (rect.height - BUTTONSIZE) / 2 + 2;

        if (isCloseButtonEnabled)
            dx -= BUTTONSIZE;

        return new Rectangle(dx, dy, BUTTONSIZE, BUTTONSIZE);
    }

    protected void updateOverTab(int x, int y)
    {
        int overTabIndex = getTabAtLocation(x, y);
        if (this.overTabIndex != overTabIndex)
        {
            this.overTabIndex = overTabIndex;
            tabScroller.tabPanel.repaint();
        }
    }

    protected void updateCloseIcon(int x, int y)
    {
        if (overTabIndex != -1) {
            int newCloseIndexStatus = INACTIVE;

            Rectangle closeRect = newCloseRect(rects[overTabIndex]);
            if (closeRect.contains(x, y))
                newCloseIndexStatus = mousePressed ? PRESSED : OVER;

            if (closeIndexStatus != newCloseIndexStatus)
            {
                closeIndexStatus = newCloseIndexStatus;
                tabScroller.tabPanel.repaint();
            }
        }
    }

    protected void updateMaxIcon(int x, int y)
    {
        if (overTabIndex != -1)
        {
            int newMaxIndexStatus = INACTIVE;

            Rectangle maxRect = newMaxRect(rects[overTabIndex]);

            if (maxRect.contains(x, y))
                newMaxIndexStatus = mousePressed ? PRESSED : OVER;

            if (maxIndexStatus != newMaxIndexStatus)
            {
                maxIndexStatus = newMaxIndexStatus;
                tabScroller.tabPanel.repaint();
            }
        }
    }

    private void setTabIcons(int x, int y)
    {
        // if the mouse isPressed
        if (!mousePressed)
            updateOverTab(x, y);

        if (isCloseButtonEnabled)
            updateCloseIcon(x, y);
        if (isMaxButtonEnabled)
            updateMaxIcon(x, y);
    }

    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommTabbedPaneUI();
    }

    /**
     * Invoked by <code>installUI</code> to create a layout manager object to
     * manage the <code>JTabbedPane</code>.
     *
     * @return a layout manager object
     *
     * @see javax.swing.JTabbedPane#getTabLayoutPolicy
     */
    @Override
    protected LayoutManager createLayoutManager()
    {
        return new TabbedPaneScrollLayout();
    }

    /*
     * In an attempt to preserve backward compatibility for programs which have
     * extended BasicTabbedPaneUI to do their own layout, the UI uses the
     * installed layoutManager (and not tabLayoutPolicy) to determine if
     * scrollTabLayout is enabled.
     */

    /**
     * Creates and installs any required subcomponents for the JTabbedPane.
     * Invoked by installUI.
     *
     * @since 1.4
     */
    @Override
    protected void installComponents()
    {
        if (tabScroller == null)
        {
            tabScroller = new ScrollableTabSupport(tabPane.getTabPlacement());
            tabPane.add(tabScroller.viewport);
            tabPane.add(tabScroller.scrollForwardButton);
            tabPane.add(tabScroller.scrollBackwardButton);
        }
    }

    /**
     * Removes any installed subcomponents from the JTabbedPane. Invoked by
     * uninstallUI.
     *
     * @since 1.4
     */
    @Override
    protected void uninstallComponents()
    {
        tabPane.remove(tabScroller.viewport);
        tabPane.remove(tabScroller.scrollForwardButton);
        tabPane.remove(tabScroller.scrollBackwardButton);
        tabScroller = null;
    }

    @Override
    protected void installListeners()
    {
        if ((propertyChangeListener = createPropertyChangeListener()) != null)
        {
            tabPane.addPropertyChangeListener(propertyChangeListener);
        }
        if ((tabChangeListener = createChangeListener()) != null)
        {
            tabPane.addChangeListener(tabChangeListener);
        }
        if ((mouseListener = createMouseListener()) != null)
        {
            tabScroller.tabPanel.addMouseListener(mouseListener);
        }

        if ((focusListener = createFocusListener()) != null)
        {
            tabPane.addFocusListener(focusListener);
        }

        // PENDING(api) : See comment for ContainerHandler
        if ((containerListener = new ContainerHandler()) != null)
        {
            tabPane.addContainerListener(containerListener);
            if (tabPane.getTabCount() > 0)
            {
                htmlViews = createHTMLVector();
            }
        }

        if ((motionListener = new MyMouseMotionListener()) != null)
        {
            tabScroller.tabPanel.addMouseMotionListener(motionListener);
        }

    }

    @Override
    protected void uninstallListeners()
    {
        if (mouseListener != null)
        {
            tabScroller.tabPanel.removeMouseListener(mouseListener);
            mouseListener = null;
        }

        if (motionListener != null)
        {
            tabScroller.tabPanel.removeMouseMotionListener(motionListener);
            motionListener = null;
        }

        if (focusListener != null)
        {
            tabPane.removeFocusListener(focusListener);
            focusListener = null;
        }

        // PENDING(api): See comment for ContainerHandler
        if (containerListener != null)
        {
            tabPane.removeContainerListener(containerListener);
            containerListener = null;
            if (htmlViews != null)
            {
                htmlViews.removeAllElements();
                htmlViews = null;
            }
        }
        if (tabChangeListener != null)
        {
            tabPane.removeChangeListener(tabChangeListener);
            tabChangeListener = null;
        }
        if (propertyChangeListener != null)
        {
            tabPane.removePropertyChangeListener(propertyChangeListener);
            propertyChangeListener = null;
        }

    }

    @Override
    protected ChangeListener createChangeListener()
    {
        return new TabSelectionHandler();
    }

    @Override
    protected void installKeyboardActions()
    {
        InputMap km
            = getMyInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        SwingUtilities.replaceUIInputMap(tabPane,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, km);
        km = getMyInputMap(JComponent.WHEN_FOCUSED);
        SwingUtilities.replaceUIInputMap(tabPane, JComponent.WHEN_FOCUSED, km);

        ActionMap am = createMyActionMap();

        SwingUtilities.replaceUIActionMap(tabPane, am);

        tabScroller.scrollForwardButton.setAction(am
                .get("scrollTabsForwardAction"));
        tabScroller.scrollBackwardButton.setAction(am
                .get("scrollTabsBackwardAction"));

    }

    InputMap getMyInputMap(int condition)
    {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        {
            return (InputMap) UIManager.get("TabbedPane.ancestorInputMap");
        }
        else if (condition == JComponent.WHEN_FOCUSED)
        {
            return (InputMap) UIManager.get("TabbedPane.focusInputMap");
        }
        return null;
    }

    ActionMap createMyActionMap()
    {
        ActionMap map = new ActionMapUIResource();
        map.put("navigateNext", new DirectionAction(NEXT));
        map.put("navigatePrevious", new DirectionAction(PREVIOUS));
        map.put("navigateRight", new DirectionAction(EAST));
        map.put("navigateLeft", new DirectionAction(WEST));
        map.put("navigateUp", new DirectionAction(NORTH));
        map.put("navigateDown", new DirectionAction(SOUTH));
        map.put("navigatePageUp", new PageAction(true));
        map.put("navigatePageDown", new PageAction(false));
        map.put("requestFocus", new RequestFocusAction());
        map.put("requestFocusForVisibleComponent",
                new RequestFocusForVisibleAction());
        map.put("setSelectedIndex", new SetSelectedIndexAction());
        map.put("scrollTabsForwardAction", new ScrollTabsForwardAction());
        map.put("scrollTabsBackwardAction", new ScrollTabsBackwardAction());
        return map;
    }

    @Override
    protected void uninstallKeyboardActions()
    {
        SwingUtilities.replaceUIActionMap(tabPane, null);
        SwingUtilities.replaceUIInputMap(tabPane,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
        SwingUtilities
                .replaceUIInputMap(tabPane, JComponent.WHEN_FOCUSED, null);
    }

    /**
     * Reloads the mnemonics. This should be invoked when a memonic changes,
     * when the title of a mnemonic changes, or when tabs are added/removed.
     */
    private void updateMnemonics()
    {
        resetMnemonics();
        for (int counter = tabPane.getTabCount() - 1; counter >= 0; counter--)
        {
            int mnemonic = tabPane.getMnemonicAt(counter);

            if (mnemonic > 0)
            {
                addMnemonic(counter, mnemonic);
            }
        }
    }

    /**
     * Resets the mnemonics bindings to an empty state.
     */
    private void resetMnemonics()
    {
        if (mnemonicToIndexMap != null)
        {
            mnemonicToIndexMap.clear();
            mnemonicInputMap.clear();
        }
    }

    /**
     * Adds the specified mnemonic at the specified index.
     */
    private void addMnemonic(int index, int mnemonic)
    {
        if (mnemonicToIndexMap == null)
        {
            initMnemonics();
        }

        mnemonicInputMap.put(KeyStroke.getKeyStroke(mnemonic, Event.ALT_MASK),
                "setSelectedIndex");

        mnemonicToIndexMap.put(mnemonic, index);
    }

    /**
     * Installs the state needed for mnemonics.
     */
    private void initMnemonics()
    {
        mnemonicToIndexMap = new Hashtable<Integer, Integer>();
        mnemonicInputMap = new InputMapUIResource();
        mnemonicInputMap.setParent(SwingUtilities.getUIInputMap(tabPane,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        SwingUtilities
                .replaceUIInputMap(tabPane,
                        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                        mnemonicInputMap);
    }

    // UI Rendering

    @Override
    public void paint(Graphics g, JComponent c)
    {
        int tc = tabPane.getTabCount();

        if (tabCount != tc) {
            tabCount = tc;
            updateMnemonics();
        }

        int selectedIndex = tabPane.getSelectedIndex();
        int tabPlacement = tabPane.getTabPlacement();

        ensureCurrentLayout();

        // Paint content border
        paintContentBorder(g, tabPlacement, selectedIndex);
    }

    @Override
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects,
            int tabIndex, Rectangle iconRect, Rectangle textRect)
    {
        Rectangle tabRect = rects[tabIndex];
        int selectedIndex = tabPane.getSelectedIndex();
        boolean isSelected = selectedIndex == tabIndex;
        boolean isOver = overTabIndex == tabIndex;
        Graphics2D g2 = null;
        Shape save = null;
        boolean cropShape = false;
        int cropx = 0;
        int cropy = 0;

        if (g instanceof Graphics2D)
        {
            g2 = (Graphics2D) g;

            AntialiasingManager.activateAntialiasing(g2);

            // Render visual for cropped tab edge...
            Rectangle viewRect = tabScroller.viewport.getViewRect();
            int cropline;

            cropline = viewRect.x + viewRect.width;
            if ((tabRect.x < cropline)
                    && (tabRect.x + tabRect.width > cropline))
            {

                cropx = cropline - 1;
                cropy = tabRect.y;
                cropShape = true;
            }

            if (cropShape)
            {
                save = g2.getClip();
                g2.clipRect(tabRect.x, tabRect.y, tabRect.width,
                                tabRect.height);
            }
        }

        paintTabBackground(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
                tabRect.width, tabRect.height, isSelected);

        paintTabBorder(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
                tabRect.width, tabRect.height, isSelected);

        String title = tabPane.getTitleAt(tabIndex);
        Font font = tabPane.getFont();
        FontMetrics metrics = g.getFontMetrics(font);
        Icon icon = getIconForTab(tabIndex);

        layoutLabel(tabPlacement, metrics, tabIndex, title, icon, tabRect,
                iconRect, textRect, isSelected);

        paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect,
                isSelected);

        paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);

        paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect,
                textRect, isSelected);

        if (cropShape)
        {
            paintCroppedTabEdge(g, tabPlacement, tabIndex, isSelected, cropx,
                    cropy);
            g2.setClip(save);
        }
        else if (isOver || isSelected)
        {
            Rectangle closeRect = newCloseRect(tabRect);

            int dx = closeRect.x;
            int dy = closeRect.y;

            if (isCloseButtonEnabled)
                paintCloseIcon(g2, dx, dy, isOver);
            if (isMaxButtonEnabled)
                paintMaxIcon(g2, dx, dy, isOver);
        }

    }

    protected void paintCloseIcon(Graphics g, int dx, int dy, boolean isOver)
    {
        // paintActionButton(g, dx, dy, closeIndexStatus, isOver, closeB,
        // closeImgB);
        g.drawImage(closeImgI, dx, dy + 1, null);
    }

    protected void paintMaxIcon(Graphics g, int dx, int dy, boolean isOver)
    {
        if (isCloseButtonEnabled)
            dx -= BUTTONSIZE;

        // paintActionButton(g, dx, dy, maxIndexStatus, isOver, maxB, maxImgB);
        g.drawImage(maxImgI, dx, dy + 1, null);
    }

    protected void paintActionButton(Graphics g, int dx, int dy, int status,
            boolean isOver, JButton button, Image image)
    {
        button.setBorder(null);

        if (isOver) {
            switch (status) {
            case OVER:
                button.setBorder(OVERBORDER);
                break;
            case PRESSED:
                button.setBorder(PRESSEDBORDER);
                break;
            }
        }

        button.setBackground(tabScroller.tabPanel.getBackground());
        button.paint(image.getGraphics());
        g.drawImage(image, dx, dy, null);
    }

    /*
     * This method will create and return a polygon shape for the given tab
     * rectangle which has been cropped at the specified cropline with a torn
     * edge visual. e.g. A "File" tab which has cropped been cropped just after
     * the "i": ------------- | ..... | | . | | ... . | | . . | | . . | | . . |
     * --------------
     *
     * The x, y arrays below define the pattern used to create a "torn" edge
     * segment which is repeated to fill the edge of the tab. For tabs placed on
     * TOP and BOTTOM, this righthand torn edge is created by line segments
     * which are defined by coordinates obtained by subtracting xCropLen[i] from
     * (tab.x + tab.width) and adding yCroplen[i] to (tab.y). For tabs placed on
     * LEFT or RIGHT, the bottom torn edge is created by subtracting xCropLen[i]
     * from (tab.y + tab.height) and adding yCropLen[i] to (tab.x).
     */

    //private static final int CROP_SEGMENT = 12;

    private void paintCroppedTabEdge(Graphics g, int tabPlacement,
            int tabIndex, boolean isSelected, int x, int y)
    {
        g.setColor(shadow);
        g.drawLine(x, y, x, y + rects[tabIndex].height);

    }

    private void ensureCurrentLayout()
    {
        if (!tabPane.isValid())
        {
            tabPane.validate();
        }
        /*
         * If tabPane doesn't have a peer yet, the validate() call will silently
         * fail. We handle that by forcing a layout if tabPane is still invalid.
         * See bug 4237677.
         */
        if (!tabPane.isValid())
        {
            TabbedPaneLayout layout = (TabbedPaneLayout) tabPane.getLayout();
            layout.calculateLayoutInfo();
        }
    }

    /**
     * Returns the bounds of the specified tab in the coordinate space of the
     * JTabbedPane component. This is required because the tab rects are by
     * default defined in the coordinate space of the component where they are
     * rendered, which could be the JTabbedPane (for WRAP_TAB_LAYOUT) or a
     * ScrollableTabPanel (SCROLL_TAB_LAYOUT). This method should be used
     * whenever the tab rectangle must be relative to the JTabbedPane itself and
     * the result should be placed in a designated Rectangle object (rather than
     * instantiating and returning a new Rectangle each time). The tab index
     * parameter must be a valid tabbed pane tab index (0 to tab count - 1,
     * inclusive). The destination rectangle parameter must be a valid
     * <code>Rectangle</code> instance. The handling of invalid parameters is
     * unspecified.
     *
     * @param tabIndex
     *            the index of the tab
     * @param dest
     *            the rectangle where the result should be placed
     * @return the resulting rectangle
     *
     * @since 1.4
     */

    @Override
    protected Rectangle getTabBounds(int tabIndex, Rectangle dest)
    {
        dest.width = rects[tabIndex].width;
        dest.height = rects[tabIndex].height;

        Point vpp = tabScroller.viewport.getLocation();
        Point viewp = tabScroller.viewport.getViewPosition();
        dest.x = rects[tabIndex].x + vpp.x - viewp.x;
        dest.y = rects[tabIndex].y + vpp.y - viewp.y;

        return dest;
    }

    private int getTabAtLocation(int x, int y)
    {
        ensureCurrentLayout();

        int tabCount = tabPane.getTabCount();
        for (int i = 0; i < tabCount; i++)
        {
            if (rects[i].contains(x, y))
            {
                return i;
            }
        }
        return -1;
    }

    public int getOverTabIndex()
    {
        return overTabIndex;
    }

    /**
     * Returns the index of the tab closest to the passed in location, note that
     * the returned tab may not contain the location x,y.
     */
    protected int getClosestTab(int x, int y)
    {
        int min = 0;
        int tabCount = Math.min(rects.length, tabPane.getTabCount());
        int max = tabCount;
        int tabPlacement = tabPane.getTabPlacement();
        boolean useX = (tabPlacement == TOP || tabPlacement == BOTTOM);
        int want = (useX) ? x : y;

        while (min != max)
        {
            int current = (max + min) / 2;
            int minLoc;
            int maxLoc;

            if (useX)
            {
                minLoc = rects[current].x;
                maxLoc = minLoc + rects[current].width;
            }
            else
            {
                minLoc = rects[current].y;
                maxLoc = minLoc + rects[current].height;
            }
            if (want < minLoc)
            {
                max = current;
                if (min == max)
                {
                    return Math.max(0, current - 1);
                }
            }
            else if (want >= maxLoc)
            {
                min = current;
                if (max - min <= 1)
                {
                    return Math.max(current + 1, tabCount - 1);
                }
            }
            else
            {
                return current;
            }
        }
        return min;
    }

    /**
     * Returns a point which is translated from the specified point in the
     * JTabbedPane's coordinate space to the coordinate space of the
     * ScrollableTabPanel. This is used for SCROLL_TAB_LAYOUT ONLY.
     */
    private Point translatePointToTabPanel(int srcx, int srcy, Point dest)
    {
        Point vpp = tabScroller.viewport.getLocation();
        Point viewp = tabScroller.viewport.getViewPosition();
        dest.x = srcx + vpp.x + viewp.x;
        dest.y = srcy + vpp.y + viewp.y;
        return dest;
    }

    // BasicTabbedPaneUI methods

    // Tab Navigation methods

    // REMIND(aim,7/29/98): This method should be made
    // protected in the next release where
    // API changes are allowed
    //
    boolean requestMyFocusForVisibleComponent()
    {
        Component visibleComponent = getVisibleComponent();
        if (visibleComponent.isFocusable())
        {
            visibleComponent.requestFocus();
            return true;
        }
        else if (visibleComponent instanceof JComponent)
        {
            if (((JComponent) visibleComponent).requestFocusInWindow())
            {
                return true;
            }
        }
        return false;
    }

    private static class DirectionAction
        extends AbstractAction
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private final int direction;

        public DirectionAction(int direction)
        {
            this.direction = direction;
        }

        public void actionPerformed(ActionEvent e)
        {
            JTabbedPane pane = (JTabbedPane) e.getSource();
            SIPCommTabbedPaneUI ui = (SIPCommTabbedPaneUI) pane.getUI();
            ui.navigateSelectedTab(direction);
        }
    };

    private static class PageAction
        extends AbstractAction
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private final boolean up;

        public PageAction(boolean up)
        {
            this.up = up;
        }

        public void actionPerformed(ActionEvent e)
        {
            JTabbedPane pane = (JTabbedPane) e.getSource();
            SIPCommTabbedPaneUI ui = (SIPCommTabbedPaneUI) pane.getUI();
            int tabPlacement = pane.getTabPlacement();
            if (tabPlacement == TOP || tabPlacement == BOTTOM)
            {
                ui.navigateSelectedTab(up ? WEST : EAST);
            }
            else
            {
                ui.navigateSelectedTab(up ? NORTH : SOUTH);
            }
        }
    };

    private static class RequestFocusAction
        extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            JTabbedPane pane = (JTabbedPane) e.getSource();
            pane.requestFocus();
        }
    };

    private static class RequestFocusForVisibleAction
        extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            JTabbedPane pane = (JTabbedPane) e.getSource();
            SIPCommTabbedPaneUI ui = (SIPCommTabbedPaneUI) pane.getUI();
            ui.requestMyFocusForVisibleComponent();
        }
    };

    /**
     * Selects a tab in the JTabbedPane based on the String of the action
     * command. The tab selected is based on the first tab that has a mnemonic
     * matching the first character of the action command.
     */
    private static class SetSelectedIndexAction
        extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            JTabbedPane pane = (JTabbedPane) e.getSource();

            if (pane != null && (pane.getUI() instanceof SIPCommTabbedPaneUI))
            {
                SIPCommTabbedPaneUI ui = (SIPCommTabbedPaneUI) pane.getUI();
                String command = e.getActionCommand();

                if (command != null && command.length() > 0)
                {
                    int mnemonic = e.getActionCommand().charAt(0);
                    if (mnemonic >= 'a' && mnemonic <= 'z')
                    {
                        mnemonic -= ('a' - 'A');
                    }

                    Integer index = ui.mnemonicToIndexMap.get(mnemonic);
                    if (index != null && pane.isEnabledAt(index.intValue()))
                    {
                        pane.setSelectedIndex(index.intValue());
                    }
                }
            }
        }
    };

    private static class ScrollTabsForwardAction
        extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            JTabbedPane pane = null;
            Object src = e.getSource();
            if (src instanceof JTabbedPane)
            {
                pane = (JTabbedPane) src;
            }
            else if (src instanceof ScrollableTabButton)
            {
                pane = (JTabbedPane) ((ScrollableTabButton) src).getParent();
            }
            else
            {
                return; // shouldn't happen
            }
            SIPCommTabbedPaneUI ui = (SIPCommTabbedPaneUI) pane.getUI();

            ui.tabScroller.scrollForward(pane.getTabPlacement());
        }
    }

    private static class ScrollTabsBackwardAction
        extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            JTabbedPane pane = null;
            Object src = e.getSource();
            if (src instanceof JTabbedPane)
            {
                pane = (JTabbedPane) src;
            }
            else if (src instanceof ScrollableTabButton)
            {
                pane = (JTabbedPane) ((ScrollableTabButton) src).getParent();
            }
            else
            {
                return; // shouldn't happen
            }
            SIPCommTabbedPaneUI ui = (SIPCommTabbedPaneUI) pane.getUI();

            ui.tabScroller.scrollBackward(pane.getTabPlacement());
        }
    }

    /**
     * This inner class is marked &quot;public&quot; due to a compiler bug. This
     * class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicTabbedPaneUI.
     */
    private class TabbedPaneScrollLayout
        extends TabbedPaneLayout
    {

        @Override
        protected int preferredTabAreaHeight(int tabPlacement, int width)
        {
            return calculateMaxTabHeight(tabPlacement);
        }

        @Override
        protected int preferredTabAreaWidth(int tabPlacement, int height)
        {
            return calculateMaxTabWidth(tabPlacement);
        }

        @Override
        public void layoutContainer(Container parent)
        {
            int tabPlacement = tabPane.getTabPlacement();
            int tabCount = tabPane.getTabCount();
            Insets insets = tabPane.getInsets();
            int selectedIndex = tabPane.getSelectedIndex();
            Component visibleComponent = getVisibleComponent();

            calculateLayoutInfo();

            if (selectedIndex < 0)
            {
                if (visibleComponent != null)
                {
                    // The last tab was removed, so remove the component
                    setVisibleComponent(null);
                }
            }
            else
            {
                Component selectedComponent =
                    tabPane.getComponentAt(selectedIndex);
                boolean shouldChangeFocus = false;

                // In order to allow programs to use a single component
                // as the display for multiple tabs, we will not change
                // the visible component if the currently selected tab
                // has a null component. This is a bit dicey, as we don't
                // explicitly state we support this in the spec, but since
                // programs are now depending on this, we're making it work.
                //
                if (selectedComponent != null)
                {
                    if (selectedComponent != visibleComponent
                        && visibleComponent != null)
                    {
                        if (KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                .getFocusOwner() != null)
                        {
                            shouldChangeFocus = true;
                        }
                    }
                    setVisibleComponent(selectedComponent);
                }
                int tx, ty, tw, th; // tab area bounds
                int cx, cy, cw, ch; // content area bounds
                Insets contentInsets = getContentBorderInsets(tabPlacement);
                Rectangle bounds = tabPane.getBounds();
                int numChildren = tabPane.getComponentCount();

                if (numChildren > 0)
                {

                    // calculate tab area bounds
                    tw = bounds.width - insets.left - insets.right;
                    th =
                        calculateTabAreaHeight(tabPlacement, runCount,
                            maxTabHeight);
                    tx = insets.left;
                    ty = insets.top;

                    // calculate content area bounds
                    cx = tx + contentInsets.left;
                    cy = ty + th + contentInsets.top;
                    cw =
                        bounds.width - insets.left - insets.right
                            - contentInsets.left - contentInsets.right;
                    ch =
                        bounds.height - insets.top - insets.bottom - th
                            - contentInsets.top - contentInsets.bottom;

                    for (int i = 0; i < numChildren; i++)
                    {
                        Component child = tabPane.getComponent(i);

                        if (child instanceof ScrollableTabViewport)
                        {
                            JViewport viewport = (JViewport) child;
                            Rectangle viewRect = viewport.getViewRect();
                            int vw = tw;
                            int vh = th;

                            int totalTabWidth =
                                rects[tabCount - 1].x
                                    + rects[tabCount - 1].width;
                            if (totalTabWidth > tw)
                            {
                                // Need to allow space for scrollbuttons
                                vw = Math.max(tw - 36, 36);
                                ;
                                if (totalTabWidth - viewRect.x <= vw)
                                {
                                    // Scrolled to the end, so ensure the
                                    // viewport size is
                                    // such that the scroll offset aligns with a
                                    // tab
                                    vw = totalTabWidth - viewRect.x;
                                }
                            }

                            child.setBounds(tx, ty, vw, vh);

                        }
                        else if (child instanceof ScrollableTabButton)
                        {
                            ScrollableTabButton scrollbutton =
                                (ScrollableTabButton) child;
                            Dimension bsize = scrollbutton.getPreferredSize();
                            int bx = 0;
                            int by = 0;
                            int bw = bsize.width;
                            int bh = bsize.height;
                            boolean visible = false;

                            int totalTabWidth =
                                rects[tabCount - 1].x
                                    + rects[tabCount - 1].width;

                            if (totalTabWidth > tw)
                            {
                                int dir =
                                    scrollbutton.scrollsForward() ? EAST : WEST;
                                scrollbutton.setDirection(dir);
                                visible = true;
                                bx =
                                    dir == EAST ? bounds.width - insets.left
                                        - bsize.width : bounds.width
                                        - insets.left - 2 * bsize.width;
                                by =
                                    (tabPlacement == TOP ? ty + th
                                        - bsize.height : ty);
                            }

                            child.setVisible(visible);
                            if (visible)
                            {
                                child.setBounds(bx, by, bw, bh);
                            }

                        }
                        else
                        {
                            // All content children...
                            child.setBounds(cx, cy, cw, ch);
                        }
                    }
                    if (shouldChangeFocus)
                    {
                        if (!requestMyFocusForVisibleComponent())
                        {
                            tabPane.requestFocus();
                        }
                    }
                }
            }
        }

        @Override
        protected void calculateTabRects(int tabPlacement, int tabCount)
        {
            FontMetrics metrics = getFontMetrics();;
            Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
            int i;

            int x = tabAreaInsets.left - 2;
            int y = tabAreaInsets.top;
            int totalWidth = 0;
            int totalHeight = 0;

            //
            // Calculate bounds within which a tab run must fit
            //
            maxTabHeight = calculateMaxTabHeight(tabPlacement);

            runCount = 0;
            selectedRun = -1;

            if (tabCount == 0)
                return;

            selectedRun = 0;
            runCount = 1;

            // Run through tabs and lay them out in a single run
            Rectangle rect;
            for (i = 0; i < tabCount; i++)
            {
                rect = rects[i];

                if (i > 0)
                {
                    rect.x = rects[i - 1].x + rects[i - 1].width - 1;
                }
                else
                {
                    tabRuns[0] = 0;
                    maxTabWidth = 0;
                    totalHeight += maxTabHeight;
                    rect.x = x;
                }
                rect.width = calculateTabWidth(tabPlacement, i, metrics);
                totalWidth = rect.x + rect.width;
                maxTabWidth = Math.max(maxTabWidth, rect.width);

                rect.y = y;
                rect.height = maxTabHeight /* - 2 */;
            }

            // tabPanel.setSize(totalWidth, totalHeight);
            tabScroller.tabPanel.setPreferredSize(new Dimension(totalWidth,
                    totalHeight));
        }
    }

    protected class ScrollableTabSupport implements ChangeListener
    {
        public ScrollableTabViewport viewport;

        public ScrollableTabPanel tabPanel;

        public ScrollableTabButton scrollForwardButton;

        public ScrollableTabButton scrollBackwardButton;

        public int leadingTabIndex;

        private Point tabViewPosition = new Point(0, 0);

        ScrollableTabSupport(int tabPlacement)
        {
            viewport = new ScrollableTabViewport();
            tabPanel = new ScrollableTabPanel();
            viewport.setView(tabPanel);
            viewport.addChangeListener(this);

            scrollForwardButton = createScrollableTabButton(EAST);
            scrollBackwardButton = createScrollableTabButton(WEST);
            // scrollForwardButton = new ScrollableTabButton(EAST);
            // scrollBackwardButton = new ScrollableTabButton(WEST);
        }

        public void scrollForward(int tabPlacement)
        {
            Dimension viewSize = viewport.getViewSize();
            Rectangle viewRect = viewport.getViewRect();

            if (tabPlacement == TOP || tabPlacement == BOTTOM)
            {
                if (viewRect.width >= viewSize.width - viewRect.x)
                    return; // no room left to scroll
            }
            else
            { // tabPlacement == LEFT || tabPlacement == RIGHT
                if (viewRect.height >= viewSize.height - viewRect.y)
                    return;
            }
            setLeadingTabIndex(tabPlacement, leadingTabIndex + 1);
        }

        public void scrollBackward(int tabPlacement)
        {
            if (leadingTabIndex == 0)
                return; // no room left to scroll

            setLeadingTabIndex(tabPlacement, leadingTabIndex - 1);
        }

        public void setLeadingTabIndex(int tabPlacement, int index)
        {
            leadingTabIndex = index;
            Dimension viewSize = viewport.getViewSize();
            Rectangle viewRect = viewport.getViewRect();

            tabViewPosition.x = leadingTabIndex == 0 ? 0
                    : rects[leadingTabIndex].x;

            if ((viewSize.width - tabViewPosition.x) < viewRect.width)
            {
                // We've scrolled to the end, so adjust the viewport size
                // to ensure the view position remains aligned on a tab boundary
                Dimension extentSize = new Dimension(viewSize.width
                        - tabViewPosition.x, viewRect.height);
                viewport.setExtentSize(extentSize);
            }

            viewport.setViewPosition(tabViewPosition);
        }

        public void stateChanged(ChangeEvent e)
        {
            JViewport viewport = (JViewport) e.getSource();
            int tabPlacement = tabPane.getTabPlacement();
            int tabCount = tabPane.getTabCount();
            Rectangle vpRect = viewport.getBounds();
            Dimension viewSize = viewport.getViewSize();
            Rectangle viewRect = viewport.getViewRect();

            leadingTabIndex = getClosestTab(viewRect.x, viewRect.y);

            // If the tab isn't right aligned, adjust it.
            if (leadingTabIndex + 1 < tabCount)
            {

                if (rects[leadingTabIndex].x < viewRect.x)
                    leadingTabIndex++;
            }
            Insets contentInsets = getContentBorderInsets(tabPlacement);

            tabPane.repaint(vpRect.x, vpRect.y + vpRect.height, vpRect.width,
                    contentInsets.top);
            scrollBackwardButton.setEnabled(viewRect.x > 0);
            scrollForwardButton.setEnabled(leadingTabIndex < tabCount - 1
                    && viewSize.width - viewRect.x > viewRect.width);

        }

        @Override
        public String toString()
        {
            return new String("viewport.viewSize=" + viewport.getViewSize()
                    + "\n" + "viewport.viewRectangle=" + viewport.getViewRect()
                    + "\n" + "leadingTabIndex=" + leadingTabIndex + "\n"
                    + "tabViewPosition=" + tabViewPosition);
        }

    }

    protected static class ScrollableTabViewport
        extends JViewport
        implements UIResource
    {
        private static final long serialVersionUID = 0L;

        public ScrollableTabViewport()
        {
            setOpaque(false);
            setScrollMode(SIMPLE_SCROLL_MODE);
        }
    }

    private class ScrollableTabPanel
        extends TransparentPanel
        implements UIResource
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        public ScrollableTabPanel()
        {
            setLayout(null);
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            SIPCommTabbedPaneUI.this.paintTabArea(g, tabPane.getTabPlacement(),
                    tabPane.getSelectedIndex());
        }
    }

    protected static class ScrollableTabButton
        extends BasicArrowButton
        implements UIResource, SwingConstants
    {
        private static final long serialVersionUID = 0L;

        public ScrollableTabButton(int direction)
        {
            super(direction, UIManager.getColor("TabbedPane.selected"),
                    UIManager.getColor("TabbedPane.shadow"), UIManager
                            .getColor("TabbedPane.darkShadow"), UIManager
                            .getColor("TabbedPane.highlight"));
        }

        public boolean scrollsForward()
        {
            return direction == EAST || direction == SOUTH;
        }
    }

    /**
     * This inner class is marked &quot;public&quot; due to a compiler bug. This
     * class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicTabbedPaneUI.
     */
    private class TabSelectionHandler implements ChangeListener
    {
        public void stateChanged(ChangeEvent e)
        {
            JTabbedPane tabPane = (JTabbedPane) e.getSource();
            tabPane.revalidate();
            tabPane.repaint();

            if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT)
            {
                int index = tabPane.getSelectedIndex();

                if (index < rects.length && index != -1)
                    tabScroller.tabPanel.scrollRectToVisible(rects[index]);
            }
        }
    }

    /**
     * This inner class is marked &quot;public&quot; due to a compiler bug. This
     * class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicTabbedPaneUI.
     */

    /*
     * GES 2/3/99: The container listener code was added to support HTML
     * rendering of tab titles.
     *
     * Ideally, we would be able to listen for property changes when a tab is
     * added or its text modified. At the moment there are no such events
     * because the Beans spec doesn't allow 'indexed' property changes (i.e. tab
     * 2's text changed from A to B).
     *
     * In order to get around this, we listen for tabs to be added or removed by
     * listening for the container events. we then queue up a runnable (so the
     * component has a chance to complete the add) which checks the tab title of
     * the new component to see if it requires HTML rendering.
     *
     * The Views (one per tab title requiring HTML rendering) are stored in the
     * htmlViews Vector, which is only allocated after the first time we run
     * into an HTML tab. Note that this vector is kept in step with the number
     * of pages, and nulls are added for those pages whose tab title do not
     * require HTML rendering.
     *
     * This makes it easy for the paint and layout code to tell whether to
     * invoke the HTML engine without having to check the string during
     * time-sensitive operations.
     *
     * When we have added a way to listen for tab additions and changes to tab
     * text, this code should be removed and replaced by something which uses
     * that.
     */
    private class ContainerHandler implements ContainerListener
    {
        public void componentAdded(ContainerEvent e)
        {
            JTabbedPane tp = (JTabbedPane) e.getContainer();
            Component child = e.getChild();
            if (child instanceof UIResource)
                return;

            int index = tp.indexOfComponent(child);
            String title = tp.getTitleAt(index);

            boolean isHTML = BasicHTML.isHTMLString(title);
            if (isHTML)
            {
                if (htmlViews == null)
                { // Initialize vector
                    htmlViews = createHTMLVector();
                }
                else
                { // Vector already exists
                    View v = BasicHTML.createHTMLView(tp, title);
                    htmlViews.insertElementAt(v, index);
                }
            }
            else
            { // Not HTML
                if (htmlViews != null)
                { // Add placeholder
                    htmlViews.insertElementAt(null, index);
                } // else nada!
            }
        }

        public void componentRemoved(ContainerEvent e)
        {
            JTabbedPane tp = (JTabbedPane) e.getContainer();
            Component child = e.getChild();
            if (child instanceof UIResource)
                return;

            // NOTE 4/15/2002 (joutwate):
            // This fix is implemented using client properties since there is
            // currently no IndexPropertyChangeEvent. Once
            // IndexPropertyChangeEvents have been added this code should be
            // modified to use it.
            Integer indexObj = (Integer) tp
                    .getClientProperty("__index_to_remove__");
            if (indexObj != null)
            {
                int index = indexObj.intValue();
                if (htmlViews != null && htmlViews.size() >= index)
                {
                    htmlViews.removeElementAt(index);
                }
            }
        }
    }

    private Vector<View> createHTMLVector()
    {
        Vector<View> htmlViews = new Vector<View>();
        int count = tabPane.getTabCount();
        if (count > 0) {
            for (int i = 0; i < count; i++)
            {
                String title = tabPane.getTitleAt(i);
                if (BasicHTML.isHTMLString(title))
                {
                    htmlViews.addElement(BasicHTML.createHTMLView(tabPane,
                            title));
                }
                else
                {
                    htmlViews.addElement(null);
                }
            }
        }
        return htmlViews;
    }

    private class MyMouseHandler extends MouseHandler
    {
        @Override
        public void mousePressed(MouseEvent e)
        {
            if (closeIndexStatus == OVER)
            {
                closeIndexStatus = PRESSED;
                tabScroller.tabPanel.repaint();
            }
            else if (maxIndexStatus == OVER)
            {
                maxIndexStatus = PRESSED;
                tabScroller.tabPanel.repaint();
            }
            else
            {
                super.mousePressed(e);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (e.getClickCount() > 1 && overTabIndex != -1)
            {
                ((SIPCommTabbedPane) tabPane).fireDoubleClickTabEvent(e,
                        overTabIndex);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            updateOverTab(e.getX(), e.getY());

            if (overTabIndex == -1) {
                if (e.isPopupTrigger())
                    ((SIPCommTabbedPane) tabPane).firePopupOutsideTabEvent(e);
                return;
            }

            if (isOneActionButtonEnabled() && e.isPopupTrigger())
            {
                super.mousePressed(e);

                closeIndexStatus = INACTIVE; // Prevent undesired action when
                maxIndexStatus = INACTIVE; // right-clicking on icons

                actionPopupMenu.show(tabScroller.tabPanel, e.getX(), e.getY());
                return;
            }

            if (closeIndexStatus == PRESSED)
            {
                closeIndexStatus = OVER;
                tabScroller.tabPanel.repaint();
                ((SIPCommTabbedPane) tabPane)
                    .fireCloseTabEvent(e, overTabIndex);
                return;
            }

            if (maxIndexStatus == PRESSED)
            {
                maxIndexStatus = OVER;
                tabScroller.tabPanel.repaint();
                ((SIPCommTabbedPane) tabPane).fireMaxTabEvent(e, overTabIndex);
                return;
            }

            // Allow tabs closing with mouse middle button
            if (e.getButton() == MouseEvent.BUTTON2)
                ((SIPCommTabbedPane) tabPane).fireCloseTabEvent(e, overTabIndex);
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (!mousePressed)
            {
                overTabIndex = -1;
                tabScroller.tabPanel.repaint();
            }
        }

    }

    private class MyMouseMotionListener
        implements MouseMotionListener
    {
        public void mouseMoved(MouseEvent e)
        {
            if (actionPopupMenu.isVisible())
                return; // No updates when popup is visible
            mousePressed = false;
            setTabIcons(e.getX(), e.getY());
        }

        public void mouseDragged(MouseEvent e)
        {
            if (actionPopupMenu.isVisible())
                return; // No updates when popup is visible
            mousePressed = true;
            setTabIcons(e.getX(), e.getY());
        }
    }

    /**
     * We don't want to have a content border.
     */
    @Override
    protected void paintContentBorder(  Graphics g,
                                        int tabPlacement,
                                        int selectedIndex)
    {}

    /**
     * Reloads close icon.
     */
    public void loadSkin()
    {
        closeImgI = DesktopUtilActivator.getImage(CLOSE_TAB_ICON);

        maxImgI = new BufferedImage(BUTTONSIZE, BUTTONSIZE,
                BufferedImage.TYPE_4BYTE_ABGR);
    }
}
