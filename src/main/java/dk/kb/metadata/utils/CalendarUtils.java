package dk.kb.metadata.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Utility class for calendar issues.
 */
public final class CalendarUtils {
    /** Private constructor to prevent instantiation of utility class. */
    private CalendarUtils() {}
    
    /** A single instance of the DatatypeFactory to prevent overlap from recreating it too often.*/
    private static DatatypeFactory factory = null;
    
    /**
     * Turns a date into a XMLGregorianCalendar.
     * 
     * @param date The date.
     * @return The XMLGregorianCalendar.
     */
    public static XMLGregorianCalendar getXmlGregorianCalendar(Date date) {
        try {
        	if(factory == null) {
        		factory = DatatypeFactory.newInstance();
        	}
        	
        	GregorianCalendar gc = new GregorianCalendar();
        	gc.setTime(date);
            return factory.newXMLGregorianCalendar(gc);
        } catch (Exception e) {
            IllegalStateException res = new IllegalStateException("Could not create XML date for the date '" + date + "'.", e);
            ExceptionUtils.insertException(res);
            throw res;
        }
    }
    
    /**
     * @return The current date in the XML date format.
     */
    public static String getCurrentDate() {
        return getXmlGregorianCalendar(new Date()).toString();
    }
    
    /**
     * Retrieves the a date in the XML format, which needs to be transformed from another given format.
     * @param format The format of the given date.
     * @param dateString The given date for transform.
     * @return The given date in the XML date format.
     */
    public static String getDateTime(String format, String dateString) {
        SimpleDateFormat formater = new SimpleDateFormat(format);
        try {
            Date date = formater.parse(dateString);
            return getXmlGregorianCalendar(date).toString();
        } catch (ParseException e) {
            try {
                SimpleDateFormat formater2 = new SimpleDateFormat(format, Locale.US);
                Date date2 = formater2.parse(dateString);
                return getXmlGregorianCalendar(date2).toString();
            } catch (ParseException e2) {
                IllegalStateException res = new IllegalStateException("Can neither parse date '" + dateString + "' in format '" + format + "'. " 
                        + "Caught exceptions: " + e + " , " + e2, e2);
                ExceptionUtils.insertException(res);
                throw res;
            }
        }
    }
}
