package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

/**
 * Created by gp on 6/3/15.
 */
public enum SimulcastMode
{
    REWRITING("REWRITING"),
    SWITCHING("SWITCHING");

    private String text;

    SimulcastMode(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public static SimulcastMode fromString(String text) {
        if (text != null) {
            for (SimulcastMode b : SimulcastMode.values()) {
                if (text.equalsIgnoreCase(b.text)) {
                    return b;
                }
            }
        }
        return null;
    }
}
