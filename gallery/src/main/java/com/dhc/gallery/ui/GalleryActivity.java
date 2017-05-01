
package com.dhc.gallery.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.dhc.gallery.GalleryConfig;
import com.dhc.gallery.R;
import com.dhc.gallery.actionbar.ActionBarLayout;
import com.dhc.gallery.actionbar.BaseFragment;
import com.dhc.gallery.proxy.PhotoViewer;
import com.dhc.gallery.utils.Gallery;
import com.dhc.gallery.utils.ImageLoader;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * receive {@link java.util.List<String>} of photo path or video path by
 * {@link GalleryActivity#PHOTOS} or {@link GalleryActivity#VIDEO} in
 * {@link Activity#onActivityResult}
 */
public class GalleryActivity extends Activity implements ActionBarLayout.ActionBarLayoutDelegate, PhotoCropActivity.PhotoEditActivityDelegate, CameraActivity.CameraActivityDelegate {

    public static final String PHOTOS = "PHOTOS";
    public static final String VIDEO = "VIDEOS";
    public static final String DATA = "DATA";

    public static final String GALLERY_CONFIG = "GALLERY_CONFIG";

    private ArrayList<BaseFragment> mainFragmentsStack = new ArrayList<>();
    private ActionBarLayout actionBarLayout;
    private PhotoAlbumPickerActivity albumPickerActivity;
    private GalleryConfig config;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            config = intent.getParcelableExtra(GALLERY_CONFIG);
        } else {
            config = savedInstanceState.getParcelable(GALLERY_CONFIG);
        }
        Gallery.init(getApplication());
        FrameLayout mian = (FrameLayout) findViewById(R.id.mian);
        actionBarLayout = new ActionBarLayout(this);
        mian.addView(actionBarLayout);
        actionBarLayout.init(mainFragmentsStack);
        actionBarLayout.setDelegate(this);
        String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
        if (checkCallingOrSelfPermission(
                READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{
                        READ_EXTERNAL_STORAGE
                }, 1);
                return;
            }
        }
        showContent(config);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        showContent(config);
    }

    private void showContent(GalleryConfig config) {
        if (config.getType() == GalleryConfig.RECORD_VEDIO
                || config.getType() == GalleryConfig.TAKE_PHOTO
                || config.getType() == GalleryConfig.TAKEPHOTO_RECORDVEDIO) {
            CameraActivity cameraActivity = new CameraActivity(getIntent().getExtras());
            actionBarLayout.presentFragment(cameraActivity, false, true, true);
            cameraActivity.setDelegate(this);
        } else if (config.getType() == GalleryConfig.SELECT_PHOTO
                || config.getType() == GalleryConfig.SELECT_VEDIO) {
            albumPickerActivity = new PhotoAlbumPickerActivity(
                    this.config.getFilterMimeTypes(),
                    this.config.getLimitPickPhoto(),
                    this.config.isSinglePhoto(),
                    this.config.getHintOfPick(),
                    false);
            albumPickerActivity.setDelegate(mPhotoAlbumPickerActivityDelegate);
            actionBarLayout.presentFragment(albumPickerActivity, false, true, true);
        }
    }

    private PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate mPhotoAlbumPickerActivityDelegate = new PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate() {
        @Override
        public void didSelectPhotos(ArrayList<String> photos, ArrayList<String> captions) {
            if (photos == null || photos.size() <= 0) {
                return;
            }
            if (config != null && config.isNeedCrop() && config.isSinglePhoto()) {
                startCrop(photos.get(0), null);
            } else {
                Intent intent = new Intent();
                intent.putExtra(PHOTOS, photos);
                setResult(Activity.RESULT_OK, intent);
            }

        }

        @Override
        public boolean didSelectVideo(String path) {
            Intent intent = new Intent();
            intent.putExtra(VIDEO, path);
            setResult(Activity.RESULT_OK, intent);
            return true;
        }

        @Override
        public void startPhotoSelectActivity() {
        }
    };

    private void startCrop(String path, Uri uri) {
        try {
            Bundle args = new Bundle();
            if (path != null) {
                args.putString("photoPath", path);
            } else if (uri != null) {
                args.putParcelable("photoUri", uri);
            }
            PhotoCropActivity photoCropActivity = new PhotoCropActivity(args);
            photoCropActivity.setDelegate(this);
            actionBarLayout.presentFragment(photoCropActivity, true);
        } catch (Exception e) {
            Bitmap bitmap = ImageLoader.loadBitmap(path, uri, 800, 800, true);
            didFinishEdit(bitmap);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (config != null)
            outState.putParcelable(GALLERY_CONFIG, config);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        actionBarLayout.onLowMemory();
    }

    @Override
    public void onBackPressed() {
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
        } else {
            actionBarLayout.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        actionBarLayout.onPause();
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        actionBarLayout.onResume();
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().onResume();
        }
    }

    @Override
    public boolean onPreIme() {
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean needPresentFragment(BaseFragment fragment, boolean removeLast,
                                       boolean forceWithoutAnimation, ActionBarLayout layout) {
        return true;
    }

    @Override
    public boolean needAddFragmentToStack(BaseFragment fragment, ActionBarLayout layout) {
        return true;
    }

    @Override
    public boolean needCloseLastFragment(ActionBarLayout layout) {
        if (layout.fragmentsStack.size() <= 1) {
            finish();
            return false;
        }
        return true;
    }

    @Override
    public void onRebuildAllFragments(ActionBarLayout layout) {
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            actionBarLayout.onKeyUp(keyCode, event);
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        PhotoViewer.getInstance().destroyPhotoViewer();
        ImageLoader.getInstance().clearMemory();
        if (albumPickerActivity != null)
            albumPickerActivity.removeSelfFromStack();
        actionBarLayout.clear();
        mainFragmentsStack.clear();
        mainFragmentsStack = null;
        actionBarLayout = null;
        albumPickerActivity = null;
        config = null;
        super.onDestroy();
    }

    /**
     * open gallery
     *  @param activity    parent activity
     * @param requestCode {@link Activity#onActivityResult}
     * @param config      {@link GalleryConfig}
     */
    public static void openActivity(Activity activity, int requestCode, GalleryConfig config) {
        Intent intent = new Intent(activity, GalleryActivity.class);
        intent.putExtra(GALLERY_CONFIG, config);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * open gallery
     *
     * @param activity    parent activity
     * @param requestCode {@link Activity#onActivityResult}
     * @param config      {@link GalleryConfig}
     */
    public static void openActivity(Fragment activity, int requestCode, GalleryConfig config) {
        Intent intent = new Intent(activity.getActivity(), GalleryActivity.class);
        intent.putExtra(GALLERY_CONFIG, config);
        activity.startActivityForResult(intent, requestCode);
    }
    /**
     * open gallery
     *
     * @param fragment    parent activity
     * @param requestCode {@link Activity#onActivityResult}
     * @param config      {@link GalleryConfig}
     */
    public static void openActivity(android.support.v4.app.Fragment fragment, int requestCode, GalleryConfig config) {
        Intent intent = new Intent(fragment.getActivity(), GalleryActivity.class);
        intent.putExtra(GALLERY_CONFIG, config);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 裁剪后返回的数据
     *
     * @param bitmap
     */
    @Override
    public void didFinishEdit(Bitmap bitmap) {
        if (saveBitmap(bitmap, config.getFilePath(), true)) {
            Intent intent = new Intent();
            ArrayList<String> paths=new ArrayList<>();
            paths.add(config.getFilePath());
            intent.putExtra(PHOTOS, paths);
            setResult(Activity.RESULT_OK, intent);
        } else {
            Intent intent = new Intent();
            intent.putExtra(DATA, getCroppedImage(bitmap));
            setResult(Activity.RESULT_OK, intent);

        }
    }


    /**
     * 拍照返回路径
     *
     * @param absolutePath
     */
    @Override
    public void didCameraPhoto(String absolutePath) {
        if (absolutePath == null) {
            return;
        }
        if (config != null && config.isNeedCrop()
                && config.getType() == GalleryConfig.TAKE_PHOTO) {
            startCrop(absolutePath, null);
        } else {
            ArrayList<String> paths=new ArrayList<>();
            paths.add(absolutePath);
            Intent intent = new Intent();
            intent.putStringArrayListExtra(PHOTOS, paths);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }

    }

    @Override
    public void didVedioOver(String absolutePath) {
        if (absolutePath == null) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(VIDEO, absolutePath);
        setResult(Activity.RESULT_OK, intent);
        finish();

    }


    public byte[] getCroppedImage(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] data = stream.toByteArray();
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }


    public boolean saveBitmap(Bitmap bitmap, String path, boolean recyle) {
        if (bitmap == null || TextUtils.isEmpty(path)) {
            return false;
        }

        BufferedOutputStream bos = null;
        try {
            FileOutputStream fos = new FileOutputStream(path);
            bos = new BufferedOutputStream(fos);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            return true;

        } catch (FileNotFoundException e) {
            return false;
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
            if (recyle) {
                bitmap.recycle();
            }
        }
    }


}
