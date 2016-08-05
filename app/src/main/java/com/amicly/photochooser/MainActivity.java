package com.amicly.photochooser;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainAct";

    private static final int REQUEST_FROM_GALLERY = 0;
    private static final int REQUEST_CROP = 6;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showPhotoGalleryChooser();
    }

    public void showPhotoGalleryChooser() {

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            Intent pickPhotoIntent = new Intent();
            pickPhotoIntent.setAction(Intent.ACTION_GET_CONTENT);
            pickPhotoIntent.setType("image/*");

            if (pickPhotoIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(
                        Intent.createChooser(pickPhotoIntent, "Select Picture"), REQUEST_FROM_GALLERY);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_FROM_GALLERY) {
                Uri selectedImageUri = data.getData();
//                String imagePath = getFilePathUri(this, selectedImageUri);
//                Intent cropIntent = ImageUtils.getCropIntent(this, imagePath);
//                startActivityForResult(cropIntent, REQUEST_CROP);
                cropImageFromUri(selectedImageUri);
            }
            if (requestCode == REQUEST_CROP ) {
                Uri selectedImageUri = data.getData();
                String selectedImagePath = getImageFilePathFromFileUri(selectedImageUri);
                getBytesFromFile(selectedImagePath);
            }
        }
    }

    public static byte[] getBytesFromFile(String path) {

        File file = new File(path);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bytes;
    }

    public static String getImageFilePathFromFileUri(Uri fileUri) {
        String path = fileUri.toString().replace("file://", "");
        return path;
    }

    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static Intent getCropIntent(Context context, String fileString) {
        File file = new File(context.getExternalCacheDir(), "croppedImage");
        Uri outputUri = Uri.fromFile(file);


        Intent cropIntent = new Intent("com.android.camera.action.CROP");

        // Check to see if a Google application can handle the crop intent

        ComponentName cropWithGoogleApp = isGooglePhotosCropAvailable(context);

        if (cropWithGoogleApp != null) {
            cropIntent.setComponent(cropWithGoogleApp);
        }

        // indicate image type and Uri
        File f = new File(fileString);
        Uri contentUri = Uri.fromFile(f);

        cropIntent.setDataAndType(contentUri, "image/*");
        // set crop properties
        cropIntent.putExtra("crop", "true");
        // indicate aspect of desired crop
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        // indicate output X and Y
        cropIntent.putExtra("outputX", 700);
        cropIntent.putExtra("outputY", 700);

        // true to return a Bitmap, false to directly save the cropped image
        cropIntent.putExtra("return-data", false);


        //save output image in uri
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

        return cropIntent;

    }

    public static ComponentName isGooglePhotosCropAvailable(Context context) {

        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);

        if (resolveInfoList == null ) {
            return null;
        }

        for (ResolveInfo resolveInfo : resolveInfoList) {

            if (resolveInfo.activityInfo.toString().contains("com.google.android.apps")) {
                String packageName = resolveInfo.activityInfo.packageName;
                String className = resolveInfo.activityInfo.name;
                return new ComponentName(packageName, className);
            }

        }

        return null;

    }


    public void cropImageFromUri(@NonNull Uri imageUri) {
        Log.d(TAG, "cropImageFromUri: beginning to crop image");
        String imagePath = getFilePathUri(this, imageUri);
        Intent cropIntent = getCropIntent(this, imagePath);
        startActivityForResult(cropIntent, REQUEST_CROP);
    }

    public static String getFilePathUri(final Context context, final Uri uri) {
        Log.d(TAG, "getFilePathUri: getting file path uri");

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
//                if (DEBUG)
//                    DatabaseUtils.dumpCursor(cursor);

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}
