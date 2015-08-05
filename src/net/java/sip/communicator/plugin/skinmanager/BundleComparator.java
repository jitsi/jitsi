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
package net.java.sip.communicator.plugin.skinmanager;

import java.util.*;

import org.osgi.framework.*;

/**
 * Comparator for bundle array sort
 *
 * @author ROTH Damien
 */
public class BundleComparator
    implements Comparator<Bundle>
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
