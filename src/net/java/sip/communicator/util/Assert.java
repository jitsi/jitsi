/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */package net.java.sip.communicator.util;

/**
 * @author Alexander Pelov
 */
public class Assert {
	/**
	 * The logger for this class.
	 */
	private static final Logger log = Logger.getLogger(Assert.class.getName());

	/**
	 * Enables or disables assertion checks.
	 */
	public static boolean EnableAssertions = true;

	/**
	 * Throws an exception if the passed condition is false and
	 * <code>EnableAssertions == true</code>.
	 * 
	 * @param condition The condition to be met.
	 * @param message The message to be displayed as explanation on assertion fail.
	 * @throws IllegalArgumentException Thrown if condition is false and 
	 * 			assertions are enabled.
	 */
	public static final void assertTrue(boolean condition, String message) throws AssertionError {
		if (EnableAssertions && !condition) {
            StackTraceElement caller = new Throwable().getStackTrace()[1];

			String fullText = "Assertion failed at " + caller.getMethodName() + ", line " + 
				caller.getLineNumber() + 
				"\n\tMessage: " + message;
			
			log.error(fullText);
			
			throw new AssertionError(fullText);
		}
	}

	/**
	 * Throws an exception if <code>EnableAssertions == true</code>.
	 * 
	 * @param message The message to be displayed as explanation on assertion fail.
	 * @throws IllegalArgumentException Thrown if assertions are enabled.
	 */
	public static final void fail(String message) throws AssertionError {
		Assert.assertTrue(false, message);
	}
	
	/**
	 * Throws an exception if the passed object is null and
	 * <code>EnableAssertions == true</code>.
	 * 
	 * @param obj The object to be checked.
	 * @param message The message to be displayed as explanation on assertion fail.
	 * @throws IllegalArgumentException Thrown if obj is null 
	 * 			and assertions are enabled.
	 */
	public static final void assertNonNull(Object obj, String message) throws AssertionError {
		Assert.assertTrue(obj != null, message);
	}

	/**
	 * Throws an exception if the passed object is not null and
	 * <code>EnableAssertions == true</code>.
	 * 
	 * @param obj The object to be checked.
	 * @param message The message to be displayed as explanation on assertion fail.
	 * @throws IllegalArgumentException Thrown if obj is not null and 
	 * 			assertions are enabled.
	 */
	public static final void assertNull(Object obj, String message) throws AssertionError {
		Assert.assertTrue(obj == null, message);
	}
}
