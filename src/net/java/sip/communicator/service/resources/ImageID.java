/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

/**
 * Represents the Image Identifier.
 */
public class ImageID
{
    private final String id;

    public ImageID(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }
}
