/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.i18n;

public class I18NString
{

    private String text;
    
    private char mnemonic;

    public char getMnemonic()
    {
        return mnemonic;
    }

    public void setMnemonic(char mnemonic)
    {
        this.mnemonic = mnemonic;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
}
