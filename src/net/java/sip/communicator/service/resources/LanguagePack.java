/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

import java.util.*;

/**
 *
 * @author Damian Minkov
 */
public interface LanguagePack 
    extends ResourcePack
{
    public String RESOURCE_NAME_DEFAULT_VALUE = "DefaultLanguagePack";
    
    public Iterator getAvailableLocales();
}
