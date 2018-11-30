package test.mike.com.mediaframeworktools.ExifUtils;

import android.media.ExifInterface;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class ExifUtility {

    private static final Pattern sNonZeroTimePattern = Pattern.compile(".*[1-9].*");

    public static long getGpsDateTime(ExifInterface exif) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        String date = exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
        String time = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
        if (date == null || time == null
                || (!sNonZeroTimePattern.matcher(date).matches()
                && !sNonZeroTimePattern.matcher(time).matches())) {
            return -1;
        }

        String dateTimeString = date + ' ' + time;

        ParsePosition pos = new ParsePosition(0);
        try {
            Date datetime = formatter.parse(dateTimeString, pos);
            if (datetime == null) return -1;
            return datetime.getTime();
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    public static long getDateTime(ExifInterface exif) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        String dateTimeString = exif.getAttribute(ExifInterface.TAG_DATETIME);
        if (dateTimeString == null
                || !sNonZeroTimePattern.matcher(dateTimeString).matches()) return -1;

        ParsePosition pos = new ParsePosition(0);
        try {
            // The exif field is in local time. Parsing it as if it is UTC will yield time
            // since 1/1/1970 local time
            Date datetime = formatter.parse(dateTimeString, pos);
            if (datetime == null) return -1;
            long msecs = datetime.getTime();

            String subSecs = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME);
            if (subSecs != null) {
                try {
                    long sub = Long.parseLong(subSecs);
                    while (sub > 1000) {
                        sub /= 10;
                    }
                    msecs += sub;
                } catch (NumberFormatException e) {
                    // Ignored
                }
            }
            return msecs;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }
}
