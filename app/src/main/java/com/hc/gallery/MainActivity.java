package com.hc.gallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dhc.gallery.ui.GalleryActivity;
import com.dhc.gallery.GalleryConfig;
import com.dhc.gallery.GalleryHelper;
import com.hc.gallery.util.StorageType;
import com.hc.gallery.util.StorageUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    List<String> mList;
    String outputPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StorageUtil.init(this, null);//初始化监测sdk
        mList = new ArrayList<>();
        mList.add((mList.size() + 1) + ".  选择单张图片");
        mList.add((mList.size() + 1) + ".  选择单张图片并裁剪");
        mList.add((mList.size() + 1) + ".  选择多张图片");
        mList.add((mList.size() + 1) + ".  选择视频");
        mList.add((mList.size() + 1) + ".  拍摄视频");
        mList.add((mList.size() + 1) + ".  拍照片");
        mList.add((mList.size() + 1) + ".  拍照片并裁剪");
        ListView listView = (ListView) findViewById(R.id.ls_home);

        listView.setAdapter(new MyAdapter(this));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: /*** 选择单张图片 onActivityResult{@link GalleryActivity.PHOTOS}*/
                        GalleryHelper.with(MainActivity.this).type(GalleryConfig.SELECT_PHOTO).requestCode(12).singlePhoto().execute();
                        break;
                    case 1:  /***选择单张图片并裁剪 onActivityResult{@link GalleryActivity.CROP}*/
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
                        GalleryHelper.with(MainActivity.this).type(GalleryConfig.RECORD_VEDIO).requestCode(12).execute();
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

            } else if (data.getStringExtra(GalleryActivity.CROP) != null) {//裁剪回来
                if (outputPath == null) {//没有传入返回裁剪路径

                    byte[] datas = data.getByteArrayExtra(GalleryActivity.CROP);
                    Toast.makeText(MainActivity.this, datas.toString(), Toast.LENGTH_SHORT).show();

                } else {//传入返回裁剪路径
                    String path = data.getStringExtra(GalleryActivity.CROP);
                    Toast.makeText(MainActivity.this, path.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }

    }
}
