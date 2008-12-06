/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.i18n;

import java.text.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.resources.*;

/**
 * The Messages class manages the access to the internationalization properties
 * files.
 * 
 * @author Yana Stamcheva
 */
public class Messages {

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static I18NString getI18NString(String key) {        
        I18NString i18nString = new I18NString();

        try {
            ResourceManagementService resources = GuiActivator.getResources();
            String resourceString = resources.getI18NString(key);
            char mnemonic = resources.getI18nMnemonic(key);

            if(mnemonic > 0) 
                i18nString.setMnemonic(mnemonic);            
            i18nString.setText(resourceString);

        } catch (MissingResourceException e) {
            i18nString.setText('!' + key + '!');
        }
        
        return i18nString;
    }

    /**
     * Returns an internationalized string corresponding to the given key, by
     * replacing all occurrences of '?' with the given string param.
     * 
     * @param key The key of the string.
     * @param params the params, that should replace {1}, {2}, etc. in the
     *            string given by the key parameter
     * @return An internationalized string corresponding to the given key, by
     *         replacing all occurrences of '?' with the given string param.
     */
    public static I18NString getI18NString(String key, String[] params) {        
        I18NString i18nString = new I18NString();

        try {
            ResourceManagementService resources = GuiActivator.getResources();
            String resourceString = resources.getI18NString(key);

            // Escape the single quote
            resourceString = resourceString.replaceAll("'", "''");
            resourceString =
                MessageFormat.format(resourceString, (Object[]) params);

            char mnemonic = resources.getI18nMnemonic(key);

            if(mnemonic > 0) 
                i18nString.setMnemonic(mnemonic);
            i18nString.setText(resourceString);

        } catch (MissingResourceException e) {
            i18nString.setText('!' + key + '!');
        }
        
        return i18nString;
    }
}
