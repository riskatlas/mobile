/**
 *
 */
package cz.hsrs.hsform.activity.error;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import cz.hsrs.hsform.R;

/**
 * Activity that prints stack of exceptions with device information
 * @author mkepka
 *
 */
public class CrashActivity extends Activity {

    TextView errorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        setContentView(R.layout.crash_activity_main);

        errorView = (TextView) findViewById(R.id.error);
        String errorExtra = this.getResources().getString(R.string.errorExtra);
        if(errorExtra != null){
            if(!errorExtra.isEmpty()){
                errorView.setText(getIntent().getStringExtra(errorExtra));
            }
        }
        else{
            errorView.setText(getIntent().getStringExtra("cz.hsrs.hsform.error"));
        }
    }
}
