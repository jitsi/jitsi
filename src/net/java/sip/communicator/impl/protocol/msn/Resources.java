/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.protocol.msn;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The Resources class manages the access to the internationalization
 * properties files.
 *
 * @author Yana Stamcheva
 */
public class Resources
{

    /**
     * Logger for this class.
     */
    private static Logger log = Logger.getLogger(Resources.class);

    /**
     * Name of the bundle were we will search for localized string.
     */
    private static final String BUNDLE_NAME
        = "net.java.sip.communicator.impl.protocol.msn.resources";

    /**
     * Bundle which handle access to localized resources.
     */
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        try
        {
            return RESOURCE_BUNDLE.getString(key);

        }
        catch (MissingResourceException e) {

            return '!' + key + '!';
        }
    }
}
