package com.dhc.gallery;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * 创建者     邓浩宸
 * 创建时间   2017/4/17 16:29
 * 描述	      ${TODO}
 */
public class GalleryData implements Parcelable {

    private List<String> mList;
    private Bitmap mBitmap;
    private  boolean isBitmap;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(this.mList);
        dest.writeParcelable(this.mBitmap, flags);
        dest.writeByte(this.isBitmap ? (byte) 1 : (byte) 0);
    }

    public GalleryData() {
    }

    protected GalleryData(Parcel in) {
        this.mList = in.createStringArrayList();
        this.mBitmap = in.readParcelable(Bitmap.class.getClassLoader());
        this.isBitmap = in.readByte() != 0;
    }

    public static final Parcelable.Creator<GalleryData> CREATOR = new Parcelable.Creator<GalleryData>() {
        @Override
        public GalleryData createFromParcel(Parcel source) {
            return new GalleryData(source);
        }

        @Override
        public GalleryData[] newArray(int size) {
            return new GalleryData[size];
        }
    };
}
