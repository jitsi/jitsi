/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

/**
 * Facebook Error Code
 * 
 * @author Dai Zhiwei
 */
public class FacebookErrorCode
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
    //Bad Parameter; There was an error understanding
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
}
