/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.lang.reflect.*;

import net.java.sip.communicator.service.gui.internal.*;
import net.java.sip.communicator.service.resources.*;

/**
 * @author Lubomir Marinov
 */
public class LazyConfigurationForm
    implements ConfigurationForm
{
    private static ResourceManagementService resources;

    private static ResourceManagementService getResources()
    {
        if (resources == null)
            resources =
                ResourceManagementServiceUtils.getService(GuiServiceActivator
                    .getBundleContext());
        return resources;
    }

    private final ClassLoader formClassLoader;

    private final String formClassName;

    private final String iconID;

    private final int index;

    private final String titleID;

    public LazyConfigurationForm(String formClassName,
        ClassLoader formClassLoader, String iconID, String titleID)
    {
        this(formClassName, formClassLoader, iconID, titleID, -1);
    }

    public LazyConfigurationForm(String formClassName,
        ClassLoader formClassLoader, String iconID, String titleID, int index)
    {
        this.formClassName = formClassName;
        this.formClassLoader = formClassLoader;
        this.iconID = iconID;
        this.titleID = titleID;
        this.index = index;
    }

    public Object getForm()
    {
        Exception exception;
        try
        {
            return Class
                .forName(getFormClassName(), true, getFormClassLoader())
                .newInstance();
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

    protected ClassLoader getFormClassLoader()
    {
        return formClassLoader;
    }

    protected String getFormClassName()
    {
        return formClassName;
    }

    public byte[] getIcon()
    {
        return getResources().getImageInBytes(getIconID());
    }

    protected String getIconID()
    {
        return iconID;
    }

    public int getIndex()
    {
        return index;
    }

    public String getTitle()
    {
        return getResources().getI18NString(getTitleID());
    }

    protected String getTitleID()
    {
        return titleID;
    }
}
