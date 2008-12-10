/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>AccountsConfigurationForm</tt> is the form where the user could
 * create, modify or delete an account.
 * 
 * @author Yana Stamcheva
 */
public class AccountsConfigurationForm
    extends AbstractConfigurationForm
{
    private Object form;

    /**
     * Returns the form of this configuration form.
     * 
     * @return the form of this configuration form.
     */
    public Object getForm()
    {
        if (form == null)
            form = super.getForm();
        return form;
    }

    protected String getFormClassName()
    {
        return "net.java.sip.communicator.impl.gui.main.account.AccountsConfigurationPanel";
    }

    /**
     * Returns the icon of this configuration form.
     * 
     * @return the icon of this configuration form.
     */
    public byte[] getIcon()
    {
        return ImageLoader.getImageInBytes(ImageLoader.ACCOUNT_ICON);
    }

    public int getIndex()
    {
        return 1;
    }

    /**
     * Returns the title of this configuration form.
     * 
     * @return the title of this configuration form.
     */
    public String getTitle()
    {
        return GuiActivator.getResources()
            .getI18NString("service.gui.ACCOUNTS");
    }
}
