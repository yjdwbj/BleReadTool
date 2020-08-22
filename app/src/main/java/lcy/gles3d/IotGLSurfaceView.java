package lcy.gles3d;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class IotGLSurfaceView  extends GLSurfaceView {
    private final float TOUCH_SCALE_FACTOR = 180.0f/320;//角度缩放比例
    private static final String TAG = IotGLSurfaceView.class.getName();

    private float previousX;
    private float previousY;


//    private TriangleRenderer renderer;
    private CubeRender renderer;
//      private SimpleTextureCube renderer;



    public IotGLSurfaceView(Context context, AttributeSet attributeSet){
        super(context,attributeSet);
        setEGLContextClientVersion(2);
//        renderer = new TriangleRenderer();
//        setPreserveEGLContextOnPause(true);
        renderer = new CubeRender(getContext());
//        renderer = new SimpleTextureCube(getContext());
        setRenderer(renderer);

        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);
        setVisibility(VISIBLE);
    }



    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float y = e.getY();
        float x = e.getX();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                previousX = e.getX();
                previousY = e.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float dy = y - previousY;//计算触控笔Y位移
                final float dx = x - previousX;//计算触控笔Y位移

//                queueEvent(new Runnable() {
//                    @Override
//                    public void run() {
//                        renderer.handleTouchDrag(dy,dx);
//                        requestRender();
//                    }
//                });

                Log.i(TAG,"ACTION_MOVE: x " + x + " y: " + y);
//                renderer.setAngle(renderer.getAngle() + (dx+dy) * TOUCH_SCALE_FACTOR );
//                requestRender();
        }
//        previousY = y;//记录触控笔位置
//        previousX = x;//记录触控笔位置
        return true;
    }

    public  void updateXYZ(float x,float y,float z){
        renderer.updateXYZ(x,y,z);
        requestRender();
    }





}
