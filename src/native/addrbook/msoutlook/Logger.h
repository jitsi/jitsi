/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_LOGGER_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_LOGGER_H_

#include <stdio.h>

#define LOGGER_LEVEL_INFO 0
#define LOGGER_LEVEL_TRACE 1

/**
 * Utility class for logging messages in file.
 */
class Logger
{
	/**
	 * The full file name of the log file
	 */
	char* logFile;

	/**
	 * The path of the file.
	 */
	char* logPath;

	/**
	 * The file handle.
	 */
	FILE* file;

	/**
	 * Indicates whether the log file is successfully opened or not.
	 */
	bool canWriteInFile;

	/**
	 * Current log level.
	 **/
	int logLevel;

	/**
	 * Returns current timestamp string.
	 */
	void getCurrentTimeString(char*);

	/**
	 * Not implemented.
	 */
	const char* getCurrentFile();

public:
	/**
	 * Constructs new Logger object.
	 * @param pLogFile the filename of the log file.
	 * @param pLogPath the path of the log file.
	 * @param pLogLevel the current log level
	 */
	Logger(const char* pLogFile, const char* pLogPath, int pLogLevel);

	/**
	 * Destructor.
	 */
	~Logger();

	/**
	 * Logs a message
	 * @param message the message.
	 */
	void log(const char* message);

	/**
	 * Logs a message
	 * @param message the message.
	 */
	void logInfo(const char* message);

	/**
	 * Returns the path of the log file.
	 */
	char* getLogPath();

	/**
	 * Returns the current log level.
	 */
	int getLogLevel();
};

#endif
