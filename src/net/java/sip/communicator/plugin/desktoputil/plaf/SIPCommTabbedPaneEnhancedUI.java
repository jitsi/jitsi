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
import java.awt.image.*;
import java.util.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

/**
 * This UI displays a different interface, which is independent from the look
 * and feel.
 *
 * @author David Bismut, davidou@mageos.com
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommTabbedPaneEnhancedUI
    extends SIPCommTabbedPaneUI
    implements Skinnable
{
    private static Color TAB_HIGHLIGHT_FOREGROUND_COLOR
        = new Color(DesktopUtilActivator.getResources()
            .getColor("service.gui.TAB_TITLE_HIGHLIGHT"));

    private static Color TAB_SELECTED_FOREGROUND_COLOR
        = new Color(DesktopUtilActivator.getResources()
            .getColor("service.gui.TAB_TITLE_SELECTED"));

    private static final int TAB_OVERLAP
        = Integer.parseInt(DesktopUtilActivator.getResources().
            getSettingsString("impl.gui.TAB_OVERLAP"));

    private static final int PREFERRED_WIDTH = 150;

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a selected tab.
     */
    private static final String SELECTED_TAB_LEFT_BG =
        "service.gui.lookandfeel.SELECTED_TAB_LEFT_BG";

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a selected tab.
     */
    private static final String SELECTED_TAB_MIDDLE_BG =
        "service.gui.lookandfeel.SELECTED_TAB_MIDDLE_BG";

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a selected tab.
     */
    private static final String SELECTED_TAB_RIGHT_BG =
        "service.gui.lookandfeel.SELECTED_TAB_RIGHT_BG";

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a tab.
     */
    private static final String TAB_LEFT_BG =
        "service.gui.lookandfeel.TAB_LEFT_BG";

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a tab.
     */
    private static final String TAB_MIDDLE_BG =
        "service.gui.lookandfeel.TAB_MIDDLE_BG";

    /**
     * The image used in the <tt>SIPCommLookAndFeel</tt> to paint the background
     * of a tab.
     */
    private static final String TAB_RIGHT_BG =
        "service.gui.lookandfeel.TAB_RIGHT_BG";

    protected final java.util.List<Integer> highlightedTabs
        = new Vector<Integer>();

    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommTabbedPaneEnhancedUI();
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement,
            Rectangle[] rects, int tabIndex, Rectangle iconRect,
            Rectangle textRect, boolean isSelected) {}

    /**
     * Overriden to paint nothing.
     */
    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
            int x, int y, int w, int h, boolean isSelected) {}

    @Override
    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement,
            int selectedIndex, int x, int y, int w, int h)
    {
        if (tabPane.getTabCount() < 1)
            return;

        g.setColor(shadow);
        g.drawLine(x, y, x + w - 2, y);
    }

    @Override
    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement,
            int selectedIndex, int x, int y, int w, int h)
    {
        if (tabPane.getTabCount() < 1)
            return;

        g.setColor(shadow);

        g.drawLine(x, y, x, y + h - 3);
    }

    @Override
    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement,
            int selectedIndex, int x, int y, int w, int h)
    {
        if (tabPane.getTabCount() < 1)
            return;

        g.setColor(shadow);
        g.drawLine(x + 1, y + h - 3, x + w - 2, y + h - 3);
        g.drawLine(x + 1, y + h - 2, x + w - 2, y + h - 2);
        g.setColor(shadow.brighter());
        g.drawLine(x + 2, y + h - 1, x + w - 1, y + h - 1);

    }

    @Override
    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement,
            int selectedIndex, int x, int y, int w, int h)
    {
        if (tabPane.getTabCount() < 1)
            return;

        g.setColor(shadow);

        g.drawLine(x + w - 3, y + 1, x + w - 3, y + h - 3);
        g.drawLine(x + w - 2, y + 1, x + w - 2, y + h - 3);
        g.setColor(shadow.brighter());
        g.drawLine(x + w - 1, y + 2, x + w - 1, y + h - 2);

    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement,
        int tabIndex, int x, int y, int w, int h, boolean isSelected)
    {
        g = g.create();
        try
        {
            internalPaintTabBackground(g, tabPlacement, tabIndex, x, y, w, h,
                isSelected);
        }
        finally
        {
            g.dispose();
        }
    }

    private void internalPaintTabBackground(Graphics g, int tabPlacement,
        int tabIndex, int x, int y, int w, int h, boolean isSelected)
    {
        BufferedImage leftImg = null;
        BufferedImage middleImg = null;
        BufferedImage rightImg = null;

        Graphics2D g2 = (Graphics2D) g;

        AntialiasingManager.activateAntialiasing(g2);

        int tabOverlap = 0;

        if (isSelected)
        {
            if (tabPane.isEnabledAt(tabIndex))
            {
                leftImg = DesktopUtilActivator.getImage(SELECTED_TAB_LEFT_BG);
                middleImg = DesktopUtilActivator.getImage(SELECTED_TAB_MIDDLE_BG);
                rightImg = DesktopUtilActivator.getImage(SELECTED_TAB_RIGHT_BG);

                tabOverlap = TAB_OVERLAP;
            }
            else
            {
                leftImg = DesktopUtilActivator.getImage(TAB_LEFT_BG);
                middleImg = DesktopUtilActivator.getImage(TAB_MIDDLE_BG);
                rightImg = DesktopUtilActivator.getImage(TAB_RIGHT_BG);
            }
        }
        else
        {
            leftImg = DesktopUtilActivator.getImage(TAB_LEFT_BG);
            middleImg = DesktopUtilActivator.getImage(TAB_MIDDLE_BG);
            rightImg = DesktopUtilActivator.getImage(TAB_RIGHT_BG);
        }

        // Remove the existing gap between the tabs and the panel, which is due
        // to the removal of the tabbed pane border.
        y++;
        // If the current tab is not the selected tab we paint it 2 more pixels
        // to the bottom in order to create the disabled look.
        if (!isSelected)
            y+=2;

        g2.drawImage(leftImg, x, y, null);
        g2.drawImage(middleImg, x + leftImg.getWidth(), y,
            w - leftImg.getWidth() - rightImg.getWidth() + tabOverlap,
            leftImg.getHeight(), null);
        g2.drawImage(rightImg, x + w - rightImg.getWidth() + tabOverlap, y, null);
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font,
            FontMetrics metrics, int tabIndex, String title,
            Rectangle textRect, boolean isSelected)
    {
        g.setFont(font);

        int titleWidth = SwingUtilities.computeStringWidth(metrics, title);

        int preferredWidth = 0;
        if (isOneActionButtonEnabled()) {
            preferredWidth = calculateTabWidth(tabPlacement, tabIndex, metrics)
                - WIDTHDELTA - 15;

            if (isCloseEnabled())
                preferredWidth -= BUTTONSIZE;

            if (isMaxEnabled())
                preferredWidth -= BUTTONSIZE;
        }
        else
        {
            preferredWidth = titleWidth;
        }

        while (titleWidth > preferredWidth)
        {
            if (title.endsWith("..."))
                title = title.substring(0, title.indexOf("...") - 1)
                        .concat("...");
            else
                title = title.substring(0, title.length() - 4)
                        .concat("...");

            titleWidth = SwingUtilities.computeStringWidth(metrics, title);
        }

        textRect.width = titleWidth;

        View v = getTextViewForTab(tabIndex);
        if (v != null)
        {
            // html
            v.paint(g, textRect);
        }
        else
        {
            // plain text
            int mnemIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);

            if (tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex))
            {
                if (isSelected)
                    g.setColor(TAB_SELECTED_FOREGROUND_COLOR);
                else
                {
                    if (this.isTabHighlighted(tabIndex))
                    {
                        g.setColor(TAB_HIGHLIGHT_FOREGROUND_COLOR);
                    }
                    else
                        g.setColor(tabPane.getForegroundAt(tabIndex));
                }

                BasicGraphicsUtils
                        .drawString(g, title, mnemIndex,
                                textRect.x, textRect.y + metrics.getAscent());
            }
            else
            { // tab disabled
                g.setColor(tabPane.getBackgroundAt(tabIndex).brighter());
                BasicGraphicsUtils
                        .drawStringUnderlineCharAt(g, title, mnemIndex,
                                textRect.x, textRect.y + metrics.getAscent());

                g.setColor(tabPane.getBackgroundAt(tabIndex).darker());
                BasicGraphicsUtils.drawStringUnderlineCharAt(g, title,
                        mnemIndex, textRect.x - 1, textRect.y
                                + metrics.getAscent() - 1);
            }
        }
    }

    protected class ScrollableTabButton extends
            SIPCommTabbedPaneUI.ScrollableTabButton
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        public ScrollableTabButton(int direction)
        {
            super(direction);
            setRolloverEnabled(true);
        }

        @Override
        public Dimension getPreferredSize()
        {
            return new Dimension(16, calculateMaxTabHeight(0));
        }

        @Override
        public void paint(Graphics g)
        {
            Color origColor;
            boolean isPressed, isRollOver, isEnabled;
            int w, h, size;

            w = getWidth();
            h = getHeight();
            origColor = g.getColor();
            isPressed = getModel().isPressed();
            isRollOver = getModel().isRollover();
            isEnabled = isEnabled();

            g.setColor(getBackground());
            g.fillRect(0, 0, w, h);

            g.setColor(shadow);
            // Using the background color set above
            if (direction == WEST) {
                g.drawLine(0, 0, 0, h - 1); // left
                g.drawLine(w - 1, 0, w - 1, 0); // right
            } else
                g.drawLine(w - 2, h - 1, w - 2, 0); // right

            g.drawLine(0, 0, w - 2, 0); // top

            if (isRollOver)
            {
                // do highlights or shadows
                Color color1;
                Color color2;

                if (isPressed)
                {
                    color2 = Color.WHITE;
                    color1 = shadow;
                }
                else
                {
                    color1 = Color.WHITE;
                    color2 = shadow;
                }

                g.setColor(color1);

                if (direction == WEST) {
                    g.drawLine(1, 1, 1, h - 1); // left
                    g.drawLine(1, 1, w - 2, 1); // top
                    g.setColor(color2);
                    g.drawLine(w - 1, h - 1, w - 1, 1); // right
                } else {
                    g.drawLine(0, 1, 0, h - 1);
                    g.drawLine(0, 1, w - 3, 1); // top
                    g.setColor(color2);
                    g.drawLine(w - 3, h - 1, w - 3, 1); // right
                }

            }

            // g.drawLine(0, h - 1, w - 1, h - 1); //bottom

            // If there's no room to draw arrow, bail
            if (h < 5 || w < 5) {
                g.setColor(origColor);
                return;
            }

            if (isPressed) {
                g.translate(1, 1);
            }

            // Draw the arrow
            size = Math.min((h - 4) / 3, (w - 4) / 3);
            size = Math.max(size, 2);

            boolean highlight = false;

            if(!highlightedTabs.isEmpty() &&
                ((direction == WEST
                    && tabScroller.scrollBackwardButton.isEnabled())
                || (direction == EAST
                    && tabScroller.scrollForwardButton.isEnabled())))
            {
                Rectangle viewRect = tabScroller.viewport.getViewRect();

                if(direction == WEST)
                {
                    int leadingTabIndex = getClosestTab(viewRect.x, viewRect.y);

                    for(int i = 0; i < leadingTabIndex; i++)
                    {
                        if(highlightedTabs.contains(i)
                           && !isScrollTabVisible(i))
                        {
                            highlight = true;
                            break;
                        }
                    }
                }
                else
                {
                    int leadingTabIndex =
                        getClosestTab(viewRect.x + viewRect.y, viewRect.y);

                    for(int i = leadingTabIndex; i < tabPane.getTabCount(); i++)
                    {
                        if(highlightedTabs.contains(i)
                            && !isScrollTabVisible(i))
                        {
                            highlight = true;
                            break;
                        }
                    }
                }

                if(highlight)
                {
                    Image img = DesktopUtilActivator.getImage(
                        direction == WEST ?
                            "service.gui.icons.TAB_UNREAD_BACKWARD_ICON"
                            : "service.gui.icons.TAB_UNREAD_FORWARD_ICON");

                    int wi = img.getWidth(null);

                    g.drawImage(img,
                                (w - wi)/2,
                                (h - size) / 2 - 2/* 2 borders 1px width*/,
                                null);
                }
            }

            if(!highlight)
                paintTriangle(g, (w - size) / 2, (h - size) / 2,
                    size, direction, isEnabled);

            // Reset the Graphics back to it's original settings
            if (isPressed) {
                g.translate(-1, -1);
            }
            g.setColor(origColor);
        }
    }

    /**
     * Checks whether the <tt>tabIndex</tt> is visible in the scrollable
     * tabs list.
     *
     * @param tabIndex the tab index to check.
     * @return whether <tt>tabIndex</tt> is visible in the list of scrollable
     * tabs.
     */
    private boolean isScrollTabVisible(int tabIndex)
    {
        Rectangle tabRect = rects[tabIndex];
        Rectangle viewRect = tabScroller.viewport.getViewRect();
        if ((tabRect.x + tabRect.width - BUTTONSIZE < viewRect.x)
            || (tabRect.x + BUTTONSIZE > viewRect.x + viewRect.width))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    protected SIPCommTabbedPaneUI.ScrollableTabButton createScrollableTabButton(
            int direction)
    {
        return new ScrollableTabButton(direction);
    }


    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex,
            FontMetrics metrics)
    {
        int width = super.calculateTabWidth(tabPlacement, tabIndex, metrics);

        if (isOneActionButtonEnabled())
        {
            if(width > PREFERRED_WIDTH)
                width = PREFERRED_WIDTH;
        }

        return width + WIDTHDELTA;
    }

    public void tabAddHightlight(int tabIndex)
    {
        this.highlightedTabs.add(tabIndex);
    }

    public void tabRemoveHighlight(int tabIndex)
    {
        Iterator<Integer> highlightedIter = highlightedTabs.iterator();

        while (highlightedIter.hasNext())
        {
            if (highlightedIter.next().intValue() == tabIndex)
            {
                highlightedIter.remove();
                break;
            }
        }
    }

    public boolean isTabHighlighted(int tabIndex)
    {
        return highlightedTabs.contains(tabIndex);
    }

    /**
     * Reloads color info.
     */
    @Override
    public void loadSkin()
    {
        super.loadSkin();

        TAB_HIGHLIGHT_FOREGROUND_COLOR = new Color(DesktopUtilActivator.getResources()
            .getColor("service.gui.TAB_TITLE_HIGHLIGHT"));

        TAB_SELECTED_FOREGROUND_COLOR = new Color(DesktopUtilActivator.getResources()
            .getColor("service.gui.TAB_TITLE_SELECTED"));
    }
}
