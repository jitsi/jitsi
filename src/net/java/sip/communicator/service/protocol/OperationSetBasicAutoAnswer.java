/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * An Operation Set defining option
 * to unconditional auto answer incoming calls.
 *
 * @author Damian Minkov
 */
public interface OperationSetBasicAutoAnswer
    extends OperationSet
{
    /**
     * Auto answer unconditional account property.
     */
    public static final String AUTO_ANSWER_UNCOND_PROP =
        "AUTO_ANSWER_UNCONDITIONAL";

    /**
     * Sets the auto answer option to unconditionally answer all incoming calls.
     */
    public void setAutoAnswerUnconditional();

    /**
     * Is the auto answer option set to unconditionally
     * answer all incoming calls.
     * @return is auto answer set to unconditional.
     */
    public boolean isAutoAnswerUnconditionalSet();

    /**
     * Clear any previous settings.
     */
    public void clear();
}
