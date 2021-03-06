package kr.co.musi.uploadtoserver;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    TextView messageText;
    Button uploadButton;
    Button downloadButton;
    Button showImages;
    ImageView imageView;
    ImageView imageViewDown;

    int serverResponseCode = 0;
    ProgressDialog dialog = null;
    String upLoadServerUri = null;
    String downloadServerUri = null;

    static final String TAG = "MainActivity";
    final String uploadFilePath = "/data/";
    String uploadFileName = "velvet.jpg";

    ActivityResultLauncher<Intent> resultLauncher;

    //TODO [????????? ?????? ??? ?????? ?????? ?????? ??????]
    Uri photoUri;

    //TODO [?????? ?????? ??????]
    //????????? ?????? ?????? (?????? ????????????) : content://media/external/images/media/37353
    //?????? ?????? ?????? (????????? ?????? ??????) : /storage/emulated/0/Pictures/.pending-1621513927-PT20210513213207.JPG
    //[??????] 145. (AndroidStudio/android/java) Scoped Storage ????????? ????????? ?????? ?????? ??? ?????? - ContentResolver , MediaStore|????????? ?????????2K


    public static final String PREFERENCES_NAME = "rebuild_preference";
    private static final String DEFAULT_VALUE_STRING = "";

    ArrayList<WearItem> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // OS??? Marshmallow ????????? ?????? ??????????????? ?????? ?????????.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
            } else {
                // READ_EXTERNAL_STORAGE ??? ?????? ????????? ??????.

            }
        }
        // OS??? Marshmallow ????????? ?????? ??????????????? ?????? ?????????.
        else{

        }
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }


        uploadButton = (Button) findViewById(R.id.uploadButton);
        downloadButton = (Button) findViewById(R.id.downloadButton);
        showImages = (Button) findViewById(R.id.showImages);
        messageText = (TextView) findViewById(R.id.messageText);
        imageView = (ImageView)findViewById(R.id.imageView);
        imageViewDown = (ImageView)findViewById(R.id.imageViewDown);

        messageText.setText("Uploading file path : " + uploadFilePath + uploadFileName);

        openGalleryCallbak();

        upLoadServerUri = "https://www.musi.co.kr/upload/UploadToServer.php";
        uploadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openGallery();
            }
        });
        downloadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                downloadServerUri = "https://www.musi.co.kr/upload/data/" + uploadFileName;
                new DownloadFilesTask().execute(downloadServerUri);
            }
        });
        showImages.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showImagesAll();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // READ_EXTERNAL_STORAGE ??? ?????? ?????? ??????.

        }
    }

    private void openGallery() {
        Log.d(TAG,"================================================");
        Log.d(TAG,"\n"+"[A_ScopePicture > goGallary() ????????? : ????????? ????????? ?????? ??????]");
        Log.d(TAG,"================================================");
        try {
            /*
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); //[??????]
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(intent, PICK_FROM_ALBUM);
            overridePendingTransition(0, 0);
            */
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            resultLauncher.launch(intent);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    private void openGalleryCallbak() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        if(result.getData() == null) {
                            return;
                        }

                        Toast.makeText(getApplication(), "????????? ?????????????????? ... ", Toast.LENGTH_SHORT).show();
                        photoUri = result.getData().getData();

                        Log.d(TAG,"================================================");
                        Log.d(TAG,"\n"+"[A_ScopePicture > onActivityResult() ????????? : ????????? ?????? ?????? ??????]");
                        Log.d(TAG,"\n"+"[?????? ?????? : "+String.valueOf(photoUri)+"]");
                        Log.d(TAG,"================================================");

                        try {
                            // [????????? ??????????????? ????????? ??????]
                            readImageFile(imageView, String.valueOf(photoUri));

                            // [?????? ?????? ?????? ??? > ?????? ?????? ???????????? ?????? > ????????? ?????? ??????]
                            saveFile(getNowTime24(), imageView, String.valueOf(photoUri));
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    //TODO [?????? ?????? ???????????? ?????????]
    public static String getNowTime24() {
        long time = System.currentTimeMillis();
        //SimpleDateFormat dayTime = new SimpleDateFormat("hh:mm:ss");
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyyMMddkkmmss");
        String str = dayTime.format(new Date(time));
        return "PT"+str; //TODO [PT??? picture ??????]
    }

    //TODO [MediaStore ?????? ?????? ??????]
    private void saveFile(String fileName, ImageView view, String fileRoot) {
        Log.d(TAG, "saveFile:"+fileRoot+","+fileName);
        String deleteCheck = getPrefString(getApplication(), "saveImageScopeContent");
        if(deleteCheck != null && deleteCheck.length() > 0){ //TODO ????????? ????????? ????????? ?????? ?????? ?????????
            try {
                ContentResolver contentResolver = getContentResolver();
                contentResolver.delete(
                        Uri.parse(getPrefString(getApplication(), "saveImageScopeContent")),
                        null,
                        null);
                Log.d(TAG,"\n"+"[A_ScopePicture > saveFile() ????????? : ????????? ????????? ?????? ?????? ??????]");
                Log.d(TAG,"\n"+"[????????? ?????? ?????? : "+String.valueOf(deleteCheck)+"]");
                Log.d(TAG,"\n"+"[?????? ?????? ?????? : "+getPrefString(getApplication(), "saveImageScopeAbsolute")+"]");
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        Log.d(TAG,"\n"+"[A_ScopePicture > saveFile() ????????? : MediaStore ?????? ?????? ??????]");
        Log.d(TAG,"\n"+"[?????? ?????? : "+String.valueOf(fileName)+"]");
        Log.d(TAG,"\n"+"[?????? ?????? : "+String.valueOf(fileRoot)+"]");

        //TODO [??????????????? ?????? ??????, ?????? ??????]
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName+".JPG");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }
        ContentResolver contentResolver = getContentResolver();
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            //TODO [?????? ?????? ??????]
            ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(item, "w", null);

            if (pdf == null) {
                Log.d(TAG,"\n"+"[A_ScopePicture > saveFile() ????????? : MediaStore ?????? ?????? ??????]");
                Log.d(TAG,"\n"+"[?????? : "+String.valueOf("ParcelFileDescriptor ?????? null")+"]");
            } else {
                //TODO [????????? ?????? ????????? ????????? ????????????]
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                //Bitmap bitmap= BitmapFactory.decodeFile(filePath, options);
                Bitmap bitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();

                //TODO [????????? ???????????? ??????]
                int width = 354; // [???????????? ?????? (???????????? ?????? ??????)]
                int height = 472; //[???????????? ?????? (???????????? ?????? ??????)]
                float bmpWidth = bitmap.getWidth();
                float bmpHeight = bitmap.getHeight();

                if (bmpWidth > width) {
                    // [????????? ???????????? ??? ????????? ??????]
                    float mWidth = bmpWidth / 100;
                    float scale = width/ mWidth;
                    bmpWidth *= (scale / 100);
                    bmpHeight *= (scale / 100);
                } else if (bmpHeight > height) {
                    // [????????? ???????????? ??? ????????? ??????]
                    float mHeight = bmpHeight / 100;
                    float scale = height/ mHeight;
                    bmpWidth *= (scale / 100);
                    bmpHeight *= (scale / 100);
                }

                //TODO [???????????? ??? ????????? ???????????? ?????????]
                //Bitmap resizedBmp = Bitmap.createScaledBitmap(bitmap, (int) bmpWidth, (int) bmpHeight, true); //TODO [????????? ?????? ??????]
                Bitmap resizedBmp = Bitmap.createScaledBitmap(bitmap, (int) width, (int) height, true); //TODO [????????? ?????? ?????? ??????]
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] imageInByte = baos.toByteArray();

                //TODO [???????????? ???????????? ??????]
                FileOutputStream outputStream = new FileOutputStream(pdf.getFileDescriptor());
                outputStream.write(imageInByte);
                outputStream.close();

                //TODO [?????? ?????? ??????]
                //[????????? : ????????? ?????? ??????]
                setPrefString(getApplication(), "saveImageScopeContent", String.valueOf(item));

                //[?????? : ????????? ?????? ??????]
                Cursor c = getContentResolver().query(Uri.parse(String.valueOf(item)), null,null,null,null);
                c.moveToNext();
                String absolutePath = c.getString(c.getColumnIndex(MediaStore.MediaColumns.DATA));
                Log.d(TAG, "absolutePath:"+absolutePath);
                setPrefString(getApplication(), "saveImageScopeAbsolute", absolutePath);

                Log.d(TAG,"\n"+"[A_ScopePicture > saveFile() ????????? : MediaStore ?????? ?????? ??????]");
                Log.d(TAG,"\n"+"[????????? ?????? ?????? : "+getPrefString(getApplication(), "saveImageScopeContent")+"]");
                Log.d(TAG,"\n"+"[?????? ?????? ?????? : "+getPrefString(getApplication(), "saveImageScopeAbsolute")+"]");

                //TODO [?????? ?????? ??????]
                readImageFile(imageView, getPrefString(getApplication(), "saveImageScopeContent"));

                //TODO [????????? ?????? ?????? ?????? ??????]
                uploadFileName = fileName+".jpg";
                postRegisterPicture(absolutePath, uploadFileName);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void postRegisterPicture(String postUrl, String filename) {

        dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
        new Thread(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("uploading started...");
                        uploadFileToServer(postUrl, filename);
                    }
                });
            }
        }).start();
    }

    //????????? ?????? ????????? ??????
    private void readImageFile(ImageView view, String contentUrl) {

        if (checkMediaStore()) return;  // ???????????? ???????????? ??????

        //TODO [?????? ?????? ???????????? ??????]
        try {
            InputStream is = getContentResolver().openInputStream(Uri.parse(contentUrl));
            if(is != null){
                // [????????? ??????????????? ????????? ??????]
                Bitmap img = BitmapFactory.decodeStream(is);
                is.close();

                // [????????? ?????? ????????? ??????]
                view.setImageBitmap(img);
                Log.d(TAG,"\n"+"[A_ScopePicture > readFile() ????????? : MediaStore ?????? ???????????? ??????]");
                Log.d(TAG,"\n"+"[????????? ?????? ?????? : "+String.valueOf(contentUrl)+"]");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //TODO [String ??? ??????]
    public static String getPrefString(Context context, String key) {
        SharedPreferences prefs = getPreferences(context);
        String value = prefs.getString(key, DEFAULT_VALUE_STRING);
        return value;
    }
    //TODO [String ??? ??????]
    public static void setPrefString(Context context, String key, String value) {
        Log.d(TAG, "setPrefString:"+key+","+value);
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
        editor.apply();
    }
    //TODO [?????? ??????]
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private boolean checkMediaStore() {
        // ??????????????? ?????? ?????? ?????? ?????? ??????
        Uri externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE
        };

        Cursor cursor = getContentResolver().query(externalUri, projection, null, null, null);

        if (cursor == null || !cursor.moveToFirst()) {
            Log.d(TAG, "\n" + "[A_ScopePicture > readFile() ????????? : MediaStore ?????? ???????????? ??????]");
            Log.d(TAG, "\n" + "[?????? : " + String.valueOf("Cursor ?????? null") + "]");
            return true;
        }
        return false;
    }


    public int uploadFileToServer(String sourceFileUri, String targetFilename) {
        String fileName = sourceFileUri;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

            dialog.dismiss();
            Log.e(TAG, "Source File not exist :" + sourceFileUri);

            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File not exist : " + sourceFileUri);
                }
            });

            return 0;

        } else {
            Log.d(TAG, "source file exist. : " + sourceFileUri);

            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                if (conn == null) {
                    Log.e(TAG, "conn is null!!");
                }
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", targetFilename);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name='uploaded_file';filename='"
                        + targetFilename + "'" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i(TAG, "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){

                    runOnUiThread(new Runnable() {
                        public void run() {

                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
                                    +" http://www.musi.co.kr/upload/data/"
                                    +uploadFileName;

                            messageText.setText(msg);
                            Toast.makeText(MainActivity.this, "File Upload Complete.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(MainActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e(TAG, "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e(TAG, "Exception : "
                        + e.getMessage(), e);
            }

            dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }




    private class DownloadFilesTask extends AsyncTask<String,Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bmp = null;
            try {
                String img_url = strings[0]; //url of the image
                URL url = new URL(img_url);
                bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bmp;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected void onPostExecute(Bitmap result) {
            // doInBackground ?????? ????????? total ??? ?????? ??????
            imageViewDown.setImageBitmap(result);
        }
    }
    private void showImagesAll() {

        arrayList = new ArrayList<>();
        WearItem sampleItem = new WearItem("https://www.musi.co.kr/upload/data/PT20220527232446.jpg");

        arrayList.add(sampleItem);
        arrayList.add(sampleItem);
        arrayList.add(sampleItem);
        arrayList.add(sampleItem);

        for (int i = 0; i < arrayList.size(); i++){
            // ????????? ????????????
            SubLayout subLayout = new SubLayout(getApplicationContext(), arrayList.get(i));
            // ????????? ??????
            LinearLayout layout = (LinearLayout)findViewById(R.id.input_here_layout);
            layout.addView(subLayout);
        }
    }
}