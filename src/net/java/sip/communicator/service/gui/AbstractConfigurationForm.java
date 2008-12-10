/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.lang.reflect.*;

/**
 * @author Lubomir Marinov
 */
public abstract class AbstractConfigurationForm
    implements ConfigurationForm
{
    public Object getForm()
    {
        Exception exception;
        try
        {
            return Class.forName(getFormClassName(), true,
                getClass().getClassLoader()).newInstance();
        }
        catch (ClassNotFoundException ex)
        {
            exception = ex;
        }
        catch (IllegalAccessException ex)
        {
            exception = ex;
        }
        catch (InstantiationException ex)
        {
            exception = ex;
        }
        throw new UndeclaredThrowableException(exception);
    }

    protected abstract String getFormClassName();

    public int getIndex()
    {
        return -1;
    }
}
