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

    //TODO [인텐트 요청 및 요청 결과 확인 위함]
    Uri photoUri;

    //TODO [파일 경로 설명]
    //콘텐츠 파일 경로 (사진 불러오기) : content://media/external/images/media/37353
    //절대 파일 경로 (서버에 사진 등록) : /storage/emulated/0/Pictures/.pending-1621513927-PT20210513213207.JPG
    //[출처] 145. (AndroidStudio/android/java) Scoped Storage 사용해 이미지 파일 저장 및 호출 - ContentResolver , MediaStore|작성자 투케이2K


    public static final String PREFERENCES_NAME = "rebuild_preference";
    private static final String DEFAULT_VALUE_STRING = "";

    ArrayList<WearItem> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // OS가 Marshmallow 이상일 경우 권한체크를 해야 합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
            } else {
                // READ_EXTERNAL_STORAGE 에 대한 권한이 있음.

            }
        }
        // OS가 Marshmallow 이전일 경우 권한체크를 하지 않는다.
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
            // READ_EXTERNAL_STORAGE 에 대한 권한 획득.

        }
    }

    private void openGallery() {
        Log.d(TAG,"================================================");
        Log.d(TAG,"\n"+"[A_ScopePicture > goGallary() 메소드 : 갤러리 인텐트 이동 실시]");
        Log.d(TAG,"================================================");
        try {
            /*
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); //[변경]
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

                        Toast.makeText(getApplication(), "잠시만 기다려주세요 ... ", Toast.LENGTH_SHORT).show();
                        photoUri = result.getData().getData();

                        Log.d(TAG,"================================================");
                        Log.d(TAG,"\n"+"[A_ScopePicture > onActivityResult() 메소드 : 갤러리 응답 확인 실시]");
                        Log.d(TAG,"\n"+"[파일 경로 : "+String.valueOf(photoUri)+"]");
                        Log.d(TAG,"================================================");

                        try {
                            // [선택한 이미지에서 비트맵 생성]
                            readImageFile(imageView, String.valueOf(photoUri));

                            // [파일 저장 실시 후 > 다시 사진 불러오기 진행 > 서버로 등록 요청]
                            saveFile(getNowTime24(), imageView, String.valueOf(photoUri));
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    //TODO [현재 시간 알아오는 메소드]
    public static String getNowTime24() {
        long time = System.currentTimeMillis();
        //SimpleDateFormat dayTime = new SimpleDateFormat("hh:mm:ss");
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyyMMddkkmmss");
        String str = dayTime.format(new Date(time));
        return "PT"+str; //TODO [PT는 picture 의미]
    }

    //TODO [MediaStore 파일 저장 실시]
    private void saveFile(String fileName, ImageView view, String fileRoot) {
        Log.d(TAG, "saveFile:"+fileRoot+","+fileName);
        String deleteCheck = getPrefString(getApplication(), "saveImageScopeContent");
        if(deleteCheck != null && deleteCheck.length() > 0){ //TODO 이전에 저장된 파일이 있을 경우 지운다
            try {
                ContentResolver contentResolver = getContentResolver();
                contentResolver.delete(
                        Uri.parse(getPrefString(getApplication(), "saveImageScopeContent")),
                        null,
                        null);
                Log.d(TAG,"\n"+"[A_ScopePicture > saveFile() 메소드 : 이전에 저장된 파일 삭제 실시]");
                Log.d(TAG,"\n"+"[콘텐츠 파일 경로 : "+String.valueOf(deleteCheck)+"]");
                Log.d(TAG,"\n"+"[절대 파일 경로 : "+getPrefString(getApplication(), "saveImageScopeAbsolute")+"]");
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        Log.d(TAG,"\n"+"[A_ScopePicture > saveFile() 메소드 : MediaStore 파일 저장 실시]");
        Log.d(TAG,"\n"+"[파일 이름 : "+String.valueOf(fileName)+"]");
        Log.d(TAG,"\n"+"[원본 경로 : "+String.valueOf(fileRoot)+"]");

        //TODO [저장하려는 파일 타입, 이름 지정]
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName+".JPG");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }
        ContentResolver contentResolver = getContentResolver();
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            //TODO [쓰기 모드 지정]
            ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(item, "w", null);

            if (pdf == null) {
                Log.d(TAG,"\n"+"[A_ScopePicture > saveFile() 메소드 : MediaStore 파일 저장 실패]");
                Log.d(TAG,"\n"+"[원인 : "+String.valueOf("ParcelFileDescriptor 객체 null")+"]");
            } else {
                //TODO [이미지 뷰에 표시된 사진을 얻어온다]
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                //Bitmap bitmap= BitmapFactory.decodeFile(filePath, options);
                Bitmap bitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();

                //TODO [이미지 리사이징 실시]
                int width = 354; // [축소시킬 너비 (증명사진 크기 기준)]
                int height = 472; //[축소시킬 높이 (증명사진 크기 기준)]
                float bmpWidth = bitmap.getWidth();
                float bmpHeight = bitmap.getHeight();

                if (bmpWidth > width) {
                    // [원하는 너비보다 클 경우의 설정]
                    float mWidth = bmpWidth / 100;
                    float scale = width/ mWidth;
                    bmpWidth *= (scale / 100);
                    bmpHeight *= (scale / 100);
                } else if (bmpHeight > height) {
                    // [원하는 높이보다 클 경우의 설정]
                    float mHeight = bmpHeight / 100;
                    float scale = height/ mHeight;
                    bmpWidth *= (scale / 100);
                    bmpHeight *= (scale / 100);
                }

                //TODO [리사이징 된 파일을 비트맵에 담는다]
                //Bitmap resizedBmp = Bitmap.createScaledBitmap(bitmap, (int) bmpWidth, (int) bmpHeight, true); //TODO [해상도 맞게 표시]
                Bitmap resizedBmp = Bitmap.createScaledBitmap(bitmap, (int) width, (int) height, true); //TODO [무조건 증명 사진 기준]
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] imageInByte = baos.toByteArray();

                //TODO [이미지를 저장하는 부분]
                FileOutputStream outputStream = new FileOutputStream(pdf.getFileDescriptor());
                outputStream.write(imageInByte);
                outputStream.close();

                //TODO [경로 저장 실시]
                //[콘텐츠 : 이미지 경로 저장]
                setPrefString(getApplication(), "saveImageScopeContent", String.valueOf(item));

                //[절대 : 이미지 경로 저장]
                Cursor c = getContentResolver().query(Uri.parse(String.valueOf(item)), null,null,null,null);
                c.moveToNext();
                String absolutePath = c.getString(c.getColumnIndex(MediaStore.MediaColumns.DATA));
                Log.d(TAG, "absolutePath:"+absolutePath);
                setPrefString(getApplication(), "saveImageScopeAbsolute", absolutePath);

                Log.d(TAG,"\n"+"[A_ScopePicture > saveFile() 메소드 : MediaStore 파일 저장 성공]");
                Log.d(TAG,"\n"+"[콘텐츠 파일 경로 : "+getPrefString(getApplication(), "saveImageScopeContent")+"]");
                Log.d(TAG,"\n"+"[절대 파일 경로 : "+getPrefString(getApplication(), "saveImageScopeAbsolute")+"]");

                //TODO [다시 사진 표시]
                readImageFile(imageView, getPrefString(getApplication(), "saveImageScopeContent"));

                //TODO [서버에 사진 등록 요청 실시]
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

    //이미지 뷰에 이미지 표시
    private void readImageFile(ImageView view, String contentUrl) {

        if (checkMediaStore()) return;  // 저장공간 접근권한 체크

        //TODO [특정 파일 불러오기 실시]
        try {
            InputStream is = getContentResolver().openInputStream(Uri.parse(contentUrl));
            if(is != null){
                // [선택한 이미지에서 비트맵 생성]
                Bitmap img = BitmapFactory.decodeStream(is);
                is.close();

                // [이미지 뷰에 이미지 표시]
                view.setImageBitmap(img);
                Log.d(TAG,"\n"+"[A_ScopePicture > readFile() 메소드 : MediaStore 파일 불러오기 성공]");
                Log.d(TAG,"\n"+"[콘텐츠 파일 경로 : "+String.valueOf(contentUrl)+"]");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //TODO [String 값 호출]
    public static String getPrefString(Context context, String key) {
        SharedPreferences prefs = getPreferences(context);
        String value = prefs.getString(key, DEFAULT_VALUE_STRING);
        return value;
    }
    //TODO [String 값 저장]
    public static void setPrefString(Context context, String key, String value) {
        Log.d(TAG, "setPrefString:"+key+","+value);
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
        editor.apply();
    }
    //TODO [객체 생성]
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private boolean checkMediaStore() {
        // 저장공간에 대한 접근 권한 여부 체크
        Uri externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE
        };

        Cursor cursor = getContentResolver().query(externalUri, projection, null, null, null);

        if (cursor == null || !cursor.moveToFirst()) {
            Log.d(TAG, "\n" + "[A_ScopePicture > readFile() 메소드 : MediaStore 파일 불러오기 실패]");
            Log.d(TAG, "\n" + "[원인 : " + String.valueOf("Cursor 객체 null") + "]");
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
            // doInBackground 에서 받아온 total 값 사용 장소
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
            // 추가할 레이아웃
            SubLayout subLayout = new SubLayout(getApplicationContext(), arrayList.get(i));
            // 추가될 위치
            LinearLayout layout = (LinearLayout)findViewById(R.id.input_here_layout);
            layout.addView(subLayout);
        }
    }
}