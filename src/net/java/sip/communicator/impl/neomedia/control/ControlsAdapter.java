/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.control;

/**
 * Provides a default implementation of <tt>Controls</tt> which does not expose
 * any controls.
 *
 * @author Lubomir Marinov
 */
public class ControlsAdapter
    extends AbstractControls
{

    /**
     * The constant which represents an empty array of controls. Explicitly
     * defined in order to avoid unnecessary allocations.
     */
    public static final Object[] EMPTY_CONTROLS = new Object[0];

    /**
     * Implements {@link javax.media.Controls#getControls()}. Gets the controls
     * available for the owner of this instance. The current implementation
     * returns an empty array because it has no available controls.
     *
     * @return an array of <tt>Object</tt>s which represent the controls
     * available for the owner of this instance
     */
    public Object[] getControls()
    {
        return EMPTY_CONTROLS;
    }
}
