/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "Logger.h"
#include <windows.h>
#include <string.h>

#define LOGGER_DATE_STRING_LENGTH 25


/**
 * Constructs new Logger object.
 * @param pLogFile the filename of the log file.
 * @param pLogPath the path of the log file.
 */
Logger::Logger(const char* pLogFile, const char* pLogPath, int pLogLevel)
{
	logLevel = pLogLevel;
	canWriteInFile = false;
	if(pLogPath != NULL && strlen(pLogPath) != 0)
	{
		logPath = (char*)malloc((strlen(pLogPath)+1)*sizeof(char));
		memcpy(logPath, pLogPath, strlen(pLogPath) + 1);
		if(pLogFile != NULL && strlen(pLogFile) != 0)
		{
//			This code enables different log files for every instance of the application.
//			char *dateString = (char*)malloc(LOGGER_DATE_STRING_LENGTH*sizeof(char));
//			getCurrentTimeString(dateString);
//			logFile = (char*)malloc((strlen(pLogPath) + strlen(pLogFile) + strlen(dateString) + 1)*sizeof(char));
//			sprintf(logFile, "%s%s%s", pLogPath, dateString, pLogFile);
//			free(dateString);
			logFile = (char*)malloc((strlen(pLogPath) + strlen(pLogFile) + 1)*sizeof(char));
			sprintf(logFile, "%s%s", pLogPath, pLogFile);
			file = fopen(logFile, "w");
			if(file != NULL)
			{
				canWriteInFile = true;
			}
		}
		
	}

	if(!canWriteInFile)
	{
		logPath = NULL;
		logFile = NULL;
		file = NULL;
	}
}

Logger::~Logger()
{
	if(logPath != NULL)
	{
		free(logPath);
	}

	if(logFile != NULL)
	{
		free(logFile);
	}

	if(canWriteInFile)
		fclose(file);
}

const char* Logger::getCurrentFile()
{
	return "";
}

/**
 * Returns current timestamp string.
 */
void Logger::getCurrentTimeString(char* dateString)
{
	SYSTEMTIME systemTime;
	GetSystemTime(&systemTime);
	sprintf(dateString,"%u-%02u-%02u-%02u-%02u-%02u.%u",
		systemTime.wYear,
		systemTime.wMonth,
		systemTime.wDay,
		systemTime.wHour,
		systemTime.wMinute,
		systemTime.wSecond,
		systemTime.wMilliseconds);
}

/**
 * Logs a message
 * @param message the message.
 */
void Logger::log(const char* message)
{
	if(canWriteInFile && logLevel >= LOGGER_LEVEL_TRACE)
	{
		char *dateString = (char*)malloc(LOGGER_DATE_STRING_LENGTH*sizeof(char));
		getCurrentTimeString(dateString);
		fprintf(file, "%s %s: %s\n",dateString, getCurrentFile(), message);
		fflush(file);
		free(dateString);
	}
}

/**
 * Logs a message
 * @param message the message.
 */
void Logger::logInfo(const char* message)
{
	if(canWriteInFile && logLevel >= LOGGER_LEVEL_INFO )
	{
		char *dateString = (char*)malloc(LOGGER_DATE_STRING_LENGTH*sizeof(char));
		getCurrentTimeString(dateString);
		fprintf(file, "%s %s: %s\n",dateString, getCurrentFile(), message);
		fflush(file);
		free(dateString);
	}
}

/**
 * Returns the path of the log file.
 */
char* Logger::getLogPath()
{
	return logPath;
}

/**
 * Returns the current log level.
 */
int Logger::getLogLevel()
{
	return logLevel;
}


