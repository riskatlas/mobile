/**
 *
 */
package cz.hsrs.hsform.activity;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author mkepka
 *
 */
public class SettingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
