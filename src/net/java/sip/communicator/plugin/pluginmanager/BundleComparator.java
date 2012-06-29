/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.pluginmanager;

import java.util.*;

import org.osgi.framework.*;

/**
 * Comparator for bundle array sort
 * 
 * @author ROTH Damien
 */
public class BundleComparator implements Comparator<Bundle>
{
    /**
     * Compares the bundles using their "Bundle-Name"s.
     * @param arg0 the first bundle to compare
     * @param arg1 the second bundle to compare
     * @return the result of the string comparison between the names of the two 
     * bundles
     */
    public int compare(Bundle arg0, Bundle arg1)
    {
        String n1 = (String) arg0.getHeaders().get(Constants.BUNDLE_NAME);
        String n2 = (String) arg1.getHeaders().get(Constants.BUNDLE_NAME);

        if (n1 == null)
        {
            n1 = "unknown";
        }
        if (n2 == null)
        {
            n2 = "unknown";
        }

        return n1.compareTo(n2);
    }
}
