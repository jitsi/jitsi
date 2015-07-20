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
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.plaf.*;

/**
 * A button which text is a link. The button looks like a link.
 */
public class SIPCommLinkButton
    extends JButton
{
    private static final long serialVersionUID = 1L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String UIClassID = "LinkButtonUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(UIClassID,
            SIPCommLinkButtonUI.class.getName());
    }

    public static final int ALWAYS_UNDERLINE = 0;

    public static final int HOVER_UNDERLINE = 1;

    public static final int NEVER_UNDERLINE = 2;

    private int linkBehavior;

    private Color linkColor;

    private Color colorPressed;

    private Color visitedLinkColor;

    private Color disabledLinkColor;

    private URL buttonURL;

    private boolean isLinkVisited;

    /**
     * Created Link Button.
     */
    public SIPCommLinkButton()
    {
        this(null, null);
    }

    /**
     * Created Link Button with text.
     * @param text
     */
    public SIPCommLinkButton(String text)
    {
        this(text, null);
    }

    /**
     * Created Link Button with url.
     * @param url
     */
    public SIPCommLinkButton(URL url)
    {
        this(null, url);
    }

    /**
     * Created Link Button with text and url.
     * @param text
     * @param url
     */
    public SIPCommLinkButton(String text, URL url)
    {
        super(text);

        linkBehavior = SIPCommLinkButton.HOVER_UNDERLINE;

        linkColor = Color.blue;
        colorPressed = Color.red;
        visitedLinkColor = new Color(128, 0, 128);

        if (text == null && url != null)
          this.setText(url.toExternalForm());
        setLinkURL(url);

        this.setBorderPainted(false);
        this.setContentAreaFilled(false);
        this.setRolloverEnabled(true);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public String getUIClassID()
    {
        return SIPCommLinkButton.UIClassID;
    }

    /**
     * Setup the tooltip.
     */
    protected void setupToolTipText()
    {
        String tip = null;
        if (buttonURL != null)
            tip = buttonURL.toExternalForm();
        setToolTipText(tip);
    }

    /**
     * Changes link behaviour.
     * @param bnew the new behaviour. One of ALWAYS_UNDERLINE, HOVER_UNDERLINE
     *        and NEVER_UNDERLINE.
     */
    public void setLinkBehavior(int bnew)
    {
        if (bnew != ALWAYS_UNDERLINE && bnew != HOVER_UNDERLINE
                && bnew != NEVER_UNDERLINE)
            throw new IllegalArgumentException("Not a legal LinkBehavior");

        int old = linkBehavior;
        linkBehavior = bnew;
        firePropertyChange("linkBehavior", old, bnew);
        repaint();
    }

    /**
     * Returns the link behaviour.
     * @return the link behaviour.
     */
    public int getLinkBehavior()
    {
        return linkBehavior;
    }

    /**
     * Sets the link color.
     * @param color the new color.
     */
    public void setLinkColor(Color color)
    {
        Color colorOld = linkColor;
        linkColor = color;
        firePropertyChange("linkColor", colorOld, color);
        repaint();
    }

    /**
     * Return the link color.
     * @return link color.
     */
    public Color getLinkColor()
    {
        return linkColor;
    }

    /**
     * Sets the active link color.
     * @param colorNew the new color.
     */
    public void setActiveLinkColor(Color colorNew)
    {
        Color colorOld = colorPressed;
        colorPressed = colorNew;
        firePropertyChange("activeLinkColor", colorOld, colorNew);
        repaint();
    }

    /**
     * Returns the active link color.
     * @return the active link color.
     */
    public Color getActiveLinkColor()
    {
        return colorPressed;
    }

    /**
     * Sets disabled link color.
     * @param color the new color.
     */
    public void setDisabledLinkColor(Color color)
    {
        Color colorOld = disabledLinkColor;
        disabledLinkColor = color;
        firePropertyChange("disabledLinkColor", colorOld, color);
        if (!isEnabled())
            repaint();
    }

    /**
     * Returns the disabled link color.
     * @return the disabled link color.
     */
    public Color getDisabledLinkColor()
    {
        return disabledLinkColor;
    }

    /**
     * Set visited link color.
     * @param colorNew the new visited link color.
     */
    public void setVisitedLinkColor(Color colorNew)
    {
        Color colorOld = visitedLinkColor;
        visitedLinkColor = colorNew;
        firePropertyChange("visitedLinkColor", colorOld, colorNew);
        repaint();
    }

    /**
     * Returns visited link color.
     * @return visited link color.
     */
    public Color getVisitedLinkColor()
    {
        return visitedLinkColor;
    }

    /**
     * Set a link.
     * @param url the url.
     */
    public void setLinkURL(URL url)
    {
        URL urlOld = buttonURL;
        buttonURL = url;
        setupToolTipText();
        firePropertyChange("linkURL", urlOld, url);
        revalidate();
        repaint();
    }

    /**
     * Returns the url.
     * @return the link url.
     */
    public URL getLinkURL()
    {
        return buttonURL;
    }

    /**
     * Set a link visited.
     * @param flagNew is link visited.
     */
    public void setLinkVisited(boolean flagNew)
    {
        boolean flagOld = isLinkVisited;
        isLinkVisited = flagNew;
        firePropertyChange("linkVisited", flagOld, flagNew);
        repaint();
    }

    /**
     * Returns is link visited.
     * @return is link visited.
     */
    public boolean isLinkVisited()
    {
        return isLinkVisited;
    }
}
