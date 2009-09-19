/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

/**
 * This exception is thrown when Facebook returns an error instead of the
 * expected response.
 * 
 * @author Edgar Poce
 */
@SuppressWarnings("serial")
public class FacebookErrorException extends Exception
{
    public static final int kError_Global_ValidationError = 1346001;

    public static final int kError_Login_GenericError = 1348009;

    public static final int kError_Platform_CallbackValidationFailure = 1349007;

    public static final int kError_Platform_ApplicationResponseInvalid = 1349008;

    public static final int kError_Chat_NotAvailable = 1356002;

    public static final int kError_Chat_SendOtherNotAvailable = 1356003;

    public static final int kError_Chat_TooManyMessages = 1356008;

    public static final int kError_Async_NotLoggedIn = 1357001;

    public static final int kError_Async_LoginChanged = 1357003;

    public static final int kError_Async_CSRFCheckFailed = 1357004;
    // Bad Parameter; There was an error understanding
    // the request.
    public static final int Error_Async_BadParameter = 1357005;

    public static final int Error_Global_NoError = 0;

    public static final int Error_Async_HttpConnectionFailed = 1001;

    public static final int Error_Async_UnexpectedNullResponse = 1002;

    public static final int Error_System_UIDNotFound = 1003;

    public static final int Error_System_ChannelNotFound = 1004;

    public static final int Error_System_PostFormIDNotFound = 1005;

    public static final int Error_Global_PostMethodError = 1006;

    public static final int Error_Global_GetMethodError = 1007;

    public static final int Error_Global_JSONError = 1008;

    private FacebookSession session;
    
    private final int code;

    /**
     * Constructor which uses the given {@link FacebookSession} to try to take
     * the necessary actions according to the given error code
     * 
     * @param session
     * @param code
     */
    public FacebookErrorException(FacebookSession session, int code)
    {
        super("Facebook error " + code);
        this.code = code;
        if (code == FacebookErrorException.kError_Async_NotLoggedIn
                || code == FacebookErrorException.kError_Async_LoginChanged)
        {
            session.logout();
        }
    }

    public int getCode()
    {
        return code;
    }

    public FacebookSession getSession()
    {
        return session;
    }
}
