/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

/**
 * Exception class managing the basics dict server errors.
 * 
 * @author LITZELMANN Cedric
 * @author ROTH Damien
 */
public class DictException
    extends Exception
{
    // Error number
    private int error;
    
    // Error message
    private String errorMessage;
    
    /**
     * Create an exception from the dict error code
     * @param error Error code returned by the server
     */
    public DictException(int error)
    {
        this.error = error;
    }
    
    /**
     * Create an exception with a custom message
     * @param error Error code returned by the server
     * @param message Custom message
     */
    public DictException(int error, String message)
    {
        this.error = error;
        this.errorMessage = message;
    }
    
    /**
     * Same as the first constructor but with a string (converted to an int)
     * @param error Error code returned by the server
     */
    public DictException(String error)
    {
        this.error = Integer.parseInt(error);
    }
    
    /**
     * Create an exception from a java exception
     * @param e Java Exception
     */
    public DictException(Exception e)
    {
        String className = e.getClass().toString();
        
        if (className.endsWith("IOException"))
        {
            this.error = 901;
            this.errorMessage = "IOException";
        }
        else if (className.endsWith("UnknownHostException"))
        {
            this.error = 902;
            this.errorMessage = "UnknownHostException";
        }
        else if (className.endsWith("SecurityException"))
        {
            this.error = 903;
            this.errorMessage = "SecurityException";
        }
        else if (className.endsWith("SocketTimeoutException"))
        {
            this.error = 904;
            this.errorMessage = "SocketTimeoutException";
        }
        else
        {
            this.error = 900;
            this.errorMessage = "Unknown error [" + className + "]";
        }
        
        this.errorMessage += ": " + e.getMessage();
    }

    /**
     * Return the error code
     * @return the error code
     */
    public int getErrorCode()
    {
        return this.error;
    }
    
    /**
     * Return the error message
     * @return the error message
     */
    public String getMessage()
    {
        return this.errorMessage;
    }
    
    /**
     * Get an explanation of the error
     * @return  Returns an explanation corresponding to the current error
     * (this.error).
     */
    public String getErrorMessage()
    {
        String result;
        switch(this.error)
        {
        case 110 :
            result = "n databases present";
            break;
        case 111 :
            result = "n strategies available";
            break;
        case 112 :
            result = "database information follows";
            break;
        case 113 :
            result = "help text follows";
            break;
        case 114 :
            result = "server information follows";
            break;
        case 130 :
            result = "challenge follows";
            break;
        case 150 :
            result = "n definitions retrieved";
            break;
        case 151 :
            result = "word database name";
            break;
        case 152 :
            result = "n matches found";
            break;
        case 210 :
            result = "optional timing";
            break;
        case 220 :
            result = "Connection OK";
            break;
        case 221 :
            result = "Closing Connection";
            break;
        case 230 :
            result = "Authentication successful";
            break;
        case 250 :
            result = "OK";
            break;
        case 330 :
            result = "send response";
            break;
        case 420 :
            result = "Server temporarily unavailable";
            break;
        case 421 :
            result = "Server shutting down at operator request";
            break;
        case 500 :
            result = "Syntax error, command not recognized";
            break;
        case 501 :
            result = "Syntax error, illegal parameters";
            break;
        case 502 :
            result = "Command not implemented";
            break;
        case 503 :
            result = "Command parameter not implemented";
            break;
        case 530 :
            result = "Access denied";
            break;
        case 531 :
            result = "Access denied, use SHOW INFO for server information";
            break;
        case 532 :
            result = "Access denied, unknown mechanism";
            break;
        case 550 :
            result = "Invalid database, use SHOW DB for list of databases";
            break;
        case 551 :
            result = "Invalid strategy, use SHOW STRAT for a list of strategies";
            break;
        case 552 :
            result = "No match";
            break;
        case 554 :
            result = "No databases present";
            break;
        case 555 :
            result = "No strategies available";
            break;
        default  :
            if (error >= 900)
            {
                result = this.errorMessage;
            }
            else
            {
                result = this.errorGen(error);
            }
        }
        
        return result;
    }
    
    /**
     * Get informations about unknowns errors
     * @param err Error number
     * @return Error definition
     */
    private String errorGen(int err)
    {
        String error_type = Integer.toString(err);
        
        String result = new String();
        
        if(error_type.startsWith("1"))
        {// test on digit one
            result = "Positive Preliminary reply : ";
        }
        else if(error_type.startsWith("2"))
        {
            result = "Positive Completion reply : " ;
        }
        else if(error_type.startsWith("3"))
        {
            result =  "Positive Intermediate reply : ";
        }
        else if(error_type.startsWith("4"))
        {
            result = "Transient Negative Completion reply : ";
        }
        else if(error_type.startsWith("5"))
        {
            result = "Permanent Negative Completion reply : ";
        }
        else
        {
            return "Unknown error";
        }
        
        //test on digit two
        if(error_type.charAt(1) == '0')
        {
            result += "Syntax";
        }
        else if ( error_type.charAt(1) == '1')
        {
            result += "Information";
        }
        else if ( error_type.charAt(1) == '2')
        {
            result += "Connections";
        }
        else if ( error_type.charAt(1) == '3')
        {
            result += "Authentication";
        }
        else if ( error_type.charAt(1) == '4')
        {
            result += "Unspecified as yet";
        }
        else if ( error_type.charAt(1) == '5')
        {
            result += "DICT System";
        }
        else if ( error_type.charAt(1) == '8')
        {
            result += "Nonstandard (private implementation) extensions";
        }
        
        return result;
    }
}
