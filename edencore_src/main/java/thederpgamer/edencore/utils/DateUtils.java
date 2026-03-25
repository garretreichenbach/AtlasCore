package thederpgamer.edencore.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Contains misc date and time utilities.
 *
 * @author TheDerpGamer
 * @since 06/27/2021
 */
public class DateUtils {
	public static float getAgeDays(long time) {
		return getAgeDays(new Date(time));
	}

	public static float getAgeDays(Date date) {
		Date current = new Date(System.currentTimeMillis());
		long difference = Math.abs(current.getTime() - date.getTime());
		return ((float) difference / (1000 * 60 * 60 * 24));
	}
	
	public static String getTimeFormatted(long time) {
		return getTimeFormatted(time, "MM/dd/yyyy '-' hh:mm:ss z");
	}
	
	public static String getTimeFormatted(long time, String format) {
		return new SimpleDateFormat(format).format(new Date(time));
	}

	public static String getCurrentTimeFormatted() {
		return getCurrentTimeFormatted("MM/dd/yyyy '-' hh:mm:ss z");
	}

	public static String getCurrentTimeFormatted(String format) {
		return new SimpleDateFormat(format).format(new Date(System.currentTimeMillis()));
	}
}
