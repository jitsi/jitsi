package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>CallRenderer</tt> represents a renderer for a call. All user
 * interfaces representing a call should implement this interface.
 *
 * @author Yana Stamcheva
 */
public interface CallRenderer
{
    /**
     * Returns the call represented by this call renderer.
     * @return the call represented by this call renderer
     */
    public Call getCall();

    /**
     * Enters in full screen mode.
     */
    public void enterFullScreen();

    /**
     * Exits the full screen mode.
     */
    public void exitFullScreen();

    /**
     * Ensures the size of the window.
     * @param component the component, which size should be considered
     * @param width the desired width
     * @param height the desired height
     */
    public void ensureSize(Component component, int width, int height);

    /**
     * Returns the parent call dialog, where this renderer is contained.
     * @return the parent call dialog, where this renderer is contained
     */
    public CallDialog getCallDialog();

    /**
     * Returns the <tt>CallPeerRenderer</tt> corresponding to the given
     * <tt>callPeer</tt>.
     * @param callPeer the <tt>CallPeer</tt>, for which we're looking for a
     * renderer
     * @return the <tt>CallPeerRenderer</tt> corresponding to the given
     * <tt>callPeer</tt>
     */
    public CallPeerRenderer getCallPeerRenderer(CallPeer callPeer);
}
