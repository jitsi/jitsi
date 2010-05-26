/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

import javax.swing.*;

/**
 * @author Lubomir Marinov
 */
public class VideoLayout extends FitLayout
{
    public static final String CENTER_REMOTE = "CENTER_REMOTE";

    public static final String EAST_REMOTE = "EAST_REMOTE";

    public static final String LOCAL = "LOCAL";

    private static final float LOCAL_TO_REMOTE_RATIO = 0.30f;

    private Component local;

    private Component remote;

    private float remoteAlignmentX = Component.CENTER_ALIGNMENT;

    @Override
    public void addLayoutComponent(String name, Component comp)
    {
        super.addLayoutComponent(name, comp);

        if ((name == null) || name.equals(CENTER_REMOTE))
        {
            remote = comp;
            remoteAlignmentX = Component.CENTER_ALIGNMENT;
        }
        else if (name.equals(EAST_REMOTE))
        {
            remote = comp;
            remoteAlignmentX = Component.RIGHT_ALIGNMENT;
        }
        else if (name.equals(LOCAL))
            local = comp;
    }

    @Override
    protected Component getComponent(Container parent)
    {
        return getRemote();
    }

    public Component getLocal()
    {
        return local;
    }

    public Component getRemote()
    {
        return remote;
    }

    public void layoutContainer(Container parent)
    {
        Component local = getLocal();

        super.layoutContainer(parent,
            (local == null) ? Component.CENTER_ALIGNMENT : remoteAlignmentX);

        if (local != null)
        {
            Dimension parentSize = parent.getSize();
            int height = Math.round(parentSize.height * LOCAL_TO_REMOTE_RATIO);
            int width = Math.round(parentSize.width * LOCAL_TO_REMOTE_RATIO);

            /*
             * XXX The remote Component being a JLabel is meant to signal that
             * there is no remote video and the remote is the photoLabel.
             */
            if (remote instanceof JLabel)
            {
                super.layoutComponent(
                    local,
                    new Rectangle(
                        parentSize.width/2 - width/2,
                        parentSize.height - height,
                        Math.round(parentSize.width * LOCAL_TO_REMOTE_RATIO),
                        height),
                    Component.CENTER_ALIGNMENT,
                    Component.BOTTOM_ALIGNMENT);
            }
            else
            {
                super.layoutComponent(
                    local,
                    new Rectangle(
                        remote.getX() + 5,
                        parentSize.height - height - 5,
                        Math.round(parentSize.width * LOCAL_TO_REMOTE_RATIO),
                        height),
                    Component.LEFT_ALIGNMENT,
                    Component.BOTTOM_ALIGNMENT);
            }
        }
    }

    public Dimension minimumLayoutSize(Container parent)
    {
        // TODO Auto-generated method stub
        return super.minimumLayoutSize(parent);
    }

    public Dimension preferredLayoutSize(Container parent)
    {
        // TODO Auto-generated method stub
        return super.preferredLayoutSize(parent);
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        super.removeLayoutComponent(comp);

        if (remote == comp)
            remote = null;
        else if (local == comp)
            local = null;
    }
}
