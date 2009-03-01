/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import java.io.*;

import net.java.sip.communicator.util.xml.*;

/**
 * @author Lubomir Marinov
 */
public interface ConfigurationStore
{
    public Object getProperty(String name);

    public String[] getPropertyNames();

    public boolean isSystem(String name);

    public void reloadConfiguration(File file) throws IOException, XMLException;

    public void removeProperty(String name);

    public void setNonSystemProperty(String name, Object value);

    public void setSystemProperty(String name);

    public void storeConfiguration(OutputStream out) throws IOException;
}
