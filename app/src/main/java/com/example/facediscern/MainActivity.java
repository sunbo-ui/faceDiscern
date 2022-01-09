package com.example.facediscern;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 此项目仅在小米10上测试成功
 */
public class MainActivity extends AppCompatActivity {
    private Button send;//发送给三方接口
    private Button btnCamera;//调用安卓照相机
    private Button save;//保存到相册
    private ImageView imageView;
    private TextView textView;

    private String url = "https://api-cn.faceplusplus.com/facepp/v3/detect";
    private String api_key = "5Gy6hpIXmV-K72GCrDEr_aO0nyPjjMyq";
    private String api_secret = "P2wGH5rizRls_iBmJmVR1we-GDd5q-Q2";
    private String image_file;
    private final int CAMERA_REQUEST = 10;
    Bitmap bitmap = null; //获取暂存照片
    Bitmap bitmapZip = null;//压缩照片 (转base64太大 会扫描失败)
    private String phoPath;//照片暂存地址

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    public void initView() {


        // android 7.0系统解决拍照的问题
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        //动态获取权限
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                1);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (image_file == null || "".equals(image_file)) {
                    Toast.makeText(MainActivity.this, "请先拍个照~", Toast.LENGTH_SHORT).show();
                    return;
                }
                OkHttpClient okHttpClient = new OkHttpClient();
                //2.创建一个RequestBody,可以用add添加键值对
                RequestBody requestBody = new FormBody.Builder()
                        .add("api_key", api_key)
                        .add("api_secret", api_secret)
                        .add("return_attributes", "gender,age,beauty,skinstatus")
                        .add("image_base64", image_file)
                        .build();
                //3.创建Request对象，设置URL地址，将RequestBody作为post方法的参数传入
                Request request = new Request.Builder().url(url).post(requestBody).build();
                //4.创建一个call对象,参数就是Request请求对象
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "请求网络数据失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    //请求成功时调用该方法
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getData(response);
                            }
                        });
                    }
                });
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImageToGallery(bitmap);
            }
        });
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(MainActivity.this, "sdcard未使用~", Toast.LENGTH_SHORT).show();
                    return;
                }
                //调用摄像头
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                phoPath = getFileName();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(phoPath)));
                startActivityForResult(intent, CAMERA_REQUEST);//跳转
            }
        });
    }

    //返回结果的方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAMERA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    //将保存在本地的图片取出并显示在界面上
                    bitmap = BitmapFactory.decodeFile(phoPath);
                    //将处理过的图片显示在界面上，并保存到本地
                    imageView.setImageBitmap(bitmap);
                    getImageFile();
                }
                break;
        }
    }
    /**
     * 保存图片到图库
     */
    public void saveImageToGallery(Bitmap mBitmap) {
        if (mBitmap == null) {
            Toast.makeText(MainActivity.this, "还没有照片可以保存哦~", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, "sdcard未使用~", Toast.LENGTH_SHORT).show();
            return;
        }
        // 首先保存图片
        File appDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsoluteFile();
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "保存失败~", Toast.LENGTH_SHORT).show();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "保存失败~", Toast.LENGTH_SHORT).show();
            return;
        }
        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(getApplication().getContentResolver(), file.getAbsolutePath(), fileName, null);
            Toast.makeText(MainActivity.this, "保存成功~", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Toast.makeText(MainActivity.this, "保存失败~", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } // 最后通知图库更新
        getApplication().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + "")));
        bitmap.recycle();
    }

    public void getImageFile() {
        zip();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmapZip.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bitmapBytes = baos.toByteArray();
        //转成base64设置到ImageView
        image_file = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
        try {
            baos.flush();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void zip() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 只获取图片的大小信息，而不是将整张图片载入在内存中，避免内存溢出
        BitmapFactory.decodeFile(phoPath, options);
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 2; // 默认像素压缩比例，压缩为原图的1/2
        int minLen = Math.min(height, width); // 原图的最小边长
        if (minLen > 100) { // 如果原始图像的最小边长大于100dp（此处单位我认为是dp，而非px）
            float ratio = (float) minLen / 100.0f; // 计算像素压缩比例
            inSampleSize = (int) ratio;
        }
        options.inJustDecodeBounds = false; // 计算好压缩比例后，这次可以去加载原图了
        options.inSampleSize = inSampleSize; // 设置为刚才计算的压缩比例
        bitmapZip = BitmapFactory.decodeFile(phoPath, options); // 解码文件
    }

    private void getData(Response response) {
        try {
            String result = response.body().string();
            //这里开始解析json
            Bean bean = new Gson().fromJson(result, new TypeToken<Bean>() {
            }.getType());
            List<Bean.Face> faces = bean.faces;
            if (faces == null || faces.size() <= 0) {
                Toast.makeText(MainActivity.this, "没有找到人物哦~", Toast.LENGTH_SHORT).show();
                return;
            }
            int index = 1;
            for (Bean.Face face : faces) {
                //获取年龄
                Bean.Face.Attributes.Age age = face.attributes.age;
                //获取性别
                Bean.Face.Attributes.Gender gender = face.attributes.gender;
                //获取颜值
                Bean.Face.Attributes.Beauty beauty = face.attributes.beauty;

                textView.setText("");
                if (gender.value.equals("Male")) {
                    textView.setText(textView.getText() + "\n" + index + ": 性别:" + gender.value + " 年龄:" + age.value +
                            " 颜值:" + beauty.male_score);
                } else {
                    textView.setText(textView.getText() + "\n" + index + ": 性别:" + gender.value + " 年龄:" + age.value +
                            " 颜值:" + beauty.female_score);
                }
                index++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "照片读取错误!", Toast.LENGTH_SHORT).show();
        }
    }

    //设置文件名称
    private String getFileName() {
        String saveDir = Environment.getExternalStorageDirectory() + "/myPic";
        File file = new File(saveDir);
        if(!file.exists()){
            file.mkdirs();
        }
        //用日期作为文件名，确保唯一性
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String fileName = saveDir + formatter.format(date) + ".PNG";
        return fileName;
    }
}