package lcy.gles3d.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.core.graphics.drawable.IconCompat;

import java.io.IOException;
import java.io.InputStream;

import bt.lcy.btread.LoggerConfig;
import bt.lcy.btread.R;
import bt.lcy.btread.fragments.StringStream;

import static android.opengl.GLES20.GL_REPEAT;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLUtils.texImage2D;

public class TextureUtils {
    private final static String TAG = TextureUtils.class.getName();

    public static int loadTexture(Context context, int drawableID) {
        return loadTexture(context, drawableID, true);
    }

    public static int loadTexture(Context context, int drawableID, boolean isBottomLeftOrigin) {
        Bitmap bitmap = null;
        Bitmap flippedBitmap = null;
        int textureName;
        bitmap = BitmapFactory.decodeResource(context.getResources(), drawableID);
        if (isBottomLeftOrigin) {
            Matrix flip = new Matrix();
            flip.postScale(1f, -1f);
            flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), flip, false);
            textureName = loadTexture(flippedBitmap);
            bitmap.recycle();
        } else {
            textureName = loadTexture(bitmap);
        }

//        textureName = loadTexture(bitmap);
        return textureName;
    }

    public static int loadTexture(Bitmap bitmap) {

        final int[] textureName = new int[1];
        GLES20.glGenTextures(1, textureName, 0);
        GLES20.glBindTexture(GL_TEXTURE_2D, textureName[0]);

        glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
//        glGenerateMipmap(GL_TEXTURE_2D);
        bitmap.recycle();
        glBindTexture(GL_TEXTURE_2D, 0);
        return textureName[0];
    }

    public static int loadCubeTexture2D(@NonNull Context context, @RawRes int[] cubeResources) {

        final int[] textureId = new int[6];
        glGenTextures(6, textureId, 0);
        GLES20.glBindTexture(GL_TEXTURE_2D, textureId[0]);
        glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        Bitmap bitmap = null;
        // https://stackoverflow.com/questions/24389043/bitmapfactory-decoderesource-returns-null-for-shape-defined-in-xml-drawable
        for (int face = 0; face < 6; face++) {
            Drawable drawable = context.getResources().getDrawableForDensity(cubeResources[face], DisplayMetrics.DENSITY_MEDIUM);
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0,0,canvas.getWidth(),canvas.getHeight());
            drawable.draw(canvas);
            glBindTexture(GL_TEXTURE_2D, textureId[face]);
            texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

        }
        return textureId[0];
    }
}

