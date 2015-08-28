package cz.hsrs.hsform.activity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import locus.api.android.utils.LocusUtils;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Waypoint;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import cz.hsrs.hsform.R;
import cz.hsrs.hsform.activity.error.ExceptionHandler;
import cz.hsrs.hsform.model.HsFormObject;
import cz.hsrs.hsform.picture.BaseAlbumDirFactory;
import cz.hsrs.hsform.picture.FroyoAlbumDirFactory;
import cz.hsrs.hsform.picture.PictureUtil;

public class HsFormActivity extends Activity {
    public String TAG;
    private static SimpleDateFormat formatterTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.ROOT);
    private static SimpleDateFormat formatterTimezone = new SimpleDateFormat("ZZZ", Locale.ROOT);
    private static SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
    private static SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private LocationManager locationManager;
    private LocationListener locationListener;
    public android.location.Location currAndLoc;

    private static final int REQUEST_TAKE_PHOTO = 1;
    private PictureUtil picUtil;
    private ImageView mImageView;

    private File photoFile = null;
    private SSLRequestTask sslTransfer;
    private RequestTask transfer;

    protected final String SUCCESS_RESPONSE = "true";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        TAG = getResources().getString(R.string.TAG);

        Log.i(TAG, "OnCreate method"); // debug log
        currAndLoc = null;
        setContentView(R.layout.main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mImageView = (ImageView) findViewById(R.id.imageView);

        // Creates album storage depending on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            picUtil = new PictureUtil(new FroyoAlbumDirFactory(), this);
        } else {
            picUtil = new PictureUtil(new BaseAlbumDirFactory(), this);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        // catching of location
        this.currAndLoc = null;
        try{
            if(locationManager==null){
                locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            }
            if(locationListener==null){
                locationListener = (LocationListener) new MyLocationListener();
            }
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 25, locationListener);
            }
            else if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 50, locationListener);
            }
        } catch(Exception ex){
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_location), Toast.LENGTH_SHORT ).show();
            Log.e(TAG, "GetCoordinates: ", ex);
        }
        // load coordinates from Locus
        Intent intent = getIntent();
        if(intent == null){
            Log.w(TAG, "Intent was null!");
             return;
        }
        else{
            Log.i(TAG, "Intent action = "+intent.getAction()); // debug log
            // HsForm opened from new point menu
             if(LocusUtils.isIntentPointTools(intent)){
                try {
                    Waypoint p = LocusUtils.handleIntentPointTools(this, intent);
                    fillLocation(p.getLocation());
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            // HsForm opened from right function panel
            else if(LocusUtils.isIntentMainFunction(intent)){
                LocusUtils.handleIntentMainFunction(intent, new LocusUtils.OnIntentMainFunction() {
                    public void onReceived(Location locGps, Location locMapCenter) {
                        // GPS location
                        if(locGps != null){
                            fillLocation(locGps);
                        }
                        // center of the map
                        else if(locMapCenter != null){
                            fillLocation(locMapCenter);
                        }
                        else{
                            Log.w(TAG, "Location was not returned!");
                        }
                    }
                    public void onFailed() {
                        Log.w(TAG, "Location was not selected");
                    }
                });
            }
            else{
                if(intent.equals(Intent.ACTION_MAIN)){
                    Log.i(TAG, "Intent MAIN!"); // debug log
                }
            }
        }
    }

    @Override
    public void onStop () {
        super.onStop();
        locationManager.removeUpdates(locationListener);
    }

    /**
     * Creates menu and fills parts of menu from xml file
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.hsform_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Switch actions depending on selected part of menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_about:
                openAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method starts new activity to set preferences
     */
    private void openSettings() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    /**
     * Method starts new activity with application information
     */
    private void openAbout() {
        Intent intent = new Intent(this, About.class);
        startActivity(intent);
    }

    /**
     * Method fills form field start time of event by current timestamp
     * @param view View with given form fields
     */
    public void getCurrentStartTime(View view){
        Date current = new Date();

        EditText startTimeField  = (EditText) findViewById(R.id.starttime);
        String now = formatterTime.format(current);
        startTimeField.setText(now);

        EditText startDateField  = (EditText) findViewById(R.id.startdate);
        String today = formatterDate.format(current);
        startDateField.setText(today);

        Log.i(TAG, "Start timestamp= "+today+" "+now); // debug log
        Log.i(TAG, "Timezone = "+ formatterTimezone.format(current)); // debug log
    }

    /**
     * Method fills form field time of event by current timestamp
     * @param view View with given form fields
     */
    public void getCurrentTime(View view){
        Date current = new Date();

        EditText startTimeField  = (EditText) findViewById(R.id.time);
        String now = formatterTime.format(current);
        startTimeField.setText(now);

        EditText startDateField  = (EditText) findViewById(R.id.date);
        String today = formatterDate.format(current);
        startDateField.setText(today);
    }

    /**
     * Method clears form fields of Date and Time and fills with current values
     * @param view View with given form fields
     */
    public void clearForm(View view){
        setDateTime();
    }

    /**
     * Method fills form fields with current Date and Time Strings
     */
    public void setDateTime(){
        Date current = new Date();

        EditText timeField  = (EditText) findViewById(R.id.time);
        timeField.setText(formatterTime.format(current));

        EditText dateField  = (EditText) findViewById(R.id.date);
        dateField.setText(formatterDate.format(current));
    }

    /**
     * Method catches onClick event on "GPS Button"
     * and loads last location from Android Location provider
     * @param view Current view of activity
     */
    public void getCoordinates(View view){
        if(this.currAndLoc==null){
            this.currAndLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if(this.currAndLoc!=null){
            fillLocation(LocusUtils.convertToL(currAndLoc));
        }
        else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_location), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method extracts coordinates from Location and fills Text fields in the form
     * @param loc Locus Location of point
     */
    private void fillLocation(Location loc){
        EditText lon = (EditText) findViewById(R.id.lon);
        EditText lat = (EditText) findViewById(R.id.lat);
        lon.setText(loc.getLongitude()+"");
        lat.setText(loc.getLatitude()+"");
        setDateTime();
        formatterTimestamp.format(new Date(loc.getTime()));
        Log.i(TAG,"Location Lon="+loc.getLongitude()+" Lat="+loc.getLatitude()+" T="+formatterTimestamp.format(new Date(loc.getTime()))+" Provider="+loc.getProvider()); //debug log
    }

    /**
     * Method catches onClick event on "Camera Button"
     * and runs new Activity to take picture
     * @param view Current view of Activity
     */
    public void takePicture(View view){
        startActivityForResult(picUtil.prepareIntentPicture(), REQUEST_TAKE_PHOTO);
    }

    /**
     * Method catch Camera activity and process taken photo to form
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                photoFile = picUtil.processResult(mImageView);
            }
        }
    }

    /**
     * Method process form if user clicks on submit button
     * @param view Current view with form
     */
    public void onSubmitClick(View view){
        EditText title = (EditText) findViewById(R.id.title);
        EditText description = (EditText) findViewById(R.id.description);
        EditText lon = (EditText) findViewById(R.id.lon);
        EditText lat = (EditText) findViewById(R.id.lat);
        Spinner category = (Spinner) findViewById(R.id.category);
        Spinner status = (Spinner) findViewById(R.id.status);
        EditText sDate = (EditText) findViewById(R.id.startdate);
        EditText sTime = (EditText) findViewById(R.id.starttime);
        EditText fDate = (EditText) findViewById(R.id.date);
        EditText fTime = (EditText) findViewById(R.id.time);

        int wrongParams = 0;
        String missParams = "";

        String titleValue = title.getText().toString();
        if(titleValue == null){
            wrongParams++;
            missParams = missParams+getResources().getString(R.string.title)+" ";
        }
        else if(titleValue.isEmpty()){
            wrongParams++;
            missParams = missParams+getResources().getString(R.string.title)+" ";
        }
        String lonValue = lon.getText().toString();
        String latValue = lat.getText().toString();
        if(lonValue == null || latValue == null){
            wrongParams++;
            missParams = missParams+getResources().getString(R.string.lonlat)+" ";
        }
        else if(lonValue.isEmpty() || latValue.isEmpty()){
            wrongParams++;
            missParams = missParams+getResources().getString(R.string.lonlat)+" ";
        }

        String timeZone = formatterTimezone.format(new Date());
        String serviceURL = PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.pref_url_key).trim(), null);

        String sDateValue = sDate.getText().toString();
        String sTimeValue = sTime.getText().toString();
        String beginTime = "";
        if(sDateValue != null && sTimeValue != null){
            if(!sDateValue.isEmpty() && !sTimeValue.isEmpty()){
                beginTime = sDateValue+"T"+sTimeValue+timeZone;
            }
        }

        String fDateValue = fDate.getText().toString();
        String fTimeValue = fTime.getText().toString();
        String sysTime = "";
        if(fDateValue == null || fTimeValue == null){
            wrongParams++;
            missParams = missParams+getResources().getString(R.string.datetime)+" ";
        }
        else if(fDateValue.isEmpty() || fTimeValue.isEmpty()){
            wrongParams++;
            missParams = missParams+getResources().getString(R.string.datetime)+" ";
        }
        else{
            sysTime = fDateValue+"T"+fTimeValue+timeZone;
        }

        if(wrongParams == 0){
            HsFormObject form = new HsFormObject(
                    titleValue,
                    description.getText().toString(),
                    String.valueOf(category.getSelectedItem()),
                    String.valueOf(status.getSelectedItem()),
                    lonValue,
                    latValue,
                    beginTime,
                    sysTime,
                    PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.pref_url_key).trim(), null),
                    photoFile);
            try {
                if(serviceURL.startsWith("https://")){
                    sslTransfer = new SSLRequestTask(this);
                    sslTransfer.execute(form);
                }
                else if(serviceURL.startsWith("http://")){
                    transfer = new RequestTask(this);
                    transfer.execute(form);
                }
                else{
                    Toast.makeText(getApplicationContext(), "Wrong URL, please check!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Sending to server failed!", Toast.LENGTH_LONG).show();
            }
        }
        else{
            if(wrongParams == 1){
                Toast.makeText(getApplicationContext(), "Mandatory parameter: "+missParams+"is missing, please check!", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Mandatory parameters: "+missParams+"are missing, please check!", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Class for listening to Location change in Android Location providers
     * @author mkepka
     *
     */
    class MyLocationListener implements LocationListener  {

        @Override
        public void onLocationChanged(android.location.Location loc) {
            currAndLoc = loc;
            Toast.makeText( getApplicationContext(), "New Location - OK", Toast.LENGTH_SHORT ).show(); // debug log?
        }

        @Override
        public void onProviderDisabled(String provider)    {
            Toast.makeText( getApplicationContext(), provider + " Disabled", Toast.LENGTH_SHORT ).show();
        }

        @Override
        public void onProviderEnabled(String provider){
            Toast.makeText( getApplicationContext(), provider + " Enabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        }
    }

    /**
     * Class that process submitting of form values to server asynchronously by HTTP
     * @author mkepka
     *
     */
    class RequestTask extends AsyncTask<HsFormObject, String, String>{
        private ProgressDialog dialog = null;
        protected HttpPost postRequest;
        private Context hsfContext;
        private SharedPreferences sharedPrefs;
        private HsFormObject form;
        private boolean ABORTED = false;
        private HttpClient httpclient;
        private Resources res;

        /**
         * Constructor creates class for sending form to server, loads preferences and prepare ProcessDialog
         * @param context
         */
        public RequestTask(Context context){
            this.hsfContext = context;
            this.res = getResources();
            this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            prepareDialog(context);
            this.httpclient = new DefaultHttpClient();
        }

        private void prepareDialog(Context context){
            dialog = new ProgressDialog(hsfContext);
            dialog.setTitle("Sending to server");
            dialog.setMessage("Please wait...");
            dialog.setCancelable(false);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(postRequest != null){
                        ABORTED = true;
                        postRequest.abort();
                    }
                }
            });
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @SuppressLint("DefaultLocale")
        @Override
        protected String doInBackground(HsFormObject... forms) {
            form = forms[0];
            Log.i(TAG, form.toString()); // debug log

            HttpResponse response;
            String responseString = null;

            // load login details
            String userName = sharedPrefs.getString(res.getString(R.string.pref_user_key), null);
            String userPass = sharedPrefs.getString(res.getString(R.string.pref_pass_key), null);
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(userName, userPass);

            try {
                String serviceURL = form.getServiceUrl();
                if(serviceURL != null){
                    if(!serviceURL.isEmpty() && serviceURL.toLowerCase().startsWith("http://")){
                        postRequest = new HttpPost(sharedPrefs.getString(res.getString(R.string.pref_url_key), null));
                        postRequest.addHeader(BasicScheme.authenticate(creds, "utf-8", false));
                        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                        builder.setCharset(Charset.forName("utf-8"));

                        // Fill HTTP entity
                        builder.addPart(res.getString(R.string.titlePartName), new StringBody(form.getTitle(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.descriptionPartName), new StringBody(form.getDescription(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.categoryPartName), new StringBody(form.getCategory(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.statusPartName), new StringBody(form.getStatus(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.lonPartName), new StringBody(form.getLongitude(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.latPartName), new StringBody(form.getLattitude(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.timestampPartName), new StringBody(form.getTimestamp(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.startTimestampPartName), new StringBody(form.getBeginTimestamp(), ContentType.TEXT_PLAIN.withCharset("utf-8")));

                        File picture = form.getPicture();
                        if(picture != null){
                            if (picture.exists() && picture.isFile()){
                                builder.addPart(res.getString(R.string.pictureSizePartName), new StringBody(String.valueOf(picture.length()), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                                builder.addPart(res.getString(R.string.pictureRotationAng), new StringBody(String.valueOf(PictureUtil.getCameraPhotoOrientation(picture.getAbsolutePath())), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                                builder.addBinaryBody(res.getString(R.string.picturePartName), picture, ContentType.create("image/jpeg", "utf-8"), picture.getName());
                            }
                        }

                        final HttpEntity httpEntity = builder.build();
                        postRequest.setEntity(httpEntity);
                        if(isCancelled()){
                            return null;
                        }
                        response = httpclient.execute(postRequest);
                        StatusLine statusLine = response.getStatusLine();
                        Log.i(TAG, "Status line:"+statusLine.toString()); // debug log
                        if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            response.getEntity().writeTo(out);
                            out.close();
                            responseString = out.toString();
                        } else{
                            //Closes the connection.
                            response.getEntity().getContent().close();
                            throw new IOException(statusLine.getReasonPhrase());
                        }
                    }
                    else{
                        return "Wrong service URL, please check it!";
                    }
                }
                else{
                    return "Wrong service URL, please check it!";
                }
            } catch (ClientProtocolException e) {
                Log.e(TAG,"Exception: "+ e.getMessage()); // debug log
                cancel(false);
                return e.getMessage();
            } catch (IOException e) {
                Log.e(TAG,"Exception: "+ e.getMessage()); // debug log
                cancel(false);
                return e.getMessage();
            }
            return responseString;
        }

        /**
         *
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if(result != null){
                Log.i(TAG, "Result="+result); // debug log
                if(result.trim().equals(SUCCESS_RESPONSE)){
                    Toast.makeText(getApplicationContext(), "Saved OK", Toast.LENGTH_SHORT).show();
                    //delete image file
                    if(form.getPicture() != null && form.getPicture().exists()){
                        boolean deleted = form.getPicture().delete();
                        Log.i(TAG, "Image deleted? = "+deleted);// debug log
                    }
                    finish();
                }
                else {
                    Toast.makeText(getApplicationContext(), "SAVE ERROR!", Toast.LENGTH_LONG).show();
                }
            }
            else{
                Toast.makeText(getApplicationContext(), "Save failed!", Toast.LENGTH_LONG).show();
            }
        }

        /**
         *
         */
        @Override
        protected void onCancelled(String result){
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if(ABORTED == true){
                Toast.makeText(getApplicationContext(), "Sending canceled!", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Save failed: "+result+"!", Toast.LENGTH_LONG).show();
                Log.i(TAG, "status final = "+getStatus().name()); // debug log
            }
        }
    }

    /**
     * Class that process submitting of form values to server asynchronously by HTTPS
     * @author mkepka
     *
     */
    class SSLRequestTask extends AsyncTask<HsFormObject, String, String>{
        private ProgressDialog dialog = null;
        protected HttpPost postRequest;
        private Context hsfContext;
        private SharedPreferences sharedPrefs;
        private HsFormObject form;
        private boolean ABORTED = false;
        private HttpClient httpclient;
        private Resources res;

        /**
         * Constructor creates class for sending form to server, loads preferences and prepare ProcessDialog
         * @param context
         * @throws Exception
         */
        public SSLRequestTask(Context context) throws Exception{
            this.hsfContext = context;
            this.res = getResources();
            this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            prepareDialog(context);
            this.httpclient = new DefaultHttpClient();
            // load certificate
            KeyStore trustStore = null;
            InputStream instream = null;
            SSLSocketFactory socketFactory;
            try {
                trustStore = KeyStore.getInstance("BKS");
                instream = hsfContext.getResources().openRawResource(R.raw.hsformstore);
                String certPass = sharedPrefs.getString(res.getString(R.string.pref_certpass_key), null);
                if(certPass != null && instream != null){
                    if(!certPass.isEmpty()){
                        trustStore.load(instream, certPass.trim().toCharArray()); //hsform
                        socketFactory = new SSLSocketFactory(trustStore);
                        Scheme sch = new Scheme("https", socketFactory, 443);
                        this.httpclient.getConnectionManager().getSchemeRegistry().register(sch);
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "Truststore password is missing! Please set it!", Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Truststore password is missing! Please set it!", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Log.e(TAG,"Exception: "+ e.getMessage());
                throw new Exception(e.getMessage());
            } catch (KeyStoreException e) {
                Log.e(TAG,"Exception: "+ e.getMessage());
                throw new Exception(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG,"Exception: "+ e.getMessage());
                throw new Exception(e.getMessage());
            } catch (CertificateException e) {
                Log.e(TAG,"Exception: "+ e.getMessage());
                throw new Exception(e.getMessage());
            } catch (KeyManagementException e) {
                Log.e(TAG,"Exception: "+ e.getMessage());
                throw new Exception(e.getMessage());
            } catch (UnrecoverableKeyException e) {
                Log.e(TAG,"Exception: "+ e.getMessage());
                throw new Exception(e.getMessage());
            } finally {
                try {
                    if(instream != null){
                        instream.close();
                    }
                } catch (Exception ignore) {}
            }
        }

        private void prepareDialog(Context context){
            dialog = new ProgressDialog(hsfContext);
            dialog.setTitle("Sending to server");
            dialog.setMessage("Please wait...");
            dialog.setCancelable(false);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(postRequest != null){
                        ABORTED = true;
                        postRequest.abort();
                    }
                }
            });
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @SuppressLint("DefaultLocale")
        @Override
        protected String doInBackground(HsFormObject... forms) {
            form = forms[0];
            Log.i(TAG,""+ form.toString()); // debug log

            HttpResponse response;
            String responseString = null;

            // load login details
            String userName = sharedPrefs.getString(res.getString(R.string.pref_user_key), null).trim();
            String userPass = sharedPrefs.getString(res.getString(R.string.pref_pass_key), null).trim();
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(userName, userPass);

            try {
                String serviceURL = form.getServiceUrl();
                if(serviceURL != null){
                    if(!serviceURL.isEmpty() && serviceURL.toLowerCase().startsWith("https://")){
                        postRequest = new HttpPost(serviceURL);
                        postRequest.addHeader(BasicScheme.authenticate(creds, "utf-8", false));
                        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                        builder.setCharset(Charset.forName("utf-8"));
                        // Fill HTTP entity
                        builder.addPart(res.getString(R.string.titlePartName), new StringBody(form.getTitle(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.descriptionPartName), new StringBody(form.getDescription(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.categoryPartName), new StringBody(form.getCategory(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.statusPartName), new StringBody(form.getStatus(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.lonPartName), new StringBody(form.getLongitude(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.latPartName), new StringBody(form.getLattitude(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.timestampPartName), new StringBody(form.getTimestamp(), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                        builder.addPart(res.getString(R.string.startTimestampPartName), new StringBody(form.getBeginTimestamp(), ContentType.TEXT_PLAIN.withCharset("utf-8")));

                        File picture = form.getPicture();
                        if(picture != null){
                            if (picture.exists() && picture.isFile()){
                                builder.addPart(res.getString(R.string.pictureSizePartName), new StringBody(String.valueOf(picture.length()), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                                builder.addPart(res.getString(R.string.pictureRotationAng), new StringBody(String.valueOf(PictureUtil.getCameraPhotoOrientation(picture.getAbsolutePath())), ContentType.TEXT_PLAIN.withCharset("utf-8")));
                                builder.addBinaryBody(res.getString(R.string.picturePartName), picture, ContentType.create("image/jpeg", "utf-8"), picture.getName());
                            }
                        }

                        final HttpEntity httpEntity = builder.build();
                        postRequest.setEntity(httpEntity);
                        if(isCancelled()){
                            return "Cancelled!";
                        }
                        response = httpclient.execute(postRequest);
                        StatusLine statusLine = response.getStatusLine();
                        Log.i(TAG, "Status line: "+statusLine.toString()); // debug log
                        if(statusLine != null){
                            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                response.getEntity().writeTo(out);
                                out.close();
                                responseString = out.toString();
                            } else{
                                //Closes the connection.
                                response.getEntity().getContent().close();
                                throw new IOException(""+statusLine.getReasonPhrase());
                            }
                        }
                    }
                    else{
                        return "Wrong service URL, please check it!";
                    }
                }
                else{
                    return "Wrong service URL, please check it!";
                }
            } catch (ClientProtocolException e) {
                Log.e(TAG,"ClientProtocolException: "+ e.getMessage()); // debug log
                cancel(false);
                return e.getMessage();
            } catch (IOException e) {
                Log.e(TAG,"IOException: "+ e.getMessage()); // debug log
                cancel(false);
                return e.getMessage();
            }
            return responseString;
        }

        /**
         *
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if(result != null){
                Log.i(TAG, "Result = "+result); // debug log
                if(result.trim().equals(SUCCESS_RESPONSE)){
                    Toast.makeText(getApplicationContext(), "Saved OK", Toast.LENGTH_SHORT).show();
                    //delete image file
                    if(form.getPicture() != null && form.getPicture().exists()){
                        boolean deleted = form.getPicture().delete();
                        Log.i(TAG, "Image deleted? = "+deleted);// debug log
                    }
                    finish();
                }
                else {
                    Toast.makeText(getApplicationContext(), "SAVE ERROR!", Toast.LENGTH_LONG).show();
                }
            }
            else{
                Toast.makeText(getApplicationContext(), "Save failed!", Toast.LENGTH_LONG).show();
            }
        }

        /**
         *
         */
        @Override
        protected void onCancelled(String result){
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if(ABORTED == true){
                Toast.makeText(getApplicationContext(), "Sending canceled!", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Save failed: "+result+"!", Toast.LENGTH_LONG).show();
                Log.i(TAG, "status final = "+getStatus().name()); // debug log
            }
        }
    }
}
