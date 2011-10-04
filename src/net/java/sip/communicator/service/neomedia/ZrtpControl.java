package net.java.sip.communicator.service.neomedia;

public interface ZrtpControl
    extends SrtpControl
{
    /**
     * Return the zrtp hello hash String.
     *
     * @return String the zrtp hello hash.
     */
    public String getHelloHash();

    /**
     * Get the ZRTP Hello Hash data - separate strings.
     *
     * @return String array containing the version string at offset 0, the Hello
     *         hash value as hex-digits at offset 1. Hello hash is available
     *         immediately after class instantiation. Returns <code>null</code>
     *         if ZRTP is not available.
     */
    public String[] getHelloHashSep();
}
