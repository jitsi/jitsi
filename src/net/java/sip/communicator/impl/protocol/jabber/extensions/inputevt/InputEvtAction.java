/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt;

/**
 * Enumeration about the possible actions for an InputEvt IQ.
 *
 * @author Sebastien Vincent
 */
public enum InputEvtAction
{
    /**
     * The <tt>notify</tt> action.
     */
    NOTIFY("notify");

    /**
     * The name of this direction.
     */
    private final String actionName;

    /**
     * Creates a <tt>InputEvtAction</tt> instance with the specified name.
     *
     * @param actionName the name of the <tt>InputEvtAction</tt> we'd like
     * to create.
     */
    private InputEvtAction(String actionName)
    {
        this.actionName = actionName;
    }

    /**
     * Returns the name of this <tt>InputEvtAction</tt>. The name returned by
     * this method is meant for use directly in the XMPP XML string.
     *
     * @return Returns the name of this <tt>InputEvtAction</tt>.
     */
    @Override
    public String toString()
    {
        return actionName;
    }

    /**
     * Returns a <tt>InputEvtAction</tt> value corresponding to the specified
     * <tt>inputActionStr</tt>.
     *
     * @param inputActionStr the action <tt>String</tt> that we'd like to
     * parse.
     * @return a <tt>InputEvtAction</tt> value corresponding to the specified
     * <tt>inputActionStr</tt>.
     * @throws IllegalArgumentException in case <tt>inputActionStr</tt> is
     * not valid
     */
    public static InputEvtAction parseString(String inputActionStr)
        throws IllegalArgumentException
    {
        for (InputEvtAction value : values())
            if (value.toString().equals(inputActionStr))
                return value;

        throw new IllegalArgumentException(
            inputActionStr + " is not a valid Input action");
    }
}
