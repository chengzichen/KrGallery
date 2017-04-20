package com.dhc.gallery.components;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.dhc.gallery.R;
import com.dhc.gallery.utils.AndroidUtilities;


/**
 * Created by shiming on 16/11/28.
 */

public class BaseDialog extends Dialog implements View.OnClickListener,DialogInterface.OnDismissListener {

    private Context mContext;

    private FrameLayout mContainer;

    private FrameLayout mBtnPanel;

    private int lifeTime;

    private Handler handler;

    private OnClickListener mListener;
    private ImageView closeIV;

    public BaseDialog(Context context) {
        super(context, R.style.dialog_custom);
        mContext = context;
        handler = new Handler(context.getMainLooper());
        initView();
    }

    private void initView() {
        Window window = getWindow();
        window.setGravity(Gravity.CENTER); // 此处可以设置dialog显示的位置为居中
        window.setWindowAnimations(R.style.bottom_menu_animation); // 添加动画效果
        View child = getLayoutInflater().inflate(R.layout.layout_dialog_base, null, false);
        setContentView(child);
        mContainer = (FrameLayout) findViewById(R.id.fl_container);
        mBtnPanel = (FrameLayout) findViewById(R.id.fl_btn_panel);
        closeIV = (ImageView) findViewById(R.id.iv_close);
        closeIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        DisplayMetrics d = mContext.getResources().getDisplayMetrics(); // 获取屏幕宽、高用
        lp.width = (int) (d.widthPixels * 0.8); // 宽度设置为屏幕的0.9
        dialogWindow.setAttributes(lp);
        setIsCancelable(true);

        setOnDismissListener(this);
    }

    public BaseDialog setLifeTime(int seconds){
        lifeTime = seconds;
        return this;
    }

    public BaseDialog setWindowAnimation(int style){
        getWindow().setWindowAnimations(style);
        return this;
    }

    public BaseDialog showCloseBtn(boolean isShow){
        if (isShow){
            closeIV.setVisibility(View.VISIBLE);
        }else{
            closeIV.setVisibility(View.GONE);
        }
        return this;
    }

    public BaseDialog setCloseBtnClickListener(View.OnClickListener listener){
        closeIV.setOnClickListener(listener);
        return this;
    }

    public BaseDialog setMatchParent() {
        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        dialogWindow.setAttributes(lp);
        return this;
    }

    public BaseDialog setWindowBackground(int color){
        mContainer.setBackgroundColor(mContext.getResources().getColor(color));
        mBtnPanel.setBackgroundColor(mContext.getResources().getColor(color));
        return this;
    }

    public BaseDialog setDialogSize(float widthDP,float heightDP){
        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = AndroidUtilities.dp2px(getContext(),widthDP);
        lp.height = AndroidUtilities.dp2px(getContext(),heightDP);
        dialogWindow.setAttributes(lp);
        return this;
    }

    public BaseDialog setGravity(int gravity) {
        getWindow().setGravity(gravity);
        return this;
    }

    public BaseDialog setIsCancelable(boolean isCancelable) {
        setCancelable(isCancelable);
        return this;
    }

    public BaseDialog setCanCancelOutside(boolean isCan){
        setCanceledOnTouchOutside(isCan);
        return this;
    }

    public BaseDialog setCustomerContent(int layoutId) {
        View child = getLayoutInflater().inflate(layoutId, null, false);
        mContainer.addView(child);
        return this;
    }



    public BaseDialog setBackgroundResource(int viewId, int resId) {
        findViewById(viewId).setBackgroundResource(resId);
        return this;
    }

    public BaseDialog setText(int viewId, int textId) {
        ((TextView) findViewById(viewId)).setText(textId);
        return this;
    }
    public BaseDialog setText(int viewId, String text) {
        ((TextView) findViewById(viewId)).setText(text);
        return this;
    }


    public BaseDialog setTextColor(int viewId, int colorId) {
        ((TextView) findViewById(viewId)).setTextColor(colorId);
        return this;
    }

    public BaseDialog setImageResource(int viewId, int resId) {
        ((ImageView) findViewById(viewId)).setImageResource(resId);
        return this;
    }

    public BaseDialog setViewOnClickListener(int viewId, View.OnClickListener listener) {
        findViewById(viewId).setOnClickListener(listener);
        return this;
    }

    @Override
    public void show() {
        super.show();
        if (lifeTime > 0){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismiss();
                }
            },lifeTime * 1000);
        }
    }

    public BaseDialog setListener(OnClickListener listener){
        mListener = listener;
        return this;
    }


    public BaseDialog setBtnPanelView(int layoutId){
        setBtnPanelView(layoutId,null);
        return this;
    }

    public BaseDialog setBtnPanelView(int layoutId, OnClickListener listener) {
        ViewGroup viewGroup = (ViewGroup) getLayoutInflater().inflate(layoutId, null, false);
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof Button) {
                child.setOnClickListener(this);
            }
        }
        mListener = listener;
        mBtnPanel.addView(viewGroup);
//        设置容器的背景  可以根据实际情况动态添加
//        mContainer.setBackgroundResource(R.drawable.zhenai_library_dialog_base_top_shape);
        return this;
    }

    @Override
    public void onClick(View view) {
        if (mListener != null) {
            mListener.onClick(this, view.getId());
        }

    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        handler.removeCallbacksAndMessages(null);
        handler = null;
    }
}
