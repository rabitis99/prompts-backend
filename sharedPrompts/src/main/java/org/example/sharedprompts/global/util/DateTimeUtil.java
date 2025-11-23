package org.example.sharedprompts.global.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
    private DateTimeUtil() {}

    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.ISO_DATE);
    }
}
