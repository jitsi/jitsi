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
package net.java.sip.communicator.service.protocol.jabber;

import net.java.sip.communicator.util.*;
import org.jivesoftware.smack.provider.*;


/**
 *
 * This class abstracts interactions within Smack XMPP library (mostly with
 * its <tt>ProviderManager</tt> class).
 * This exists because <tt>JingleIQProvider</tt> and <tt>ColibriIQProvider</tt>
 * are in need to be used with Smack v4, where <tt>ProviderManager</tt>
 * has a different interface.
 *
 * @author Maksym Kulish
 */
abstract public class AbstractSmackInteroperabilityLayer {

    /**
     * The <tt>Logger</tt> used by the 
     * <tt>AbstractSmackInteroperabilityLayer</tt> class for
     * reporting on improper implementations instantiation
     */
    private static final Logger logger = Logger.getLogger(
            AbstractSmackInteroperabilityLayer.class);
    
    /**
     * The implementation class to be used within its Jitsi application
     */
    private static Class<AbstractSmackInteroperabilityLayer> 
            implementationClass;

    /**
     *  The instance of Smack interoperability layer implementation class
     */
    private static AbstractSmackInteroperabilityLayer
            interopLayerInstance;

    /**
     * Get the instance of Smack interoperability layer implementation class
     * 
     * @return Smack interoperation layer implementation class
     */
    public static AbstractSmackInteroperabilityLayer getInstance() 
    {
        if (interopLayerInstance == null) 
        {
            try
            {
                interopLayerInstance = 
                        implementationClass.newInstance();
            }
            catch (IllegalAccessException e)
            {
                // Never thrown within proper implementation
                logger.fatal("Your AbstractSmackInteroperabilityLayer " +
                        "implementation " +
                        "cannot be accessed properly. " +
                        "Please fix the implementation");
            }
            catch (InstantiationException e)
            {
                // Never thrown within proper implementation
                logger.fatal("Your AbstractSmackInteroperabilityLayer " +
                        "implementation " +
                        "cannot be instantiated properly. " +
                        "Please fix the implementation");
            }
        }
        return interopLayerInstance;
    }
    
    /**
     * Set the Smack interoperation layer
     * implementation class to be used within this Jitsi application
     * @param implementationClass Smack interoperation layer 
     *                            implementation class 
     */
    public static void setImplementationClass(
            Class implementationClass) 
    {
        AbstractSmackInteroperabilityLayer.implementationClass 
                = implementationClass;
    }
    
    
    /**
     * Add <tt>PacketExtensionProvider</tt> to the list of known
     * providers
     * 
     * @param elementName The element name where the matching is happening
     * @param namespace The XML namespace used in that element
     * @param provider <tt>PacketExtensionProvider</tt> implementation to be 
     *                 used
     */
    abstract public void addExtensionProvider(
            String elementName, String namespace, Object provider);

    /**
     * Add <tt>IQProvider</tt> to the list of known
     * providers
     *
     * @param elementName The element name where the matching is happening
     * @param namespace The XML namespace used in that element
     * @param provider <tt>IQProvider</tt> implementation to be 
     *                 used
     */
    abstract public void addIQProvider(
            String elementName, String namespace, Object provider);

    /**
     * Get the <tt>PacketExtensionProvider</tt> for given element name and XML 
     * namespace
     * 
     * @param elementName The element name where the matching is happening
     * @param namespace The XML namespace used in that element
     * @return <tt>PacketExtensionProvider</tt> implementation to be 
     *                 used
     */
    abstract public PacketExtensionProvider getExtensionProvider(
            String elementName, String namespace);

    
}
