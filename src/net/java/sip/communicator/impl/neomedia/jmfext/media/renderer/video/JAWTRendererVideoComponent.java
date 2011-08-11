/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.video;

import java.awt.*;

/**
 * Implements an AWT <tt>Component</tt> in which <tt>JAWTRenderer</tt> paints.
 *
 * @author Lyubomir Marinov
 */
public class JAWTRendererVideoComponent
    extends Canvas
{
    /**
     * The <tt>JAWTRenderer</tt> which paints in this
     * <tt>JAWTRendererVideoComponent</tt>.
     */
    protected final JAWTRenderer renderer;

    /**
     * The indicator which determines whether the native counterpart of this
     * <tt>JAWTRenderer</tt> wants <tt>paint</tt> calls on its AWT
     * <tt>Component</tt> to be delivered. For example, after the native
     * counterpart has been able to acquire the native handle of the AWT
     * <tt>Component</tt>, it may be able to determine when the native
     * handle needs painting without waiting for AWT to call <tt>paint</tt>
     * on the <tt>Component</tt>. In such a scenario, the native counterpart
     * may indicate with <tt>false</tt> that it does not need further
     * <tt>paint</tt> deliveries.
     */
    private boolean wantsPaint = true;

    /**
     * Initializes a new <tt>JAWTRendererVideoComponent</tt> instance.
     *
     * @param renderer
     */
    public JAWTRendererVideoComponent(JAWTRenderer renderer)
    {
        this.renderer = renderer;
    }

    @Override
    public void addNotify()
    {
        super.addNotify();

        wantsPaint = true;
    }

    /**
     * Gets the handle of the native counterpart of the
     * <tt>JAWTRenderer</tt> which paints in this
     * <tt>AWTVideoComponent</tt>.
     *
     * @return the handle of the native counterpart of the
     * <tt>JAWTRenderer</tt> which paints in this <tt>AWTVideoComponent</tt>
     */
    protected long getHandle()
    {
        return renderer.getHandle();
    }

    /**
     * Gets the synchronization lock which protects the access to the
     * <tt>handle</tt> property of this <tt>AWTVideoComponent</tt>.
     *
     * @return the synchronization lock which protects the access to the
     * <tt>handle</tt> property of this <tt>AWTVideoComponent</tt>
     */
    protected Object getHandleLock()
    {
        return renderer.getHandleLock();
    }

    @Override
    public void paint(Graphics g)
    {
        synchronized (getHandleLock())
        {
            long handle = getHandle();

            if (wantsPaint && (handle != 0))
                wantsPaint = JAWTRenderer.paint(handle, this, g);
        }
    }

    @Override
    public void removeNotify()
    {
        /*
         * In case the associated JAWTRenderer has said that it does not
         * want paint events/notifications, ask it again next time because
         * the native handle of this Canvas may be recreated.
         */
        wantsPaint = true;

        super.removeNotify();
    }

    @Override
    public void update(Graphics g)
    {
        /*
         * Skip the filling with the background color because it causes
         * flickering.
         */
        paint(g);
    }
}
