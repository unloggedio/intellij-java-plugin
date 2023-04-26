package com.insidious.plugin.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    public static String formatDate(Date date) {
        return formatter.format(date);
    }


}
