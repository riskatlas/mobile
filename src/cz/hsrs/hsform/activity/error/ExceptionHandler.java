/**
 *
 */
package cz.hsrs.hsform.activity.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cz.hsrs.hsform.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

/**
 * Class that handles uncaught exceptions or RuntimeExceptions
 * @author mkepka
 *
 */
public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {

    private final Activity myContext;
    private final String LINE_SEPARATOR = "\n";
    private static SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private final String errorExtra;

    public ExceptionHandler(Activity context) {
        myContext = context;
        errorExtra = context.getResources().getString(R.string.errorExtra);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        StringBuilder errorReport = new StringBuilder();

        errorReport.append("************ CAUSE OF ERROR ************\n\n");
        errorReport.append(stackTrace.toString());

        errorReport.append("\n************ DEVICE INFORMATION ***********\n");
        errorReport.append("Brand: ");
        errorReport.append(Build.BRAND);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Device: ");
        errorReport.append(Build.DEVICE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Model: ");
        errorReport.append(Build.MODEL);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Id: ");
        errorReport.append(Build.ID);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Product: ");
        errorReport.append(Build.PRODUCT);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("\n************ FIRMWARE ************\n");
        errorReport.append("SDK: ");
        errorReport.append(Build.VERSION.SDK);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Release: ");
        errorReport.append(Build.VERSION.RELEASE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Incremental: ");
        errorReport.append(Build.VERSION.INCREMENTAL);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("\n************ TIMESTAMP ************\n");
        errorReport.append("Current timestamp: ");
        errorReport.append(formatterDate.format(new Date()));
        errorReport.append(LINE_SEPARATOR);


        Intent intent = new Intent(myContext, CrashActivity.class);
        intent.putExtra(errorExtra, errorReport.toString());
        myContext.startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
