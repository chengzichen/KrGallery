
package com.dhc.gallery.tl;

public class TLUploadFile extends TLObject {
    public static int constructor = 0x96a18d5;

    public StorageFileType type;
    public int mtime;
    public NativeByteBuffer bytes;

    public static TLUploadFile TLdeserialize(AbstractSerializedData stream, int constructor,
                                             boolean exception) {
        if (TLUploadFile.constructor != constructor) {
            if (exception) {
                throw new RuntimeException(
                        String.format("can't parse magic %x in TLUploadFile", constructor));
            } else {
                return null;
            }
        }
        TLUploadFile result = new TLUploadFile();
        result.readParams(stream, exception);
        return result;
    }

    public void readParams(AbstractSerializedData stream, boolean exception) {
        type = StorageFileType.TLdeserialize(stream, stream.readInt32(exception), exception);
        mtime = stream.readInt32(exception);
        bytes = stream.readByteBuffer(exception);
    }

    @Override
    public void freeResources() {
        if (disableFree) {
            return;
        }
        if (bytes != null) {
            bytes.reuse();
            bytes = null;
        }
    }
}
