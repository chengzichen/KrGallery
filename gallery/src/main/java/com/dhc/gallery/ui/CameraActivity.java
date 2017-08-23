package com.dhc.gallery.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.dhc.gallery.GalleryConfig;
import com.dhc.gallery.R;
import com.dhc.gallery.actionbar.BaseFragment;
import com.dhc.gallery.actionbar.ShutterButton;
import com.dhc.gallery.camera.CameraController;
import com.dhc.gallery.camera.CameraView;
import com.dhc.gallery.components.BaseDialog;
import com.dhc.gallery.proxy.PhotoViewer;
import com.dhc.gallery.utils.AndroidUtilities;
import com.dhc.gallery.utils.LayoutHelper;
import com.dhc.gallery.utils.MediaController;
import com.dhc.gallery.utils.NotificationCenter;

import java.io.File;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;
import static com.dhc.gallery.ui.GalleryActivity.GALLERY_CONFIG;

/**
 * 创建者     邓浩宸
 * 创建时间   2017/4/7 15:13
 * 描述	      ${TODO}
 */
public class CameraActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private CameraActivityDelegate delegate;
    private CameraView cameraView;
    private TextView recordTime;
    private ImageView[] flashModeButton = new ImageView[2];
    private boolean flashAnimationInProgress;
    private boolean cameraOpened;
    private int videoRecordTime;
    private Runnable videoRecordRunnable;
    private FrameLayout cameraPanel;
    private ShutterButton shutterButton;
    private ImageView switchCameraButton;
    private File cameraFile;
    private boolean takingPhoto;
    private boolean mediaCaptured;
    private ArrayList<Object> cameraPhoto;
    private boolean deviceHasGoodCamera;
    private boolean paused;
    private boolean requestingPermissions;
    private boolean cameraAnimationInProgress;
    private String initRecordTimes;
    private boolean pressed;
    private boolean maybeStartDraging;
    private float lastY;
    private boolean dragging;
    private int[] viewPosition = new int[2];
    private DecelerateInterpolator interpolator = new DecelerateInterpolator(1.5f);
    private GalleryConfig mGalleryConfig;
    private BaseDialog mBaseDialog;
    private File mFile;

    public interface CameraActivityDelegate {

        void didCameraPhoto(String absolutePath);

        void didVedioOver(String absolutePath);
    }


    public CameraActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        mGalleryConfig = getArguments().getParcelable(GALLERY_CONFIG);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.cameraInitied);
        if (!deviceHasGoodCamera || cameraView == null) {
            return;
        }
        if (cameraAnimationInProgress) {
            return;
        }
        if (cameraOpened) {
            closeCamera(true);
        }
        hideCamera(true);
        if (cameraView != null) {
            cameraView.destroy(true, null);
            FrameLayout frameLayout = (FrameLayout) fragmentView;
            frameLayout.removeView(cameraView);
            cameraView = null;
        }
        fragmentView = null;
        if (mBaseDialog != null)
            mBaseDialog.dismiss();
        mBaseDialog = null;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.cameraInitied) {
            checkCamera(true);
            cameraPanel.bringToFront();
            recordTime.bringToFront();
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public View createView(Context context) {
        actionBar.setVisibility(View.GONE);
        fragmentView = new FrameLayout(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (cameraOpened && processTouchEvent(event)) {
                    return true;
                } else {
                    return super.onTouchEvent(event);
                }
            }
        };
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xff000000);
        frameLayout.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        if (Build.VERSION.SDK_INT >= 21) {
            frameLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        initView(context);
        return fragmentView;
    }

    private void initView(final Context context) {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.cameraInitied);
        checkCamera(true);
        if (deviceHasGoodCamera) {
            CameraController.getInstance().initCamera();
        }
        initRecordTimes = mGalleryConfig.getLimitRecordTime() == 0 ? "00:00" : String.format("%02d:%02d", mGalleryConfig.getLimitRecordTime() / 60, mGalleryConfig.getLimitRecordTime() % 60);

        if (Build.VERSION.SDK_INT >= 16) {
            recordTime = new TextView(context);
            recordTime.setBackgroundResource(R.drawable.system);
            recordTime.getBackground().setColorFilter(new PorterDuffColorFilter(0x66000000, PorterDuff.Mode.MULTIPLY));
            recordTime.setText(initRecordTimes);
            recordTime.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            recordTime.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            recordTime.setAlpha(0.0f);
            recordTime.setTextColor(0xffffffff);
            recordTime.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(5), AndroidUtilities.dp(10), AndroidUtilities.dp(5));
            FrameLayout frameLayout = (FrameLayout) fragmentView;
            frameLayout.addView(recordTime, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 16, 0, 0));

            cameraPanel = new FrameLayout(context) {
                @Override
                protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                    int cx = getMeasuredWidth() / 2;
                    int cy = getMeasuredHeight() / 2;
                    int cx2;
                    int cy2;
                    shutterButton.layout(cx - shutterButton.getMeasuredWidth() / 2, cy - shutterButton.getMeasuredHeight() / 2, cx + shutterButton.getMeasuredWidth() / 2, cy + shutterButton.getMeasuredHeight() / 2);
                    if (getMeasuredWidth() == AndroidUtilities.dp(100)) {
                        cx = cx2 = getMeasuredWidth() / 2;
                        cy2 = cy + cy / 2 + AndroidUtilities.dp(17);
                        cy = cy / 2 - AndroidUtilities.dp(17);
                    } else {
                        cx2 = cx + cx / 2 + AndroidUtilities.dp(17);
                        cx = cx / 2 - AndroidUtilities.dp(17);
                        cy = cy2 = getMeasuredHeight() / 2;
                    }
                    switchCameraButton.layout(cx2 - switchCameraButton.getMeasuredWidth() / 2, cy2 - switchCameraButton.getMeasuredHeight() / 2, cx2 + switchCameraButton.getMeasuredWidth() / 2, cy2 + switchCameraButton.getMeasuredHeight() / 2);
                    for (int a = 0; a < 2; a++) {
                        flashModeButton[a].layout(cx - flashModeButton[a].getMeasuredWidth() / 2, cy - flashModeButton[a].getMeasuredHeight() / 2, cx + flashModeButton[a].getMeasuredWidth() / 2, cy + flashModeButton[a].getMeasuredHeight() / 2);
                    }
                }


            };
            frameLayout.addView(cameraPanel, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 100, Gravity.LEFT | Gravity.BOTTOM));

            shutterButton = new ShutterButton(context, mGalleryConfig.getType());
            cameraPanel.addView(shutterButton, LayoutHelper.createFrame(84, 84, Gravity.CENTER));
            shutterButton.setDelegate(new ShutterButton.ShutterButtonDelegate() {
                @Override
                public boolean shutterLongPressed() {
                    if (mediaCaptured || takingPhoto || CameraActivity.this.getParentActivity() == null || cameraView == null) {
                        return false;
                    }
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (CameraActivity.this.getParentActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            requestingPermissions = true;
                            CameraActivity.this.getParentActivity().requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 21);
                            return false;
                        }
                    }
                    for (int a = 0; a < 2; a++) {
                        flashModeButton[a].setAlpha(0.0f);
                        flashModeButton[a].setVisibility(View.GONE);
                    }
                    switchCameraButton.setAlpha(0.0f);
                    switchCameraButton.setVisibility(View.GONE);
                    cameraFile = AndroidUtilities.generateVideoPath();
                    recordTime.setAlpha(1.0f);
                    recordTime.setText(initRecordTimes);
                    videoRecordTime = mGalleryConfig.getLimitRecordTime();
                    videoRecordRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (videoRecordRunnable == null) {
                                return;
                            }
                            if (mGalleryConfig.getLimitRecordTime() == 0) {
                                videoRecordTime++;

                            } else {
                                videoRecordTime--;
                                if (videoRecordTime == 0) {
                                    if (vedioReleased())
                                        return;
                                }
                            }
                            if (mGalleryConfig.getLimitRecordSize() != 0) {
                                int b = getSize(cameraFile);
                                if (b > 0 && b >= mGalleryConfig.getLimitRecordSize() * 1024 * 1024) {
                                    if (vedioReleased())
                                        return;
                                }
                            }
                            //TODO 录制事件的限制
                            recordTime.setText(String.format("%02d:%02d", videoRecordTime / 60, videoRecordTime % 60));
                            AndroidUtilities.runOnUIThread(videoRecordRunnable, 1000);
                        }
                    };
                    AndroidUtilities.lockOrientation(CameraActivity.this.getParentActivity());
                    CameraController.getInstance().recordVideo(cameraView.getCameraSession(), cameraFile, new CameraController.VideoTakeCallback() {
                        @Override
                        public void onFinishVideoRecording(final Bitmap thumb) {
                            if (cameraFile == null || CameraActivity.this.getParentActivity() == null) {
                                return;
                            }
                            //                            PhotoViewer.getInstance().setParentActivity(CameraActivity.this.getParentActivity());
                            //                            PhotoViewer.getInstance().setParentAlert(CameraActivity.this);
                            cameraPhoto = new ArrayList<>();
                            cameraPhoto.add(new MediaController.PhotoEntry(0, 0, 0, cameraFile.getAbsolutePath(), 0, true));
                            //                                                        Log.d("VV", "录制结束");

                            String msg = getVedioSize(cameraFile);
                            mBaseDialog = getDialog(CameraActivity.this.getParentActivity(), msg, new PhotoViewer.EmptyPhotoViewerProvider() {


                                @TargetApi(16)
                                @Override
                                public boolean cancelButtonPressed() {
                                    if (cameraOpened && cameraView != null && cameraFile != null) {
                                        cameraFile.delete();
                                        AndroidUtilities.runOnUIThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (cameraView != null && Build.VERSION.SDK_INT >= 21) {
                                                    cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
                                                }
                                            }
                                        }, 1000);
                                        CameraController.getInstance().startPreview(cameraView.getCameraSession());
                                        mBaseDialog.dismiss();
                                        cameraFile = null;
                                    }
                                    return true;
                                }

                                @Override
                                public void sendButtonPressed(int index) {
                                    if (cameraFile == null) {
                                        return;
                                    }
                                    AndroidUtilities.addMediaToGallery(cameraFile.getAbsolutePath());
                                    if (delegate != null)
                                        delegate.didVedioOver(cameraFile.getAbsolutePath());
                                    closeCamera(false);
                                    mBaseDialog.dismiss();
                                    cameraFile = null;
                                    removeSelfFromStack();
                                }

                            });
                            mBaseDialog.show();

                            mediaCaptured = false;
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            AndroidUtilities.runOnUIThread(videoRecordRunnable, 1000);
                        }
                    }, false);
                    shutterButton.setState(ShutterButton.State.RECORDING, true);
                    return true;
                }

                @Override
                public void shutterCancel() {
                    if (mediaCaptured) {
                        return;
                    }
                    cameraFile.delete();
                    resetRecordState();
                    CameraController.getInstance().stopVideoRecording(cameraView.getCameraSession(), true);
                }

                @Override
                public void shutterReleased() {
                    if (vedioReleased())
                        return;
                    cameraFile = AndroidUtilities.generatePicturePath();
                    //                                        final boolean sameTakePictureOrientation = cameraView.getCameraSession().isSameTakePictureOrientation();
                    takingPhoto = CameraController.getInstance().takePicture(cameraFile, cameraView.getCameraSession(), new Runnable() {
                        @Override
                        public void run() {
                            takingPhoto = false;
                            if (cameraFile == null) {
                                return;
                            }
                            PhotoViewer.getInstance().setParentActivity(CameraActivity.this.getParentActivity());
                            cameraPhoto = new ArrayList<>();
                            int orientation = 0;
                            try {
                                ExifInterface ei = new ExifInterface(cameraFile.getAbsolutePath());
                                int exif = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                switch (exif) {
                                    case ExifInterface.ORIENTATION_ROTATE_90:
                                        orientation = 90;
                                        break;
                                    case ExifInterface.ORIENTATION_ROTATE_180:
                                        orientation = 180;
                                        break;
                                    case ExifInterface.ORIENTATION_ROTATE_270:
                                        orientation = 270;
                                        break;
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "run "+e);
                            }
                            cameraPhoto.add(new MediaController.PhotoEntry(0, 0, 0, cameraFile.getAbsolutePath(), orientation, false));
                            PhotoViewer.getInstance().openPhotoForSelect(cameraPhoto, false, 0, 1,
                                    new PhotoViewer.EmptyPhotoViewerProvider() {
                                        @Override
                                        public boolean cancelButtonPressed() {
                                            if (cameraOpened && cameraView != null && cameraFile != null) {
                                                cameraFile.delete();
                                                AndroidUtilities.runOnUIThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (cameraView != null && Build.VERSION.SDK_INT >= 21) {
                                                            cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
                                                        }
                                                    }
                                                }, 1000);
                                                CameraController.getInstance().startPreview(cameraView.getCameraSession());
                                                cameraFile = null;
                                            }
                                            return true;
                                        }

                                        @Override
                                        public boolean isSinglePhoto() {
                                            return true;
                                        }

                                        @Override
                                        public void sendButtonPressed(int index) {
                                            if (cameraFile == null) {
                                                return;
                                            }
                                            AndroidUtilities.addMediaToGallery(cameraFile.getAbsolutePath());
                                            if (delegate != null)
                                                delegate.didCameraPhoto(cameraFile.getAbsolutePath());
                                            closeCamera(false);
                                            cameraFile = null;
                                            //                                            getParentActivity().finish();
                                            removeSelfFromStack();
                                        }

                                        @Override
                                        public void willHidePhotoViewer() {
                                            mediaCaptured = false;
                                        }

                                    });
                        }
                    });
                }

                @Override
                public void shutterLongPressedReleased() {
                    if (vedioReleased())
                        return;
                }
            });

            switchCameraButton = new ImageView(context);
            switchCameraButton.setScaleType(ImageView.ScaleType.CENTER);
            cameraPanel.addView(switchCameraButton, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
            switchCameraButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (takingPhoto || cameraView == null || !cameraView.isInitied()) {
                        return;
                    }
                    //                    cameraInitied = false;
                    cameraView.switchCamera();
                    ObjectAnimator animator = ObjectAnimator.ofFloat(switchCameraButton, "scaleX", 0.0f).setDuration(100);
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            switchCameraButton.setImageResource(cameraView.isFrontface() ? R.drawable.camera_revert1 : R.drawable.camera_revert2);
                            ObjectAnimator.ofFloat(switchCameraButton, "scaleX", 1.0f).setDuration(100).start();
                        }
                    });
                    animator.start();
                }
            });

            for (int a = 0; a < 2; a++) {
                flashModeButton[a] = new ImageView(context);
                flashModeButton[a].setScaleType(ImageView.ScaleType.CENTER);
                //                flashModeButton[a].setVisibility(View.INVISIBLE);
                cameraPanel.addView(flashModeButton[a], LayoutHelper.createFrame(48, 48, Gravity.LEFT | Gravity.TOP));
                flashModeButton[a].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View currentImage) {
                        if (flashAnimationInProgress || cameraView == null || !cameraView.isInitied() || !cameraOpened) {
                            return;
                        }
                        String current = cameraView.getCameraSession().getCurrentFlashMode();
                        String next = cameraView.getCameraSession().getNextFlashMode();
                        if (current.equals(next)) {
                            return;
                        }
                        cameraView.getCameraSession().setCurrentFlashMode(next);
                        flashAnimationInProgress = true;
                        ImageView nextImage = flashModeButton[0] == currentImage ? flashModeButton[1] : flashModeButton[0];
                        nextImage.setVisibility(View.VISIBLE);
                        setCameraFlashModeIcon(nextImage, next);
                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.playTogether(
                                ObjectAnimator.ofFloat(currentImage, "translationY", 0, AndroidUtilities.dp(48)),
                                ObjectAnimator.ofFloat(nextImage, "translationY", -AndroidUtilities.dp(48), 0),
                                ObjectAnimator.ofFloat(currentImage, "alpha", 1.0f, 0.0f),
                                ObjectAnimator.ofFloat(nextImage, "alpha", 0.0f, 1.0f));
                        animatorSet.setDuration(200);
                        animatorSet.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animator) {
                                flashAnimationInProgress = false;
                                currentImage.setVisibility(View.INVISIBLE);
                            }
                        });
                        animatorSet.start();
                    }
                });
            }
        }
    }

    private String getVedioSize(File convertedFile) {
        String message = "";
        if (convertedFile.exists() && convertedFile.length() != 0 && CameraActivity.this.getParentActivity() != null) {
            int b = (int) convertedFile.length();
            int kb = b / 1024;
            float mb = kb / 1024f;
            if (b > mGalleryConfig.getLimitRecordSize() * 1024 * 1024) {
                message += mb > 1 ? CameraActivity.this.getParentActivity().getString(R.string.over_video_size_in_mb, mb)
                        : CameraActivity.this.getParentActivity().getString(R.string.over_video_size_in_kb, kb);
            } else {
                message += mb > 1 ? CameraActivity.this.getParentActivity().getString(R.string.capture_video_size_in_mb, mb)
                        : CameraActivity.this.getParentActivity().getString(R.string.capture_video_size_in_kb, kb);
            }

            message += CameraActivity.this.getParentActivity().getString(R.string.is_send_video);
        }
        return message;
    }

    private int getSize(File convertedFile) {
        if (!convertedFile.exists() && convertedFile.length() == 0)
            return 0;
        int b = (int) convertedFile.length();
        return b;
    }

    private BaseDialog getDialog(Context context, String msg, final PhotoViewer.EmptyPhotoViewerProvider emptyPhotoViewerProvider) {
        BaseDialog baseDialog = new BaseDialog(context)
                .setCustomerContent(R.layout.dialog_layout)
                .setViewOnClickListener(R.id.dialog_cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        emptyPhotoViewerProvider.cancelButtonPressed();
                    }
                }).setViewOnClickListener(R.id.dialog_sure, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        emptyPhotoViewerProvider.sendButtonPressed(0);
                    }
                }).setText(R.id.dialog_text, msg);

        baseDialog.setCancelable(false);//设置触摸外界可以取消
        return baseDialog;
    }

    private boolean vedioReleased() {
        if (takingPhoto || cameraView == null || mediaCaptured) {
            return true;
        }
        mediaCaptured = true;
        if (shutterButton.getState() == ShutterButton.State.RECORDING) {
            resetRecordState();
            CameraController.getInstance().stopVideoRecording(cameraView.getCameraSession(), false);
            shutterButton.setState(ShutterButton.State.DEFAULT, true);
            return true;
        }
        return false;
    }


    @Override
    public void onPause() {
        super.onPause();
        if (shutterButton == null) {
            return;
        }
        if (!requestingPermissions) {
            if (cameraView != null && shutterButton.getState() == ShutterButton.State.RECORDING) {
                resetRecordState();
                CameraController.getInstance().stopVideoRecording(cameraView.getCameraSession(), false);
                shutterButton.setState(ShutterButton.State.DEFAULT, true);
            }
            if (cameraOpened) {
                hideCamera(true);
            }
            //                closeCamera(false);
        } else {
            if (cameraView != null && shutterButton.getState() == ShutterButton.State.RECORDING) {
                shutterButton.setState(ShutterButton.State.DEFAULT, true);
            }
            requestingPermissions = false;
        }
        paused = true;
    }

    @Override
    public void onResume() {
        if (paused) {
            if (mGalleryConfig == null)
                mGalleryConfig = getArguments().getParcelable(GALLERY_CONFIG);
            checkCamera(true);
            cameraPanel.bringToFront();
            recordTime.bringToFront();
        }
        paused = false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void checkCamera(boolean request) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (request) {
                    this.getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA}, 17);
                }
                deviceHasGoodCamera = false;
            } else {
                CameraController.getInstance().initCamera();
                deviceHasGoodCamera = CameraController.getInstance().isCameraInitied();
            }
        } else if (Build.VERSION.SDK_INT >= 16) {
            CameraController.getInstance().initCamera();
            deviceHasGoodCamera = CameraController.getInstance().isCameraInitied();
        }
        if (deviceHasGoodCamera && !cameraOpened) {
            cameraOpened = true;
            showCamera();
        }
    }

    @TargetApi(16)
    public void showCamera() {
        if (cameraView == null) {
            cameraView = new CameraView(CameraActivity.this.getParentActivity(), false);
            FrameLayout frameLayout = (FrameLayout) fragmentView;
            frameLayout.addView(cameraView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            cameraView.setDelegate(new CameraView.CameraViewDelegate() {
                @Override
                public void onCameraCreated(Camera camera) {

                }

                @Override
                public void onCameraInit() {
                    if (cameraPanel != null) {
                        cameraPanel.setAlpha(1);
                        cameraPanel.setVisibility(View.VISIBLE);
                    }
                    String current = cameraView.getCameraSession().getCurrentFlashMode();
                    String next = cameraView.getCameraSession().getNextFlashMode();
                    if (current.equals(next)) {
                        for (int a = 0; a < 2; a++) {
                            flashModeButton[a].setVisibility(View.INVISIBLE);
                            flashModeButton[a].setAlpha(0.0f);
                            flashModeButton[a].setTranslationY(0.0f);
                        }
                    } else {
                        setCameraFlashModeIcon(flashModeButton[0], cameraView.getCameraSession().getCurrentFlashMode());
                        for (int a = 0; a < 2; a++) {
                            flashModeButton[a].setVisibility(a == 0 ? View.VISIBLE : View.INVISIBLE);
                            flashModeButton[a].setAlpha(a == 0 && cameraOpened ? 1.0f : 0.0f);
                            flashModeButton[a].setTranslationY(0.0f);
                        }
                    }
                    if (switchCameraButton != null) {
                        switchCameraButton.setImageResource(cameraView.isFrontface() ? R.drawable.camera_revert1 : R.drawable.camera_revert2);
                        switchCameraButton.setVisibility(cameraView.hasFrontFaceCamera() ? View.VISIBLE : View.INVISIBLE);
                    }
                }
            });
        }
    }


    public void hideCamera(boolean async) {
        if (!deviceHasGoodCamera || cameraView == null) {
            return;
        }
        cameraView.destroy(async, null);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.removeView(cameraView);
        cameraView = null;
        cameraOpened = false;
    }


    private void setCameraFlashModeIcon(ImageView imageView, String mode) {
        switch (mode) {
            case Camera.Parameters.FLASH_MODE_OFF:
                imageView.setImageResource(R.drawable.flash_off);
                break;
            case Camera.Parameters.FLASH_MODE_ON:
                imageView.setImageResource(R.drawable.flash_on);
                break;
            case Camera.Parameters.FLASH_MODE_AUTO:
                imageView.setImageResource(R.drawable.flash_auto);
                break;
        }
    }


    private boolean processTouchEvent(MotionEvent event) {
        if (!pressed && event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            if (!takingPhoto) {
                pressed = true;
                maybeStartDraging = true;
                lastY = event.getY();
            }
        } else if (pressed) {
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
                pressed = false;
                if (dragging) {
                    dragging = false;
                    if (cameraView != null) {
                        if (Math.abs(cameraView.getTranslationY()) > cameraView.getMeasuredHeight() / 6.0f) {
                            closeCamera(true);
                        } else {
                            AnimatorSet animatorSet = new AnimatorSet();
                            animatorSet.playTogether(
                                    ObjectAnimator.ofFloat(cameraView, "translationY", 0.0f),
                                    ObjectAnimator.ofFloat(cameraPanel, "alpha", 1.0f),
                                    ObjectAnimator.ofFloat(flashModeButton[0], "alpha", 1.0f),
                                    ObjectAnimator.ofFloat(flashModeButton[1], "alpha", 1.0f));
                            animatorSet.setDuration(250);
                            animatorSet.setInterpolator(interpolator);
                            animatorSet.start();
                            cameraPanel.setTag(null);
                        }
                    }
                } else {
                    cameraView.getLocationOnScreen(viewPosition);
                    float viewX = event.getRawX() - viewPosition[0];
                    float viewY = event.getRawY() - viewPosition[1];
                    cameraView.focusToPoint((int) viewX, (int) viewY);
                }
            }
        }
        return true;
    }


    @TargetApi(16)
    public void closeCamera(boolean animated) {
        if (takingPhoto || cameraView == null) {
            return;
        }
        cameraPanel.setAlpha(0);
        cameraPanel.setVisibility(View.GONE);
        for (int a = 0; a < 2; a++) {
            if (flashModeButton[a].getVisibility() == View.VISIBLE) {
                flashModeButton[a].setAlpha(0.0f);
                break;
            }
        }
        cameraOpened = false;
        if (Build.VERSION.SDK_INT >= 21) {
            cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

    }

    private void resetRecordState() {
        for (int a = 0; a < 2; a++) {
            flashModeButton[a].setAlpha(1.0f);
            flashModeButton[a].setVisibility(View.VISIBLE);
        }
        switchCameraButton.setAlpha(1.0f);
        switchCameraButton.setVisibility(View.VISIBLE);
        recordTime.setAlpha(0.0f);
        AndroidUtilities.cancelRunOnUIThread(videoRecordRunnable);
        videoRecordRunnable = null;

    }

    public void setDelegate(CameraActivityDelegate cameraActivityDelegate) {
        delegate = cameraActivityDelegate;
    }

}
