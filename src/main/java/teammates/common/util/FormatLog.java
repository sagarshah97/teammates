package teammates.common.util;

import java.util.HashMap;
import java.util.Map;

import teammates.common.datatransfer.logs.LogSeverity;
import teammates.common.datatransfer.logs.SourceLocation;

/**
 * Formats the log messages for the desired audience.
 */
public class FormatLog {
    /**
     * Identifies in which format the log message needs to be modified.
     * @param message is the actual message
     * @param severity defines the severity of the log message
     * @param loggerSource provides the information about the source/origination of the log
     * @return formatted log message
     */
    public String formatLogMessage(String message, LogSeverity severity, StackTraceElement loggerSource) {
        if (Config.IS_DEV_SERVER) {
            return formatLogMessageForHumanDisplay(message, loggerSource);
        }
        return formatLogMessageForCloudLogging(message, severity, loggerSource);
    }

    /**
     * Formats the log message for displaying it to humans/users.
     * @param message is the actual message
     * @param loggerSource provides the information about the source/origination of the log
     * @return formatted log message
     */
    public String formatLogMessageForHumanDisplay(String message, StackTraceElement loggerSource) {
        StringBuilder prefix = new StringBuilder();

        StackTraceElement source = loggerSource;
        if (source != null) {
            prefix.append(source.getClassName()).append(':')
                    .append(source.getMethodName()).append(':')
                    .append(source.getLineNumber()).append(':');
        }
        prefix.append(' ');

        if (RequestTracer.getTraceId() == null) {
            return prefix.toString() + message;
        }
        return prefix.toString() + "[" + RequestTracer.getTraceId() + "] " + message;
    }

    /**
     * Formats the log message for logging it into the cloud base.
     * @param message is the actual message
     * @param severity defines the severity of the log message
     * @param loggerSource provides the information about the source/origination of the log
     * @return formatted log message
     */
    public String formatLogMessageForCloudLogging(String message, LogSeverity severity,
            StackTraceElement loggerSource) {
        return JsonUtils.toCompactJson(getFormattedBaseCloudLoggingPayload(message, severity, loggerSource));
    }

    /**
     * Get the payload to be stored in cloud base.
     * @param message is the actual message
     * @param severity defines the severity of the log message
     * @param loggerSource provides the information about the source of the log
     * @return key-value pairs for storing the relevant information on the cloud
     */
    public Map<String, Object> getFormattedBaseCloudLoggingPayload(String message, LogSeverity severity,
            StackTraceElement loggerSource) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("severity", severity);

        StackTraceElement source = loggerSource;
        if (source != null) {
            SourceLocation sourceLocation = new SourceLocation(
                    source.getClassName(), (long) source.getLineNumber(), source.getMethodName());
            payload.put("logging.googleapis.com/sourceLocation", sourceLocation);
        }

        if (RequestTracer.getTraceId() != null) {
            payload.put("logging.googleapis.com/trace",
                    "projects/" + Config.APP_ID + "/traces/" + RequestTracer.getTraceId());
        }

        if (RequestTracer.getSpanId() != null) {
            payload.put("logging.googleapis.com/spanId", RequestTracer.getSpanId());
        }

        return payload;
    }
}
