package com.bipin.dieto;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;

public class ImageUtils {

    public static Bitmap toBitmap(ImageProxy image) {
        if (image == null) return null;

        try {
            // CameraX 1.4.0 native toBitmap conversion supports YUV_420_888, RGBA_8888, etc.
            Bitmap bitmap = image.toBitmap();
            if (bitmap != null) {
                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                if (rotationDegrees != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
                return bitmap;
            }
        } catch (Throwable t) {
            // Fallback to manual plane extraction
        }

        try {
            if (image.getPlanes() != null && image.getPlanes().length > 0) {
                ImageProxy.PlaneProxy plane = image.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();
                buffer.rewind();

                int pixelStride = plane.getPixelStride();
                int rowStride = plane.getRowStride();
                int rowPadding = rowStride - pixelStride * image.getWidth();

                Bitmap bitmap = Bitmap.createBitmap(
                        image.getWidth() + (pixelStride > 0 ? rowPadding / pixelStride : 0),
                        image.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
                bitmap.copyPixelsFromBuffer(buffer);

                if (rowPadding > 0) {
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, image.getWidth(), image.getHeight());
                }

                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                if (rotationDegrees != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }

                return bitmap;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }
}