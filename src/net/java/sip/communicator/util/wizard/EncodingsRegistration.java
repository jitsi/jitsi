/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.wizard;

import java.util.*;

/**
 * An interface to get/set settings in the encodings panel.
 * 
 * @author Boris Grozev
 */
public interface EncodingsRegistration
{
   /**
    * Get the stored encoding properties
    * @return The stored encoding properties.
    */
   Map<String, String> getEncodingProperties();
   
   /**
    * Set the encoding properties
    * @param encodingProperties The encoding properties to set.
    */
   void setEncodingProperties(Map<String, String> encodingProperties);
   
   /**
    * Whether override encodings is enabled
    * @return Whether override encodings is enabled
    */
   boolean isOverrideEncodings();
   
   /**
    * Set the override encodings setting to <tt>override</tt>
    * @param override The value to set the override encoding settings to.
    */
   void setOverrideEncodings(boolean override);
}
