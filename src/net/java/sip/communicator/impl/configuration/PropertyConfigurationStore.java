/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import java.io.*;
import java.util.*;

/**
 * @author Lubomir Marinov
 */
public class PropertyConfigurationStore
    implements ConfigurationStore
{
    private final Properties properties = new Properties();

    public Object getProperty(String name)
    {
        Object value = properties.get(name);

        return (value != null) ? value : System.getProperty(name);
    }

    public String[] getPropertyNames()
    {
        synchronized (properties)
        {
            Set<Object> propertyNames = properties.keySet();
            return propertyNames.toArray(new String[propertyNames.size()]);
        }
    }

    public boolean isSystem(String name)
    {
        return (System.getProperty(name) != null);
    }

    public void reloadConfiguration(File file) throws IOException
    {
        properties.clear();

        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try
        {
            properties.load(in);
        }
        finally
        {
            in.close();
        }
    }

    public void removeProperty(String name)
    {
        properties.remove(name);
    }

    public void setNonSystemProperty(String name, Object value)
    {
        properties.setProperty(name, value.toString());
    }

    public void setSystemProperty(String name)
    {
        removeProperty(name);
    }

    public void storeConfiguration(OutputStream out) throws IOException
    {
        properties.store(out, null);
    }
}
