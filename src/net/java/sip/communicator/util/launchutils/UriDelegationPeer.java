/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.launchutils;

/**
 * The <tt>UriDelegationPeer</tt> is used as a mechanism to pass arguments from
 * the UriArgManager which resides in "launcher space" to our argument
 * delegation service implementation that lives as an osgi bundle. An instance
 * of this peer is created from within the argument delegation service impl
 * and is registered with the UriArgManager.
 *
 * @author Emil Ivov
 */
public interface UriDelegationPeer
{
    /**
     * Handles <tt>uriArg</tt> in whatever way it finds fit.
     *
     * @param uriArg the uri argument that this delegate has to handle.
     */
    public void handleUri(String uriArg);
}
