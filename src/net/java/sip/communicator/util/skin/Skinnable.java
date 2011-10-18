/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.skin;

/**
 * Interface that represents all skinnable user interface components. Any
 * component interested in being reloaded after a new skin installation should
 * implement this interface.
 *
 * @author Adam Netocny
 */
public interface Skinnable
{
    /**
     * Loads the skin for this skinnable. This method is meant to be used by
     * user interface components interested in being skinnable. This is where
     * all images, forgrounds and backgrounds should be loaded in order new
     * skin to take effect.
     */
    public void loadSkin();
}
