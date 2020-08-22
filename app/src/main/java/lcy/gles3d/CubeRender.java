package lcy.gles3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.renderscript.Float3;
import android.renderscript.Matrix4f;
import android.util.Log;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import bt.lcy.btread.LoggerConfig;
import bt.lcy.btread.R;
import lcy.gles3d.util.ShaderProgram;
import lcy.gles3d.util.ShaderUtils;
import lcy.gles3d.util.TextureUtils;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLUtils.texImage2D;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.translateM;

public class CubeRender implements GLSurfaceView.Renderer {

    private final static String TAG = CubeRender.class.getName();

    private float xRotation, yRotation;
    private final static int mNumerFaces = 6;
    private final static int []mTextureFaces2D = new int[mNumerFaces];

    // 关于着色语言(GLSL)  https://www.khronos.org/opengl/wiki/OpenGL_Shading_Language
    // https://learnopengl.com/Getting-started/Shaders
//    final String vertexShaderCode =
//            "attribute vec4 a_position;" +
//                    "attribute vec4 a_color;" +
//                    "attribute vec3 a_normal;" +
//                    "uniform mat4 u_VPMatrix;" +
//                    "uniform vec3 u_LightPos;" +
//                    "varying vec3 v_texCoords;" +
//                    "attribute vec3 a_texCoords;" +
//                    "void main()" +
//                    "{" +
//                    "v_texCoords = a_texCoords;" +
//                    "gl_Position = u_VPMatrix * a_position;" +
//                    "}";
//
//    final String fragmentShaderCode =
//            "precision mediump float;" +
//                    "uniform samplerCube u_texId;" +
//                    "varying vec3 v_texCoords;" +
//                    "void main()" +
//                    "{" +
//                    "gl_FragColor = textureCube(u_texId, v_texCoords);" +
//                    "}";

    Cube cube;
    private static final float ONE_SEC = 1000.0f; // 1 second
    int textureName;
    private  boolean first = false;

    private Context mContext;

    public CubeRender(Context context) {
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        ShaderProgram shader = new ShaderProgram(
                ShaderUtils.readShaderFileFromRawResource(mContext, R.raw.simple_vertex_shader),
                ShaderUtils.readShaderFileFromRawResource(mContext, R.raw.simple_fragment_shader)
        );

        textureName = TextureUtils.loadTexture(mContext, R.drawable.dice);
//        textureName = TextureUtils.loadCubeTexture2D(mContext, new int[]{
//                R.mipmap.num_one,
//                R.mipmap.num_two,
//                R.mipmap.num_three,
//                R.mipmap.num_four,
//                R.mipmap.num_five,
//                R.mipmap.num_six,
//        });

        cube = new Cube(shader);
        cube.setPosition(new Float3(0.0f, 0.0f, 0.0f));
        cube.setTexture(textureName);

    }


    @Override
    public void onDrawFrame(GL10 gl) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GL_TEXTURE_2D);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        cube.draw(0);
    }

    public void updateXYZ(float x,float y,float z)
    {
        if( cube != null) {
            Matrix4f camera2 = new Matrix4f();
            camera2.translate(0,0,-5f);
            cube.setCamera(camera2);
            cube.updateXYZ(x,y,z);
            if(LoggerConfig.ON){
                Log.i(TAG," cube X " + cube.rotationX + " Y: " + cube.rotationY + " Z: " + cube.rotationZ);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Matrix4f perspective = new Matrix4f();
        perspective.loadPerspective(85.0f, (float)width / (float)height, 1.0f, -150.0f);

        if(cube != null) {
            cube.setProjection(perspective);
        }
    }


    public void handleTouchDrag(float deltaX, float deltaY) {
        xRotation += deltaX / 16f;
        yRotation += deltaY / 16f;

        if (yRotation < -90) {
            yRotation = -90;
        } else if (yRotation > 90) {
            yRotation = 90;
        }

    }


    public static int loadCubeTextureImage2D(Context context, int[] cubeResources){
        // https://www3.ntu.edu.sg/home/ehchua/programming/android/Android_3D.html
        int[] textureId = new int[1];
        glGenTextures(1, textureId, 0);

        Bitmap bitmap;
        for(int i = 0 ; i < mNumerFaces ;i++){

            Drawable drawable = context.getResources().getDrawable(cubeResources[i]);
            try {

                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            } catch (OutOfMemoryError e) {
                // Handle the error
                if (LoggerConfig.ON) {
                    Log.i(TAG, "Resource ID " + cubeResources[i] + " could not be decoded");
                }
                return 0;
            }
            if (bitmap == null) {
                if (LoggerConfig.ON) {
                    Log.i(TAG, "Resource ID " + cubeResources[i] + " could not be decoded");
                }
                return 0;
            }
            ByteBuffer img1 = ByteBuffer.allocateDirect(bitmap.getAllocationByteCount());
            bitmap.copyPixelsToBuffer(img1);
            bitmap.recycle();
            img1.position(0);
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + 1, 0, GLES20.GL_RGB, 1, 1, 0,
                    GLES20.GL_RGB, GL_UNSIGNED_BYTE, img1);
        }
        glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        return textureId[0];
    }

    /**
     * 创建一个颜色纹理贴图。
     *
     * @return
     */
    private int createSimpleTextureCubemap() {
        int[] textureId = new int[1];

        // Face 0 - Red
        byte[] cubePixels0 = {127, 0, 0};
        // Face 1 - Green
        byte[] cubePixels1 = {0, 127, 0};
        // Face 2 - Blue
        byte[] cubePixels2 = {0, 0, 127};
        // Face 3 - Yellow
        byte[] cubePixels3 = {127, 127, 0};
        // Face 4 - Purple
        byte[] cubePixels4 = {127, 0, 127};
        // Face 5 - White
        byte[] cubePixels5 = {127, 127, 127};




        ByteBuffer cubePixels = ByteBuffer.allocateDirect(3);

        // Generate a texture object
        glGenTextures(1, textureId, 0);

        // Bind the texture object
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureId[0]);

        // Load the cube face - Positive X
        cubePixels.put(cubePixels0).position(0);
        glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, GLES20.GL_RGB, 1, 1, 0,
                GLES20.GL_RGB, GL_UNSIGNED_BYTE, cubePixels);

        // Load the cube face - Negative X
        cubePixels.put(cubePixels1).position(0);
        glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, GLES20.GL_RGB, 1, 1, 0,
                GLES20.GL_RGB, GL_UNSIGNED_BYTE, cubePixels);

        // Load the cube face - Positive Y
        cubePixels.put(cubePixels2).position(0);
        glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, GLES20.GL_RGB, 1, 1, 0,
                GLES20.GL_RGB, GL_UNSIGNED_BYTE, cubePixels);

        // Load the cube face - Negative Y
        cubePixels.put(cubePixels3).position(0);
        glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, GLES20.GL_RGB, 1, 1, 0,
                GLES20.GL_RGB, GL_UNSIGNED_BYTE, cubePixels);

        // Load the cube face - Positive Z
        cubePixels.put(cubePixels4).position(0);
        glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, GLES20.GL_RGB, 1, 1, 0,
                GLES20.GL_RGB, GL_UNSIGNED_BYTE, cubePixels);

        // Load the cube face - Negative Z
        cubePixels.put(cubePixels5).position(0);
        glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, GLES20.GL_RGB, 1, 1, 0,
                GLES20.GL_RGB, GL_UNSIGNED_BYTE, cubePixels);

        // Set the filtering mode
        glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        return textureId[0];
    }

    public static void loadCubeTexture2D(Context context, int[] cubeResources) {
        glGenTextures(mNumerFaces,mTextureFaces2D,0);

         Bitmap[] bitmaps = new Bitmap[mNumerFaces];
         for(int face=0;face < mNumerFaces;face++){
             bitmaps[face] = BitmapFactory.decodeStream(context.getResources().openRawResource(cubeResources[face]));
             glBindTexture(GL_TEXTURE_2D,mTextureFaces2D[face]);
             texImage2D(GL_TEXTURE_2D,0,bitmaps[face],0);
             bitmaps[face].recycle();
         }


    }

    public static int loadCubeTextureMap(Context context, int[] cubeResources) {
        final int[] textureObjectIds = new int[1];
        glGenTextures(1, textureObjectIds, 0);
        if (textureObjectIds[0] == 0) {
            if (LoggerConfig.ON) {
                Log.e(TAG, "Could not generate a new OpenGL texture Object.");
            }
            return 0;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = true;
        final Bitmap[] bitmaps = new Bitmap[6];

        for (int i = 0; i < 6; i++) {
//            bitmaps[i] = BitmapFactory.decodeResource(context.getResources(), cubeResources[i], options);
            Drawable drawable = context.getResources().getDrawable(cubeResources[i]);
            try {

                bitmaps[i] = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmaps[i]);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            } catch (OutOfMemoryError e) {
                // Handle the error
                if (LoggerConfig.ON) {
                    Log.i(TAG, "Resource ID " + cubeResources[i] + " could not be decoded");
                }
                return 0;
            }

            if (bitmaps[i] == null) {
                if (LoggerConfig.ON) {
                    Log.i(TAG, "Resource ID " + cubeResources[i] + " could not be decoded");
                }
                GLES20.glDeleteTextures(1, textureObjectIds, 0);
                return 0;
            }

        }


        // 绑定与配置纹理过滤器
        glBindTexture(GL_TEXTURE_2D, textureObjectIds[0]);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);


//        glGenerateMipmap(GL_TEXTURE_2D);
        texImage2D(GL_TEXTURE_2D, 0, bitmaps[0], 0);
        texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, bitmaps[1], 0);

        texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, bitmaps[2], 0);
        texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, bitmaps[3], 0);

        texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, bitmaps[4], 0);
        texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, bitmaps[5], 0);

        glBindTexture(GL_TEXTURE_2D, 0);

//        glGenerateMipmap(GL_TEXTURE_2D);

        for (Bitmap bitmap : bitmaps) {
            bitmap.recycle();
        }


        return textureObjectIds[0];
    }


    public static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);
        int[] compiled = new int[1];

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            if (LoggerConfig.ON) {
                Log.d(TAG, "Compilation\n" + GLES20.glGetShaderInfoLog(shader));
            }

            return 0;
        }

        return shader;
    }

    public static int loadProgram(String vertShaderSrc, String fragShaderSrc) {
        int vertexShader;
        int fragmentShader;
        int programObject;
        int[] linked = new int[1];

        // Load the vertex/fragment shaders
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertShaderSrc);
        if (vertexShader == 0) {
            if (LoggerConfig.ON) {
                Log.e(TAG, "loadShader  GL_VERTEX_SHADER failed\n" + vertShaderSrc);
            }
            return 0;
        }


        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragShaderSrc);
        if (fragmentShader == 0) {
            if (LoggerConfig.ON) {
                Log.e(TAG, "loadShader GL_FRAGMENT_SHADER failed\n" + fragShaderSrc);
            }
            GLES20.glDeleteShader(vertexShader);
            return 0;
        }

        // 创建程序对像
        programObject = GLES20.glCreateProgram();

        if (programObject == 0)
            return 0;
        // 附加顶点着色器到程序。
        GLES20.glAttachShader(programObject, vertexShader);
        // 附加片段着色器到程序。
        GLES20.glAttachShader(programObject, fragmentShader);

        // Link the program
        GLES20.glLinkProgram(programObject);

        // Check the link status
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
            Log.e("顶点着色器", "Error linking program:");
            Log.e("顶点着色器", GLES20.glGetProgramInfoLog(programObject));
            GLES20.glDeleteProgram(programObject);
            return 0;
        }

        // Free up no longer needed shader resources
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return programObject;
    }

}
