/**
 *
 */
package cz.hsrs.hsform.picture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import cz.hsrs.hsform.R;

/**
 * @author mkepka
 *
 */
public class PictureUtil {

    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
    private Context context;
    private String mCurrentPhotoPath;
    private File photoFile = null;

    private static final String JPEG_FILE_PREFIX = "POI_IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static String TAG;

    /**
     *
     * @param album
     * @param context
     */
    public PictureUtil(AlbumStorageDirFactory album, Context context){
        this.mAlbumStorageDirFactory = album;
        this.context = context;
        TAG = context.getResources().getString(R.string.TAG);
    }

    /**
     * Method creates directory in External data storage by given name
     * @return File object with path to Album storage directory
     */
    private File getAlbumDir() {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String albumName = context.getString(R.string.albumName);
            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(albumName);
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }
        } else {
            Log.v(context.getString(R.string.appName), "External storage is not mounted READ/WRITE.");
        }
        return storageDir;
    }

    /**
     * Method creates empty temporary file where will be taken photo stored
     * @return File object for taken photo
     */
    private File createImageFile(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "";
        File albumF = getAlbumDir();
        File imageF = null;
        try {
            imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
            return imageF;
        } catch (IOException e) {
             Log.e(TAG, e.getMessage());
             return imageF;
        } finally{
            if(imageF!= null && imageF.exists()){
                imageF.deleteOnExit();
            }
        }
    }
    /**
     * Method starts Camera that will take a photo
     * @param view Current view
     */
    public Intent prepareIntentPicture(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            photoFile = createImageFile();
            mCurrentPhotoPath = photoFile.getAbsolutePath();
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                //Log.i(TAG, "Intent prepared! "); // debug log
                return takePictureIntent;
            }
            else{
                return null;
            }
        }
        else{
            return null;
        }
    }

    /**
     *
     * @param mImageView
     * @return
     */
    public File processResult(ImageView mImageView){
        if (photoFile != null) {
            //prepareImageView(mImageView);
            prepareImageViewRotate(mImageView);
            //galleryAddPic(photoFile); // could be remove?
            //Log.i(TAG, photoFile.getAbsolutePath()); //debug log
            return photoFile;
        }
        else{
            return null;
        }
    }
    /**
     * Method add thumbnail of take picture to ImageView in form
     */
    private void prepareImageView(ImageView imgView) {
        /* Get the size of the ImageView */
        int targetW = imgView.getWidth();
        int targetH = imgView.getHeight();

        //Log.i(TAG, "ImageView W="+targetW+", H="+targetH); // debug log

        /* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        /* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
            Log.i(TAG, "scale factor="+scaleFactor); // debug log
        }

        int rotate = getCameraPhotoOrientation(mCurrentPhotoPath);
        Log.i(TAG, "rotate="+rotate); // debug log

        /* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        /* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        /* Associate the Bitmap to the ImageView */
        imgView.setImageBitmap(bitmap);
        imgView.setVisibility(View.VISIBLE);
    }

    /**
     * Method add thumbnail of take picture to ImageView in form
     * with rotation to normal position
     */
    private void prepareImageViewRotate(ImageView imgView) {
        /* Get the size of the ImageView */
        int targetW = imgView.getWidth();
        int targetH = imgView.getHeight();

        //Log.i(TAG, "ImageView W="+targetW+", H="+targetH); // debug log

        /* Check orientation */
        int rotate = getCameraPhotoOrientation(mCurrentPhotoPath);
        //Log.i(TAG, "rotate="+rotate); // debug log

        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);

        /* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        int photoW;
        int photoH;
        if(rotate == 90 || rotate == 270){
            photoW = bmOptions.outHeight;
            photoH = bmOptions.outWidth;
        }
        else{
            photoW = bmOptions.outWidth;
            photoH = bmOptions.outHeight;
        }

        /* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
            //Log.i(TAG, "scale factor="+scaleFactor); // debug log
        }

        /* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        /* Decode the JPEG file into a Bitmap */
        Bitmap bitmapScaled = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Bitmap rotatedScaled = Bitmap.createBitmap(bitmapScaled, 0, 0, bitmapScaled.getWidth(), bitmapScaled.getHeight(), matrix, true);
        /* Associate the Bitmap to the ImageView */
        imgView.setImageBitmap(rotatedScaled);
        imgView.setVisibility(View.VISIBLE);
    }

    /**
     * Method adds new picture taken by camera to standard Android gallery
     * @return taken picture as File
     */
    public void galleryAddPic(File photo) {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        //File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(photo);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
        //return f;
    }

    public void galleryUpdate(){
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                Uri.parse("file://" +  mAlbumStorageDirFactory)));
    }

    /**
     * Method get orientation of an image and converts it to rotation angle in degrees
     * @param fileName name of image file
     * @return rotation angle in degrees (0, 90, 180, 270)
     */
    public static int getCameraPhotoOrientation(String fileName){
         int rotate = 0;
         try {
             ExifInterface exif = new ExifInterface(fileName);
             int orientation = exif.getAttributeInt(
                     ExifInterface.TAG_ORIENTATION,
                     ExifInterface.ORIENTATION_NORMAL);

             switch (orientation) {
             case ExifInterface.ORIENTATION_ROTATE_270:
                 rotate = 270;
                 break;
             case ExifInterface.ORIENTATION_ROTATE_180:
                 rotate = 180;
                 break;
             case ExifInterface.ORIENTATION_ROTATE_90:
                 rotate = 90;
                 break;
             }
             //Log.i(TAG, "Exif orientation: " + orientation); //debug log
         } catch (Exception e) {
             Log.e(TAG, "Exception: "+e.getMessage());
         }
        return rotate;
     }
}
