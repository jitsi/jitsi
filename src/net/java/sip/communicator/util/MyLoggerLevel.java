package net.java.sip.communicator.util;

import java.util.logging.*;

public class MyLoggerLevel extends Level
{
    public static final Level AUDIT = new MyLoggerLevel("AUDIT", Level.INFO.intValue() + 1);
    
    protected MyLoggerLevel(String name, int value)
    {
        super(name, value);
        
    }
    
}
