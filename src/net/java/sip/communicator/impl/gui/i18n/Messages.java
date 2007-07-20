/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.i18n;

import java.text.*;
import java.util.*;
/**
 * The Messages class manages the access to the internationalization
 * properties files.
 * @author Yana Stamcheva
 */
public class Messages {
    private static final String BUNDLE_NAME 
        = "net.java.sip.communicator.impl.gui.i18n.messages";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static I18NString getI18NString(String key) {
        
        I18NString i18nString = new I18NString();
        
        String resourceString;
        try {
            resourceString = RESOURCE_BUNDLE.getString(key);
            
            int mnemonicIndex = resourceString.indexOf('&');
            
            if(mnemonicIndex > -1) {
                i18nString.setMnemonic(resourceString.charAt(mnemonicIndex + 1));
                
                String firstPart = resourceString.substring(0, mnemonicIndex);
                String secondPart = resourceString.substring(mnemonicIndex + 1);
                
                resourceString = firstPart.concat(secondPart);
            }
            
            i18nString.setText(resourceString);

        } catch (MissingResourceException e) {

            i18nString.setText('!' + key + '!');
        }
        
        return i18nString;
    }

    /**
     * Returns an internationalized string corresponding to the given key,
     * by replacing all occurences of '?' with the given string param.
     * @param key The key of the string.
     * @param params the params, that should replace {1}, {2}, etc. in the
     * string given by the key parameter 
     * @return An internationalized string corresponding to the given key,
     * by replacing all occurences of '?' with the given string param.
     */
    public static I18NString getI18NString(String key, String[] params) {
        
        I18NString i18nString = new I18NString();
        
        String resourceString;
        
        try {
            resourceString = RESOURCE_BUNDLE.getString(key);
            
            resourceString = MessageFormat.format(
                resourceString, (Object[]) params);
            
            int mnemonicIndex = resourceString.indexOf('&');
            
            if(mnemonicIndex > -1) {
                i18nString.setMnemonic(resourceString.charAt(mnemonicIndex + 1));
                
                String firstPart = resourceString.substring(0, mnemonicIndex);
                String secondPart = resourceString.substring(mnemonicIndex + 1);
                
                resourceString = firstPart.concat(secondPart);
            }
            
            i18nString.setText(resourceString);

        } catch (MissingResourceException e) {

            i18nString.setText('!' + key + '!');
        }
        
        return i18nString;
    }
}
