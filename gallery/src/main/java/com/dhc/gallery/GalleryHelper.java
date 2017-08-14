package com.dhc.gallery;

import android.app.Activity;
import android.app.Fragment;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.dhc.gallery.ui.GalleryActivity;

import static android.os.Environment.MEDIA_MOUNTED;


/**
 * 创建者     邓浩宸
 * 创建时间   2017/4/17 15:52
 * 描述 <p>
 *   <p> 1, 选择单张图片   返回 String  path
 *   <p> 2, 选择多张图片  返回 List<String>
 *   <p> 3, 选择单张图片并裁剪  如果有输入path路径 返回 String path  否则返回 byte [] data 数据
 *   <p>  4, 拍照  返回 String  path
 *   <p> 5, 拍照并裁剪  如果有输入path路径 返回 String path  否则返回 byte [] data 数据
 *   <p> 6, 摄影  返回 String  path
 *   <p> 7, 选着视频 返回 String  path
 */
public class GalleryHelper {


    public GalleryHelper() {
    }
    private Activity mActivity;
    private Fragment mFragment;
    private android.support.v4.app.Fragment mV4Fragment;


    GalleryConfig configuration = new GalleryConfig();
    static GalleryHelper instance;

    /**
     * 开启页面
     * @param mActivity
     * @return
     */
    public static GalleryHelper with(Activity mActivity) {
        instance = new GalleryHelper();
        instance.mActivity=mActivity;
        return instance;
    }

    /**
     * 开启页面
     * @param mFragment
     * @return
     */
    public static GalleryHelper with(Fragment mFragment ){
        instance = new GalleryHelper();
        instance.mFragment=mFragment;
        return instance;
    }
    /**
     * 开启页面
     * @param mV4Fragment
     * @return GalleryHelper
     */
    public static GalleryHelper with(android.support.v4.app.Fragment mV4Fragment ){
        instance = new GalleryHelper();
        instance.mV4Fragment=mV4Fragment;
        return instance;
    }
    /**
     *
     * 选择的类型
     * @param type
     * {@link GalleryConfig.SELECT_PHOTO }
     * {@link GalleryConfig.TAKE_PHOTO }
     * {@link GalleryConfig.RECORD_VEDIO }
     * {@link GalleryConfig.SELECT_VEDIO }
     * {@link GalleryConfig.TAKEPHOTO_RECORDVEDIO }
     * @return GalleryHelper
     */
    public GalleryHelper type(int type) {
        configuration.setType(type);
        return this;
    }

    /**
     * 响应码
     * @param type
     * @return GalleryHelper
     */
    public GalleryHelper requestCode(int type) {
        configuration.setRequestCode(type);
        return this;
    }

    /**
     * 限定视频录制时间
     * @param limitRecordTime
     * @return GalleryHelper
     */
    public GalleryHelper limitRecordTime(int limitRecordTime) {
        configuration.setLimitRecordTime(limitRecordTime);
        return this;
    }
    /**
     * 限定视频录制大小
     * @param limitRecordSize
     * @return GalleryHelper
     */
    public GalleryHelper limitRecordSize(int limitRecordSize) {
        configuration.setLimitRecordSize(limitRecordSize);
        return this;
    }

    /**
     * 单选视频
     * @return
     */
    public GalleryHelper isSingleVedio() {
        configuration.setSingleVedio(true);
        configuration.setFilterMimeTypes( new String[]{
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.MIME_TYPE
        });
        return this;
    }

    /**
     * 需要裁剪图片 这将返回一个  byte[] data数据类型
     * @return
     */
    public GalleryHelper isNeedCrop() {
        configuration.setNeedCrop(true);
        return this;
    }

    /**
     * 进行裁剪图片  返回一个传入路径值
     * @param filePath
     * @return
     */
    public GalleryHelper isNeedCropWithPath(String filePath) {
        configuration.setNeedCrop(true);
        configuration.setFilePath(filePath);
        return this;
    }

    /**
     * @param filterMimeTypes filter of media type， based on MimeType standards：
     *                        {http://www.w3school.com.cn/media/media_mimeref.asp}
     *                        <Li>eg:new string[]{"image/gif","image/jpeg"}
     */
    public GalleryHelper selectVedioWithMimeTypes(String[] filterMimeTypes) {
        configuration.setSingleVedio(true);
        configuration.setFilterMimeTypes(filterMimeTypes);
        return this;
    }

    /**
     * @param hintOfPick hint of Toast when limit is reached
     */
    public GalleryHelper hintOfPick(String hintOfPick) {
        configuration.setHintOfPick(hintOfPick);
        return this;
    }

    /**
     * 选择单张照片
     * @return
     */
    public GalleryHelper singlePhoto() {
        configuration.setSinglePhoto(true);
        configuration.setLimitPickPhoto(1);
        return this;
    }

    /**
     * @param limitPickPhoto the limit of photos those can be selected
     */
    public GalleryHelper limitPickPhoto(int limitPickPhoto) {
        configuration.setLimitPickPhoto(limitPickPhoto);
        return this;
    }


    public void execute() {
        if(mActivity == null&&mFragment==null&&mV4Fragment==null) {
            return;
        }
        if(!existSDcard()){
            Toast.makeText(mActivity, "没有找到SD卡", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mActivity!=null){
            GalleryActivity.openActivity(mActivity,configuration.getRequestCode(),configuration);
        }else  if(mFragment!=null){
            GalleryActivity.openActivity(mFragment,configuration.getRequestCode(),configuration);
        }else if(mV4Fragment!=null){
            GalleryActivity.openActivity(mV4Fragment,configuration.getRequestCode(),configuration);
        }
    }
    private   boolean existSDcard() {
        String externalStorageState;
        try {
            externalStorageState = Environment.getExternalStorageState();
        } catch (NullPointerException e) { // (sh)it happens (Issue #660)
            externalStorageState = "";
        } catch (IncompatibleClassChangeError e) { // (sh)it happens too (Issue #989)
            externalStorageState = "";
        }
        return MEDIA_MOUNTED.equals(externalStorageState);
    }
}
