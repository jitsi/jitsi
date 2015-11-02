package net.java.sip.communicator.util;

import java.util.logging.*;

public class AuditLevel extends Level
{
    public static final Level AUDIT = new AuditLevel("AUDIT", Level.INFO.intValue() + 1);

    protected AuditLevel(String name, int value)
    {
        super(name, value);

    }

}
