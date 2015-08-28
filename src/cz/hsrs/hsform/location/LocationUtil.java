/**
 *
 */
package cz.hsrs.hsform.location;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import locus.api.objects.extra.Location;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import cz.hsrs.hsform.R;

/**
 * @author mkepka
 *
 */
public class LocationUtil {

    private Context hsfContext;
    private View hsfView;
    //private EditText lon;
    //private EditText lat;

    private String TAG;

    public LocationUtil(Context cont, View view){
        this.hsfContext = cont;
        this.hsfView = view;
        TAG = cont.getResources().getString(R.string.TAG);
    }
    /**
     * Method extracts coordinates from Location and fills Text fields in the form
     * @param loc Location of point from Locus
     */
    private void fillLocation(Location loc){
        EditText lon = (EditText) hsfView.findViewById(R.id.lon);
        EditText lat = (EditText) hsfView.findViewById(R.id.lat);
        lon.setText(loc.getLongitude()+"");
        lat.setText(loc.getLatitude()+"");
        setDateTime();
        Log.i(TAG,"Lon="+loc.getLongitude()+" Lat="+loc.getLatitude()+" T="+loc.getTime());
    }

    /**
     * Method fills form fields with current Date and Time Strings
     */
    public void setDateTime(){
        EditText timeField  = (EditText) hsfView.findViewById(R.id.time);
        timeField.setText(getCurrentTime());
        EditText dateField  = (EditText) hsfView.findViewById(R.id.date);
        dateField.setText(getCurrentDate());
    }

    /**
     * Method creates current time as String
     * @return String of current time
     */
    private String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
        String now = formatter.format(new Date());
        return now;
    }
    /**
     * Method creates current date as String
     * @return String of current Date
     */
    private String getCurrentDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        String d = formatter.format(new Date());
        return d;
    }
}
