package net.java.sip.communicator.impl.protocol.irc.exception;


public class BadCommandInvocationException
    extends Exception
{
    private final String line;
    private final String help;

    public BadCommandInvocationException(final String line, final String help, final Throwable cause)
    {
        super("The command failed because of incorrect usage: "
            + cause.getMessage(), cause);
        this.line = line;
        this.help = help;
    }

    public String getLine()
    {
        return this.line;
    }

    public String getHelp()
    {
        return this.help;
    }
}
