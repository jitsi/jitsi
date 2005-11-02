/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * This class represents the notion of a Contact or Buddy, that is widely used
 * in instant messaging today. From a protocol point of view, a contact is
 * generally considered to be another user of the service that proposes the
 * protocol
 *
 *
 * @author Emil Ivov
 */
public class Contact
{
    private String address = null;
    private byte[] image   = null;
    private Hashtable contactProperties = new Hashtable();
    private boolean isLocal = false;

    public Contact(String address, boolean isLocal)
    {
        this.address = address;
        this.isLocal = isLocal;
    }

    //address

    public String getAddress()
    {
        return address;
    }

    public void setAddres(String address)
    {
        this.address = address;
    }

    //image
    public byte[] getImage()
    {
        return image;
    }

    public void setImage(byte[] image)
    {
        this.image = image;
    }

    //properties
    public void setProperty(String name, Object property)
    {
        contactProperties.put(name, property);
    }

    public Object getProperty(String name)
    {
        return contactProperties.get(name);
    }

    //islocal
    public boolean isLocal()
    {
        return isLocal;
    }
}
