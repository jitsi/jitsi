/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol;

/**
 * <tt>OperationFailedException</tt> indicates an exception that occurred in the
 * API.
 * <p>
 * <tt>OperationFailedException</tt> contains an error code that gives more
 * information on the exception. The application can obtain the error code using
 * {@link OperationFailedException#getErrorCode()}. The error code values are
 * defined in the <tt>OperationFailedException</tt> fields.
 * </p>
 *
 * @author Emil Ivov
 */
public class OperationFailedException
    extends Exception
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Set when no other error code can describe the exception that occurred.
     */
    public static final int GENERAL_ERROR = 1;

    /**
     * Set when command fails due to a failure in network communications or
     * a transport error.
     */
    public static final int NETWORK_FAILURE = 2;

    /**
     * Set to indicate that a provider needs to be registered or signed on
     * a public service before calling the method that threw the exception.
     */
    public static final int PROVIDER_NOT_REGISTERED = 3;

    /**
     * Set when an operation fails for implementation specific reasons.
     */
    public static final int INTERNAL_ERROR = 4;

    /**
     * Set when an operation fails for an error that has occurred on the server
     * side.
     */
    public static final int INTERNAL_SERVER_ERROR = 500;

    /**
     * Indicates that a user has tried to subscribe to a contact that already
     * had an active subscription.
     */
    public static final int SUBSCRIPTION_ALREADY_EXISTS = 5;

    /**
     * Indicates that a user has tried to create a group that already exist.
     */
    public static final int CONTACT_GROUP_ALREADY_EXISTS = 6;

    /**
     * Indicates that a user has entered wrong account properties, like wrong
     * port for example.
     */
    public static final int INVALID_ACCOUNT_PROPERTIES = 7;

    /**
     * Indicates that authentication with a server has failed.
     */
    public static final int AUTHENTICATION_FAILED = 401;

    /**
     * Indicates that the user is currently not allowed to perform the operation
     * that failed.
     */
    public static final int FORBIDDEN = 403;

    /**
     * Indicates that the user is trying to perform the current operation on a
     * resource that does not exist.
     */
    public static final int NOT_FOUND = 404;

    /**
     * Indicates that the user is trying to perform an operation with an
     * identifier that was already in use on the target resource (e.g. log with
     * a nickname that is already in use in a chat room, or create a chat room
     * on a server that already contains a room with the same ID).
     */
    public static final int IDENTIFICATION_CONFLICT = 10;

    /**
     * Indicates that the exception was thrown because a method has been
     * passed an illegal or inappropriate argument.
     */
    public static final int ILLEGAL_ARGUMENT = 11;

    /**
     * Indicates that the exception was thrown, because the user doesn't have
     * enough privileges. Meant to be used by multi user chat to indicate that
     * the user is trying to make an operation, which requires admin or owner
     * privileges.
     */
    public static final int NOT_ENOUGH_PRIVILEGES = 12;

    /**
     * Indicates that the user is required to be registered before performing
     * the operation. This property is initially created to take care of chat
     * room join error.
     */
    public static final int REGISTRATION_REQUIRED = 13;

    /**
     * Indicates that we are currently not joined to the chat room, over which
     * we try to perform an operation.
     */
    public static final int CHAT_ROOM_NOT_JOINED = 14;

    /**
     * Indicates that the authentication process has been canceled.
     */
    public static final int AUTHENTICATION_CANCELED = 15;

    /**
     * Indicates that the operation has been canceled by the user.
     */
    public static final int OPERATION_CANCELED = 16;

    /**
     * Indicates that the operation has been canceled because of a missing
     * server information.
     */
    public static final int SERVER_NOT_SPECIFIED = 17;

    /**
     * The operation that throws this exception is not supported.
     */
    public static final int NOT_SUPPORTED_OPERATION = 18;

    /**
     * The error code of the exception
     */
    private final int errorCode;

    /**
     * Creates an exception with the specified error message and error code.
     * @param message A message containing details on the error that caused the
     * exception
     * @param errorCode the error code of the exception (one of the error code
     * fields of this class)
     */
    public OperationFailedException(String message, int errorCode)
    {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception with the specified message, errorCode and cause.
     * @param message A message containing details on the error that caused the
     * exception
     * @param errorCode the error code of the exception (one of the error code
     * fields of this class)
     * @param cause the error that caused this exception
     */
    public OperationFailedException(String message,
                                    int errorCode,
                                    Throwable cause)
    {
        super(message, cause);

        this.errorCode = errorCode;
    }

    /**
     * Obtain the error code value.
     *
     * @return the error code for the exception.
     */
    public int getErrorCode()
    {
        return errorCode;
    }
}
