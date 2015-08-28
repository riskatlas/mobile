/**
 *
 */
package cz.hsrs.hsform.activity;

import cz.hsrs.hsform.R;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * @author mkepka
 *
 */
public class About extends Activity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.about);
        TextView textView = (TextView) findViewById(R.id.about);
        Resources res = getResources();
        textView.setMovementMethod (LinkMovementMethod.getInstance());
        textView.setText (Html.fromHtml(res.getString(R.string.about_text)));
    }
}
