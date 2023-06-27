package glorydark.wardensystem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggerFormatter extends Formatter {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public synchronized String format(LogRecord record) {
        return format.format(new Date(record.getMillis())) + " - " +
                "[" + record.getLevel() + "] - " +
                formatMessage(record) + "\n";
    }
}
