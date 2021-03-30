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
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_LOGGER_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_LOGGER_H_

#include <cstdio>
#include <Windows.h>

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
    LPTSTR logFile;

	/**
	 * The path of the file.
	 */
    LPTSTR logPath;

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
	static void getCurrentTimeString(LPTSTR);

	/**
	 * Not implemented.
	 */
    LPCTSTR getCurrentFile();

public:
	/**
	 * Constructs new Logger object.
	 * @param pLogFile the filename of the log file.
	 * @param pLogPath the path of the log file.
	 * @param pLogLevel the current log level
	 */
	Logger(LPCTSTR pLogFile, LPCTSTR pLogPath, int pLogLevel);

	/**
	 * Destructor.
	 */
	~Logger();

	/**
	 * Logs a message
	 * @param message the message.
	 */
	void log(LPCTSTR message);

	/**
	 * Logs a message
	 * @param message the message.
	 */
	void logInfo(LPCTSTR message);

	/**
	 * Returns the path of the log file.
	 */
    LPCTSTR getLogPath();

	/**
	 * Returns the current log level.
	 */
	int getLogLevel() const;
};

#endif
