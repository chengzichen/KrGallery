package com.hc.gallery;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dhc.gallery.GalleryConfig;
import com.dhc.gallery.GalleryHelper;
import com.dhc.gallery.ui.GalleryActivity;
import com.hc.gallery.util.StorageType;
import com.hc.gallery.util.StorageUtil;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PermissionListener {
    List<String> mList;
    String outputPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBar();
        setContentView(R.layout.activity_main);
        StorageUtil.init(this, null);//初始化监测sdcard
        mList = new ArrayList<>();
        mList.add((mList.size() + 1) + ".  选择单张图片");
        mList.add((mList.size() + 1) + ".  选择单张图片并裁剪");
        mList.add((mList.size() + 1) + ".  选择多张图片");
        mList.add((mList.size() + 1) + ".  选择视频");
        mList.add((mList.size() + 1) + ".  拍摄视频(可限制时长)");
        mList.add((mList.size() + 1) + ".  拍照片");
        mList.add((mList.size() + 1) + ".  拍照片并裁剪");
        ListView listView = (ListView) findViewById(R.id.ls_home);

        listView.setAdapter(new MyAdapter(this));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!AndPermission.hasPermission(MainActivity.this
                        , Manifest.permission.CAMERA
                        , Manifest.permission.READ_PHONE_STATE
                        , Manifest.permission.RECORD_AUDIO)) {
                    AndPermission.with(MainActivity.this)
                            .requestCode(100)
                            .permission(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_PHONE_STATE)
                            .rationale(new RationaleListener() {
                                @Override
                                public void showRequestPermissionRationale(int requestCode, Rationale rationale) {
                                    AndPermission.rationaleDialog(MainActivity.this, rationale).show();
                                }
                            })
                             .send();
                }
                switch (position) {
                    case 0: /*** 选择单张图片 onActivityResult{@link GalleryActivity.PHOTOS}*/
                        GalleryHelper.with(MainActivity.this).type(GalleryConfig.SELECT_PHOTO).requestCode(12).singlePhoto().execute();
                        break;
                    case 1:  /***选择单张图片并裁剪 onActivityResult{@link GalleryActivity.PHOTOS}*/
                        outputPath = StorageUtil.getWritePath(StorageUtil.get32UUID() + ".jpg", StorageType.TYPE_TEMP);
                        GalleryHelper.with(MainActivity.this).type(GalleryConfig.SELECT_PHOTO).requestCode(12).singlePhoto().isNeedCropWithPath(outputPath).execute();
                        break;
                    case 2:  /*** 选择多张图片 onActivityResult{@link GalleryActivity.PHOTOS}*/
                        GalleryHelper.with(MainActivity.this).type(GalleryConfig.SELECT_PHOTO).requestCode(12).limitPickPhoto(9).execute();
                        break;
                    case 3:/***选择视频 onActivityResult{@link GalleryActivity.VIDEO}*/
                        GalleryHelper.with(MainActivity.this).type(GalleryConfig.SELECT_VEDIO).requestCode(12).isSingleVedio().execute();
                        break;
                    case 4:/***拍摄视频 onActivityResult{@link GalleryActivity.VIDEO}*/

                        GalleryHelper.with(MainActivity.this).type(GalleryConfig.RECORD_VEDIO).requestCode(12)
//                                .limitRecordTime(10)//限制时长
                                .limitRecordSize(1)//限制大小
                                .execute();
                        break;
                    case 5:/***拍照片onActivityResult {@link GalleryActivity.PHOTOS}*/
                        GalleryHelper.with(MainActivity.this).type(GalleryConfig.TAKE_PHOTO).requestCode(12).execute();
                        break;
                    case 6: /***拍照片并裁剪 onActivityResult{@link GalleryActivity.CROP}*/
                        outputPath = StorageUtil.getWritePath(StorageUtil.get32UUID() + ".jpg", StorageType.TYPE_TEMP);
                        GalleryHelper.with(MainActivity.this).type(GalleryConfig.TAKE_PHOTO).isNeedCropWithPath(outputPath).requestCode(12).execute();
                        break;
                    default:
                        break;
                }
            }
        });
    }



    private class MyAdapter extends BaseAdapter {
        Context mContext;
        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            mContext = context;
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.list_item, null);
                holder.info = (TextView) convertView.findViewById(R.id.tv_info);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.info.setText(mList.get(position));

            return convertView;
        }

        class ViewHolder {
            public TextView info;
        }
    }


    //----------------------------------权限回调处理----------------------------------//

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        /**
         * 转给AndPermission分析结果。
         *
         * @param requestCode  请求码。
         * @param permissions  权限数组，一个或者多个。
         * @param grantResults 请求结果。
         * @param listener PermissionListener 对象。
         */
        AndPermission.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Override
    public void onSucceed(int requestCode, List<String> grantPermissions) {
        Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFailed(int requestCode, List<String> deniedPermissions) {
        Toast.makeText(this, "权限获取失败", Toast.LENGTH_SHORT).show();
        // 用户否勾选了不再提示并且拒绝了权限，那么提示用户到设置中授权。
        if (!AndPermission.hasAlwaysDeniedPermission(this, deniedPermissions)) {
            // 第一种：用默认的提示语。
            AndPermission.defaultSettingDialog(this, 300).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (12 == requestCode && resultCode == Activity.RESULT_OK) {
            if (data.getStringArrayListExtra(GalleryActivity.PHOTOS) != null) {//选择图片返回

                ArrayList<String> path = data.getStringArrayListExtra(GalleryActivity.PHOTOS);
                Toast.makeText(MainActivity.this, path.toString(), Toast.LENGTH_SHORT).show();

            } else if (data.getStringExtra(GalleryActivity.VIDEO) != null) {//选择和拍摄视频(目前支持单选)

                String path = data.getStringExtra(GalleryActivity.VIDEO);
                Toast.makeText(MainActivity.this, path.toString(), Toast.LENGTH_SHORT).show();

            } else if (data.getStringExtra(GalleryActivity.DATA) != null) {//裁剪回来
                if (outputPath == null) {//没有传入返回裁剪路径
                    byte[] datas = data.getByteArrayExtra(GalleryActivity.DATA);
                    Toast.makeText(MainActivity.this, datas.toString(), Toast.LENGTH_SHORT).show();

                }
            }
        }else if(requestCode==300&&resultCode == Activity.RESULT_OK){
                Toast.makeText(this, "从设置回来", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * 隐藏状态栏
     * <p>
     * 在setContentView前调用
     */
    protected void hideStatusBar() {
        final int sdkVer = Build.VERSION.SDK_INT;
        if (sdkVer < 16) {
            //4.0及一下
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }
    }
}
