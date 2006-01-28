/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Contains a list of property names that would be needed for the installation
 * of a protocol account. These property names are meant for use by
 * <p>
 * This interface is meant to serve as a contract between
 * service implementors on the one hand, who would be needing these properties
 * in order to actually create and install an account, and service users on the
 * other hand, who would need to specify the before requestion account
 * installation.
 * @author Emil Ivov
 */
public interface AccountProperties
{
    public static final String PASSWORD = "Password";
}
