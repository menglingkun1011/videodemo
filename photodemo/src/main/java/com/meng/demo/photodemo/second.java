package com.meng.demo.photodemo;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.zhy.http.okhttp.OkHttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.os.Environment.getExternalStorageDirectory;
import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;


public class second extends AppCompatActivity {

    private static final String TAG = second.class.getSimpleName();
    private Button btn;
    private Button btn1;
    private Button btn2;
    private ImageView iv1;
    private ImageView iv2;

    String srcPath = getExternalStorageDirectory().getAbsolutePath()+"/head.jpg";
    String targetPath = getExternalStorageDirectory().getAbsolutePath()+"/face.jpg";
    String targetPath1 = getExternalStorageDirectory().getAbsolutePath()+"/back.jpg";

    private int screenWidth;
    private int screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        getScreenHW();

        iv1 = (ImageView) findViewById(R.id.iv1);
        iv2 = (ImageView) findViewById(R.id.iv2);
        btn = (Button) findViewById(R.id.button);
        btn1 = (Button) findViewById(R.id.button1);
        btn2= (Button) findViewById(R.id.button2);
        btn.setOnClickListener(new View.OnClickListener() {//打开相机
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent(ACTION_IMAGE_CAPTURE);
                intent2.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(new File(getExternalStorageDirectory(), "head.jpg")));
                startActivityForResult(intent2, 2);// 采用ForResult打开
            }
        });
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//打开相册
                Intent intent1 = new Intent(Intent.ACTION_PICK, null);
                intent1.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent1, 1);
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String srcPath = Environment.getExternalStorageDirectory()+"/dog.jpg";
                String newPath = Environment.getExternalStorageDirectory()+"/smalldog.jpg";
                samplingCompress(srcPath,newPath);
            }
        });
    }

    /**
     * 获取屏幕的宽高
     */
    private void getScreenHW() {
        screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        LogUtils.e(screenWidth+":"+screenHeight);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode----->"+requestCode+",resultCode-->"+resultCode);
        switch (requestCode){
            case 1://相册
                Uri uri = data.getData();
                final String path = uri2Path(uri);
                if(!TextUtils.isEmpty(path)){
                    LogUtils.e("path:"+path);
                    new Thread(){
                        @Override
                        public void run() {
                            super.run();

                            uploadImg(path,targetPath1,iv2);
                        }
                    }.start();
                }

                break;
            case 2://相机
                new Thread(){
                    @Override
                    public void run() {
                        super.run();

                        uploadImg(srcPath,targetPath,iv1);
                    }
                }.start();

                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    Handler handler  = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

        }
    };

    /**
     * 根据图片uri获取图片的路径
     * @param uri
     * @return
     */
    private String uri2Path(Uri uri){
        String path = "";
        String[] proj = {MediaStore.Images.Media.DATA};
        ContentResolver cr = this.getContentResolver();
        Cursor cursor = cr.query(uri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        //最后根据索引值获取图片路径
        path = cursor.getString(column_index);
        return path;
    }

    /**form表单上传  上传文件大小300k以内
     * 上传图片
     * @param srcPath 原路径
     * @param targetPath 目标路径
     */
    private void uploadImg(String srcPath,String targetPath,final ImageView iv){

        File file1 = new File(srcPath);
        if(file1.exists()){
//            compress(file1,targetPath);
            samplingCompress(srcPath,targetPath);
        }
        File file = new File(targetPath);
        if(file.exists()){
            try {
                final String resposeStr = OkHttpUtils.post()
                        .addFile("head", "head.jpg", file)
                        .url("http://192.168.9.15:80/uploadimg/upload2")
                        .build()
                        .execute().body().string();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(resposeStr);
                            if(1== jsonObject.optInt("error")){
                                Toast.makeText(second.this, jsonObject.optString("info"), Toast.LENGTH_SHORT).show();
                                String url = "http://192.168.9.15:80/uploadimg/upload/"+jsonObject.optString("dir")+"/"+jsonObject.optString("fileName");
                                LogUtils.e("imgurl:-->"+url);
                                Glide.with(second.this).load(url).into(iv);
                            }else{
                                Toast.makeText(second.this, jsonObject.optInt("info"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * form表单上传
     * 上传图片的文件路径
     * @param uploadPath
     */
    private void uploadImg(String uploadPath){

        File file = new File(uploadPath);
        if(file.exists()){
            try {
                final String resposeStr = OkHttpUtils.post()
                        .addFile("head", "head.jpg", file)
                        .url("http://192.168.9.15:80/uploadimg/upload2")
                        .build()
                        .execute().body().string();
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            JSONObject jsonObject = new JSONObject(resposeStr);
                            if(1== jsonObject.optInt("error")){
                                Toast.makeText(second.this, resposeStr, Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(second.this, resposeStr, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public  void compress(File file){
        Log.i("TAG", file.getPath());
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int pos= 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压
        while(baos.size()/1000>300) {
            Log.i("TAG", baos.size()/1000+"k=============="+pos);
            pos -=10;
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, pos, baos);
        }
        FileOutputStream fos = null;
        try {
            LogUtils.e("图片大小："+baos.toByteArray().length);
            fos = new FileOutputStream(getExternalStorageDirectory().getAbsolutePath()+"/head1.jpg");
            baos.writeTo(fos);//将流写入文件
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            CloseUtils.closeIO(fos);
            CloseUtils.closeIO(baos);
        }
        CloseUtils.closeIO(fos);
        CloseUtils.closeIO(baos);

    }

    /**
     * 根据质量压缩图片
     * @param file
     * @param compressPath 压缩后存放的文件路径  Environment.getExternalStorageDirectory().getAbsolutePath()+"/head1.jpg
     */
    public  void compress(File file,String compressPath){
        Log.i("TAG", file.getPath());
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int pos= 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压
        while(baos.size()/1000>300) {
            Log.i("TAG", baos.size()/1000+"k=============="+pos);
            pos -=10;
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, pos, baos);
        }
        FileOutputStream fos = null;
        try {
            LogUtils.e("图片大小："+baos.toByteArray().length);
            fos = new FileOutputStream(compressPath);
            baos.writeTo(fos);//将流写入文件
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            CloseUtils.closeIO(fos);
            CloseUtils.closeIO(baos);
        }
        CloseUtils.closeIO(fos);
        CloseUtils.closeIO(baos);

    }

    public  void compress1(File file){
        BitmapFactory.Options opts = new BitmapFactory.Options();
        // 设置为ture只获取图片大小
        opts.inJustDecodeBounds = true;
        opts.inPreferredConfig = Bitmap.Config.ARGB_4444;
        // 获取到屏幕对象
        Display display = getWindowManager().getDefaultDisplay();
        // 获取到屏幕的真是宽和高
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();
        // 计算缩放比例
        int widthScale = opts.outWidth /screenWidth;
        int heightScale = opts.outHeight /screenHeight;
        int samPle = Math.max(widthScale,heightScale);
        opts.inSampleSize = 2;
        opts.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(getExternalStorageDirectory().getAbsolutePath()+"/head2.jpg", opts);
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new File(getExternalStorageDirectory().getAbsolutePath()+"/head2.jpeg")));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public  void compress2(File srcFilePath ,File newFilePath){
        if(srcFilePath == null || !srcFilePath.exists() || srcFilePath.isDirectory())return;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        // 设置为ture只获取图片大小
        opts.inJustDecodeBounds = true;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // 获取到屏幕对象
        Display display = getWindowManager().getDefaultDisplay();
        // 获取到屏幕的真是宽和高
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();
        // 计算缩放比例
        int widthScale = opts.outWidth /screenWidth;
        int heightScale = opts.outHeight /screenHeight;
        int samPle = Math.max(widthScale,heightScale);
        opts.inSampleSize = samPle;

        opts.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(srcFilePath.getAbsolutePath(), opts);
        FileOutputStream fos = null;
        try {
            if(newFilePath == null || !newFilePath.exists() || newFilePath.isDirectory()) {
                LogUtils.e("新文件路径不对");return;
            }
            fos = new FileOutputStream(new File(newFilePath.getAbsolutePath()));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100,fos);
            CloseUtils.closeIO(fos);
            LogUtils.e("length:"+newFilePath.length());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            CloseUtils.closeIO(fos);
        }
    }


    /**
     * 采样压缩图片
     * 按屏幕分辨率对图片进行压缩
     */
    private void samplingCompress(String srcPath,String newPath){
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath,opt);
        if(opt.outHeight > screenHeight || opt.outWidth > screenWidth){
            int wScale = opt.outWidth / screenWidth;
            int hScale = opt.outHeight / screenHeight;

            opt.inSampleSize = Math.max(wScale,hScale);
            opt.inJustDecodeBounds = false;
        }
        bitmap = BitmapFactory.decodeFile(srcPath, opt);
        qualityCompress(bitmap,newPath);

    }

    /**
     * 按照质量压缩图片
     * @param bitmap
     * @param compressPath
     */
    private void qualityCompress(Bitmap bitmap,String compressPath){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int pos= 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压
        Log.i("TAG1", baos.size()/1024+"k=============="+pos);
        while(baos.size()/1024>300) {
            Log.i("TAG2", baos.size()/1024+"k=============="+pos);
            pos -=10;
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, pos, baos);
        }
        FileOutputStream fos = null;
        try {
            LogUtils.e("图片大小："+baos.toByteArray().length/1024 +"K");
            fos = new FileOutputStream(compressPath);
            baos.writeTo(fos);//将流写入文件

            CloseUtils.closeIO(fos);
            CloseUtils.closeIO(baos);
        } catch (IOException e) {
            e.printStackTrace();
            CloseUtils.closeIO(fos);
            CloseUtils.closeIO(baos);
        }
    }

}
