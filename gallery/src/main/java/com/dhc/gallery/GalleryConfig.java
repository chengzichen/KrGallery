
package com.dhc.gallery;

import android.os.Parcel;
import android.os.Parcelable;

import com.dhc.gallery.ui.GalleryActivity;

/**
 * the {@link GalleryActivity} of buidler.
 */
public class GalleryConfig implements Parcelable {


    public static final int SELECT_PHOTO = 0;
    public static final int TAKE_PHOTO = 1;
    public static final int RECORD_VEDIO = 2;
    public static final int SELECT_VEDIO = 3;
    public static final int TAKEPHOTO_RECORDVEDIO = 4;
    private String[] filterMimeTypes = new String[]{};
    private String hintOfPick;
    private boolean singlePhoto = false;
    private int limitPickPhoto = 9;
    private boolean isSingleVedio = false;
    private boolean isNeedCrop = false;
    private String filePath;
    private int type = SELECT_PHOTO;
    private int requestCode = -1;
    private int limitRecordTime;
    private int limitRecordSize;


    public GalleryConfig() {
    }

    public String[] getFilterMimeTypes() {
        return filterMimeTypes;
    }

    public void setFilterMimeTypes(String[] filterMimeTypes) {
        this.filterMimeTypes = filterMimeTypes;
    }

    public int getLimitRecordSize() {
        return limitRecordSize;
    }

    public void setLimitRecordSize(int limitRecordSize) {
        this.limitRecordSize = limitRecordSize;
    }

    public int getLimitRecordTime() {
        return limitRecordTime;
    }

    public void setLimitRecordTime(int limitRecordTime) {
        this.limitRecordTime = limitRecordTime;
    }

    public String getHintOfPick() {
        return hintOfPick;
    }

    public void setHintOfPick(String hintOfPick) {
        this.hintOfPick = hintOfPick;
    }

    public boolean isSinglePhoto() {
        return singlePhoto;
    }

    public void setSinglePhoto(boolean singlePhoto) {
        this.singlePhoto = singlePhoto;
    }

    public int getLimitPickPhoto() {
        return limitPickPhoto;
    }

    public void setLimitPickPhoto(int limitPickPhoto) {
        this.limitPickPhoto = limitPickPhoto;
    }

    public boolean isSingleVedio() {
        return isSingleVedio;
    }

    public void setSingleVedio(boolean singleVedio) {
        isSingleVedio = singleVedio;
    }

    public boolean isNeedCrop() {
        return isNeedCrop;
    }

    public void setNeedCrop(boolean needCrop) {
        isNeedCrop = needCrop;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(this.filterMimeTypes);
        dest.writeString(this.hintOfPick);
        dest.writeByte(this.singlePhoto ? (byte) 1 : (byte) 0);
        dest.writeInt(this.limitPickPhoto);
        dest.writeByte(this.isSingleVedio ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isNeedCrop ? (byte) 1 : (byte) 0);
        dest.writeString(this.filePath);
        dest.writeInt(this.type);
        dest.writeInt(this.requestCode);
        dest.writeInt(this.limitRecordTime);
        dest.writeInt(this.limitRecordSize);
    }

    protected GalleryConfig(Parcel in) {
        this.filterMimeTypes = in.createStringArray();
        this.hintOfPick = in.readString();
        this.singlePhoto = in.readByte() != 0;
        this.limitPickPhoto = in.readInt();
        this.isSingleVedio = in.readByte() != 0;
        this.isNeedCrop = in.readByte() != 0;
        this.filePath = in.readString();
        this.type = in.readInt();
        this.requestCode = in.readInt();
        this.limitRecordTime = in.readInt();
        this.limitRecordSize = in.readInt();
    }

    public static final Parcelable.Creator<GalleryConfig> CREATOR = new Parcelable.Creator<GalleryConfig>() {
        @Override
        public GalleryConfig createFromParcel(Parcel source) {
            return new GalleryConfig(source);
        }

        @Override
        public GalleryConfig[] newArray(int size) {
            return new GalleryConfig[size];
        }
    };
}
