/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist;

/**
 * Exceptions of this class get thrown whenever an error occurs while modifying
 * the contents of the MetaContactList. Depending on the type of error that
 * caused them  MetaContactListException-s come with a corresponding error code
 * which may have a value among one of the CODE_XXX static fields.
 *
 * @author Emil Ivov
 */
public class MetaContactListException
    extends RuntimeException
{
    /**
     * Indicates that an error has occurred while performing a local IO
     * operation (e.g. while writing in a file).
     */
    public static final int CODE_LOCAL_IO_ERROR = 1;

    /**
     * Indicates that a failure has occurred while trying to communicate
     * through the network.
     */
    public static final int CODE_NETWORK_ERROR  = 2;

    /**
     * Indicates that the exception was caused by the fact that we tried to
     * add to our contact list a contact that was already in there.
     */
    public static final int CODE_CONTACT_ALREADY_EXISTS_ERROR  = 3;

    /**
     * Indicates that the exception was caused by the fact that we tried to
     * add to our contact list a group that was already in there.
     */
    public static final int CODE_GROUP_ALREADY_EXISTS_ERROR  = 4;


    /**
     * Indicates that the error which caused the exception was either unknown
     * or did not correspond to any of the other error codes
     */
    public static final int CODE_UNKNOWN_ERROR  = 5;

    /**
     * An error code indicating the nature of this excepiton.
     */
    private int errCode = CODE_UNKNOWN_ERROR;

    /**
     * Creates a MetaContactListException with the specified message cause and
     * code.
     * @param message a human readable message describing the exception.
     * @param cause the Exception/Error (if any) that caused this Exception.
     * @param code one of the statuc CODE_XXX variable, describing the nature
     * of the exception.
     */
    public MetaContactListException(String message, Exception cause, int code)
    {
        super(message, cause);
        this.errCode = code;
    }

    /**
     * Creates a MetaContactListException with the specified message error code.
     *
     * @param message a human readable message describing the exception.
     * @param code one of the statuc CODE_XXX variable, describing the nature
     * of the exception.
     */
    public MetaContactListException(String message, int code)
    {
        super(message);
        this.errCode = code;
    }


    /**
     * Returns an int describing the nature of the exception.
     * @return one of the static int CODE_XXX fields of this class.
     */
    public int getErrorCode()
    {
        return errCode;
    }
}
