package dk.kb.yggdrasil.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;

/**
 * Utility class for dealing with time/date.
 */
public class TimeUtils {
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(TimeUtils.class);

    /** The format for the timeout date. */
    private static final String[] DEFAULT_TIMEOUT_DATE_FORMAT = {"EEE MMM dd HH:mm:ss zzz yyyy"};
    
    /**
     * Parses a date in our specified format. First attempts to use the system default locale, if that
     * does not work, then it will attempt to parse it with the english locale.
     * @param date The date in text format.
     * @return The Date object for the date, or null if it could not be parsed.
     */
    public static Date parseDate(String date) {
        ArgumentCheck.checkNotNullOrEmpty(date, "String date");
        
        for(String dateFormat : DEFAULT_TIMEOUT_DATE_FORMAT) {
            try {
                DateFormat formatter = new SimpleDateFormat(dateFormat);
                Date d = formatter.parse(date);
                return d;
            } catch (ParseException e) {
                logger.warn("Could not parse the timeout date, '" + date + "' with dateformat '" + dateFormat 
                        + "' and default locale", e);
            }
            try {
                DateFormat formatter = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
                Date d = formatter.parse(date);
                return d;
            } catch (ParseException e) {
                logger.warn("Could not parse the timeout date, '" + date + "' with dateformat '" + dateFormat 
                        + "' and locale English", e);
            }
        }
        return null;
    }
}
