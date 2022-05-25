package kr.co.musi.uploadtoserver;

import android.Manifest;
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
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    TextView messageText;
    Button uploadButton;
    ImageView imageView;
    int serverResponseCode = 0;
    ProgressDialog dialog = null;
    String upLoadServerUri = null;

    static final String TAG = "MainActivity";
    final String uploadFilePath = "/DCIM/";
    String uploadFileName = "velvet.jpg";

    //TODO [인텐트 요청 및 요청 결과 확인 위함]
    Uri photoUri;
    private static final int PICK_FROM_CAMERA = 1; // [카메라 촬영으로 사진 가져오기]
    private static final int PICK_FROM_ALBUM = 2; // [앨범에서 사진 가져오기]
    private static final int CROP_FROM_CAMERA = 3; // [가져온 사진을 자르기 위한 변수]

    //TODO [파일 경로 설명]
    //콘텐츠 파일 경로 (사진 불러오기) : content://media/external/images/media/37353
    //절대 파일 경로 (서버에 사진 등록) : /storage/emulated/0/Pictures/.pending-1621513927-PT20210513213207.JPG
    //[출처] 145. (AndroidStudio/android/java) Scoped Storage 사용해 이미지 파일 저장 및 호출 - ContentResolver , MediaStore|작성자 투케이2K


    public static final String PREFERENCES_NAME = "rebuild_preference";
    private static final String DEFAULT_VALUE_STRING = "";




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

        uploadButton = (Button) findViewById(R.id.uploadButton);
        messageText = (TextView) findViewById(R.id.messageText);
        imageView = (ImageView)findViewById(R.id.imageView);

        messageText.setText("Uploading file path : " + uploadFilePath + uploadFileName);

        upLoadServerUri = "https://www.musi.co.kr/upload/UploadToServer.php";
        uploadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goGallery();
            }
        });
    }

    public int uploadFile(String sourceFileUri) {
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
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name='uploaded_file';filename='"
                        + fileName + "'" + lineEnd);

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
                                          +" http://www.musi.co.kr/upload/"
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // READ_EXTERNAL_STORAGE 에 대한 권한 획득.

        }
    }

    private void goGallery() {
        Log.d(TAG,"================================================");
        Log.d(TAG,"\n"+"[A_ScopePicture > goGallary() 메소드 : 갤러리 인텐트 이동 실시]");
        Log.d(TAG,"================================================");
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); //[변경]
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(intent, PICK_FROM_ALBUM);
            overridePendingTransition(0, 0);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    //TODO [갤러리에서 선택한 이미지 응답 확인]
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO [정장 응답이 아닌 경우]
        if (resultCode != RESULT_OK) {
            Toast.makeText(getApplication(), "다시 시도해주세요 ... ", Toast.LENGTH_SHORT).show();
        }
        //TODO [갤러리에서 응답을 받은 경우]
        if (requestCode == PICK_FROM_ALBUM) {
            Toast.makeText(getApplication(), "잠시만 기다려주세요 ... ", Toast.LENGTH_SHORT).show();
            if (data == null) {
                return;
            }
            photoUri = data.getData();

            Log.w(TAG,"================================================");
            Log.d(TAG,"\n"+"[A_ScopePicture > onActivityResult() 메소드 : 갤러리 응답 확인 실시]");
            Log.d(TAG,"\n"+"[파일 경로 : "+String.valueOf(photoUri)+"]");
            Log.w(TAG,"================================================");

            try {
                // [선택한 이미지에서 비트맵 생성]
                InputStream in = getContentResolver().openInputStream(data.getData());
                Bitmap img = BitmapFactory.decodeStream(in);
                in.close();

                // [이미지 뷰에 이미지 표시]
                imageView.setImageBitmap(img);

                // [파일 저장 실시 후 > 다시 사진 불러오기 진행 > 서버로 등록 요청]
                saveFile(getNowTime24(), imageView, String.valueOf(photoUri));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
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
        String deleteCheck = getPrefString(getApplication(), "saveImageScopeContent");
        if(deleteCheck != null && deleteCheck.length() > 0){ //TODO 이전에 저장된 파일이 있을 경우 지운다
            try {
                ContentResolver contentResolver = getContentResolver();
                contentResolver.delete(
                        Uri.parse(getPrefString(getApplication(), "saveImageScopeContent")),
                        null,
                        null);
                Log.d("---","---");
                Log.e("//===========//","================================================");
                Log.d("","\n"+"[A_ScopePicture > saveFile() 메소드 : 이전에 저장된 파일 삭제 실시]");
                Log.d("","\n"+"[콘텐츠 파일 경로 : "+String.valueOf(deleteCheck)+"]");
                Log.d("","\n"+"[절대 파일 경로 : "+getPrefString(getApplication(), "saveImageScopeAbsolute")+"]");
                Log.e("//===========//","================================================");
                Log.d("---","---");
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        Log.d("---","---");
        Log.d("//===========//","================================================");
        Log.d("","\n"+"[A_ScopePicture > saveFile() 메소드 : MediaStore 파일 저장 실시]");
        Log.d("","\n"+"[파일 이름 : "+String.valueOf(fileName)+"]");
        Log.d("","\n"+"[원본 경로 : "+String.valueOf(fileRoot)+"]");
        Log.d("//===========//","================================================");
        Log.d("---","---");

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
                Log.d("---","---");
                Log.e("//===========//","================================================");
                Log.d("","\n"+"[A_ScopePicture > saveFile() 메소드 : MediaStore 파일 저장 실패]");
                Log.d("","\n"+"[원인 : "+String.valueOf("ParcelFileDescriptor 객체 null")+"]");
                Log.e("//===========//","================================================");
                Log.d("---","---");
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
                setPrefString(getApplication(), "saveImageScopeAbsolute", absolutePath);

                Log.d("---","---");
                Log.w("//===========//","================================================");
                Log.d("","\n"+"[A_ScopePicture > saveFile() 메소드 : MediaStore 파일 저장 성공]");
                Log.d("","\n"+"[콘텐츠 파일 경로 : "+getPrefString(getApplication(), "saveImageScopeContent")+"]");
                Log.d("","\n"+"[절대 파일 경로 : "+getPrefString(getApplication(), "saveImageScopeAbsolute")+"]");
                Log.w("//===========//","================================================");
                Log.d("---","---");

                //TODO [다시 사진 표시 실시]
                readFile(imageView, getPrefString(getApplication(), "saveImageScopeContent"));

                //TODO [서버에 사진 등록 요청 실시]
                //postRegisterPicture(postUrl, postData);
                uploadFileName = fileName+".jpg";
                postRegisterPicture(absolutePath);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void postRegisterPicture(String postUrl) {

        dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
        new Thread(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("uploading started...");
                        uploadFile(postUrl);
                    }
                });

            }
        }).start();
    }

    //TODO [MediaStore 파일 불러오기 실시]
    private void readFile(ImageView view, String path) {
        Log.d("---","---");
        Log.d("//===========//","================================================");
        Log.d("","\n"+"[A_ScopePicture > readFile() 메소드 : MediaStore 파일 불러오기 실시]");
        Log.d("","\n"+"[콘텐츠 파일 경로 : "+String.valueOf(path)+"]");
        Log.d("//===========//","================================================");
        Log.d("---","---");
        Uri externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE
        };

        Cursor cursor = getContentResolver().query(externalUri, projection, null, null, null);

        if (cursor == null || !cursor.moveToFirst()) {
            Log.d("---","---");
            Log.e("//===========//","================================================");
            Log.d("","\n"+"[A_ScopePicture > readFile() 메소드 : MediaStore 파일 불러오기 실패]");
            Log.d("","\n"+"[원인 : "+String.valueOf("Cursor 객체 null")+"]");
            Log.e("//===========//","================================================");
            Log.d("---","---");
            return;
        }

        //TODO [특정 파일 불러오기 실시]
        String contentUrl = path;
        try {
            InputStream is = getContentResolver().openInputStream(Uri.parse(contentUrl));
            if(is != null){
                // [선택한 이미지에서 비트맵 생성]
                Bitmap img = BitmapFactory.decodeStream(is);
                is.close();

                // [이미지 뷰에 이미지 표시]
                view.setImageBitmap(img);
                Log.d("---","---");
                Log.w("//===========//","================================================");
                Log.d("","\n"+"[A_ScopePicture > readFile() 메소드 : MediaStore 파일 불러오기 성공]");
                Log.d("","\n"+"[콘텐츠 파일 경로 : "+String.valueOf(contentUrl)+"]");
                Log.w("//===========//","================================================");
                Log.d("---","---");
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
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
        editor.apply();

        // [전체 데이터에 키 저장]
        String data = "";
        data = getPrefTotalKey(context);
        if(data.contains("["+key+"]") == false){
            data = data + "["+ key + "]";
            setPrefTotalKey(context, "TotalKeyAllTwoK", data);
        }
    }
    //TODO [객체 생성]
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
    //TODO [전체 Key 값 호출]
    public static String getPrefTotalKey(Context context) {
        SharedPreferences prefs = getPreferences(context);
        String value = prefs.getString("TotalKeyAllTwoK", DEFAULT_VALUE_STRING);
        return value;
    }
    //TODO [전체 key 값 저장]
    public static void setPrefTotalKey(Context context, String key, String value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
        editor.apply();
    }

}