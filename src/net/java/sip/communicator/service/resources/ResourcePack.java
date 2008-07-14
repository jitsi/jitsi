/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

/**
 *
 * @author Damian Minkov
 */
public interface ResourcePack
{
    public String RESOURCE_NAME = "ResourceName";
    
    public String getResourcePackBaseName();
    public String getName();
    public String getDescription();
}
