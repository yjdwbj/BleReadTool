package lcy.gles3d;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static java.lang.Math.floor;
import static java.lang.Math.sin;


/**
 * OpenGL ES  https://developer.android.com/guide/topics/graphics/opengl
 * 使用 OpenGL ES 显示图形 https://developer.android.com/training/graphics/opengl
 * 欢迎来到OpenGL的世界  https://learnopengl-cn.github.io/  https://learnopengl-cn.readthedocs.io/zh/latest/
 * 因为在3D世界里，3角形是最简单的平面，任何复杂的平面是组件都是由3角形构成的。
 */


public class TriangleRenderer implements GLSurfaceView.Renderer {
    private int mProgram;
    private int maPositionHandle;

    private int colorHandle;

    private FloatBuffer triangleVB;
    public float mAngle;



    // 定义一个变形矩阵
    private float[] mMMatrix = new float[16];

    // 定义照相机视图
    private int muMVPMatrixHandle;
    //  复合变换：模型观察投影矩阵（MVP）
    private float[] mMVPMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mProjMatrix = new float[16];



    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;   \n" +

                    "attribute vec4 vPosition;  \n" +
                    "void main(){               \n" +

                    // the matrix must be included as a modifier of gl_Position
                    " gl_Position = uMVPMatrix * vPosition; \n" +

                    "}  \n";

    // 简单的在顶点着色器(Vertex Shader),可以使用字符串定义，也可以从文件中加载， 格式类C风格。
    // 参考： https://www.opengl-tutorial.org/beginners-tutorials/tutorial-3-matrices/
//    private final String vertexShaderCode =
//            "attribute vec4 vPosition; \n" +
//                    "void main(){              \n" +
//                    " gl_Position = vPosition; \n" +
//                    "}                         \n";

    private final String fragmentShaderCode =
            "precision mediump float;  \n" +
                    "void main(){              \n" +
                    " gl_FragColor = vec4 (0.63671875, 0.76953125, 0.22265625, 1.0); \n" +
                    "}                         \n";

    public float getAngle() {
        return mAngle;
    }

    public void setAngle(float mAngle) {
        this.mAngle = mAngle;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 创建一个顶点着色器
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        // 创建一个片段着色器。
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // 创建一个空的 OpenGL程序
        GLES20.glAttachShader(mProgram, vertexShader);   // 附加 vertex shader到程序
        GLES20.glAttachShader(mProgram, fragmentShader); // 附加fragment shader到程序
        GLES20.glLinkProgram(mProgram);                  // 链接程序

        // get handle to the vertex shader's vPosition member
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
//        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glClearColor(0f,0f,0f,1.0f);
//        GLES20.glClearDepthf(1.0f);
        initShapes();
    }

    @Override
    public void onDrawFrame(GL10 gl) {

//        gl.glClear(GL10.GL_COLOR_BUFFER_BIT|GL10.GL_DEPTH_BUFFER_BIT);

        GLES20.glClear(GL_COLOR_BUFFER_BIT);
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        float[] scratch = new float[16];

        Matrix.setRotateM(mMMatrix,0,mAngle,0,0,-1.0f);
        Matrix.multiplyMM(scratch,0,mProjMatrix,0,mMVPMatrix,0);

        // 获取顶点着色器的vPosition成员句柄
        maPositionHandle =  GLES20.glGetAttribLocation(mProgram,"vPosition");
        //启用三角形顶点的句柄
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        // 准备三角形的坐标数据
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, triangleVB);


//        // 把 mProjMatrix * mVMatrix 的矩阵放入到 mMVPMatrix。
//        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);
//        // 更新uniform值,绘制三角形的颜色。前面使用 GLES20.glGetUniformLocation(mProgram, "uMVPMatrix") 找取了它的索引值。
//        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // 创建一个旋转
        long time = SystemClock.uptimeMillis() % 4000L;
        float angle = 0.009f *((int)time);
        Matrix.setRotateM(mMMatrix,0,angle,0,0,1.0f);
        Matrix.multiplyMM(mMVPMatrix,0,mVMatrix,0,mMMatrix,0);
        Matrix.multiplyMM(mMVPMatrix,0,mProjMatrix,0,mMVPMatrix,0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        GLES20.glDisableVertexAttribArray(maPositionHandle);

    }



    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        float ratio = (float) width / height;

        // 以六个夹平面定义一个投影矩阵 视椎体。该投影矩阵应用于对象坐标。
        // in the onDrawFrame() method
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 6);

        // 更新uniform颜色
//        float timeValue = glfwGetTime();
//        float greenValue = (sin(timeValue) / 2) + 0.5;
        // 找取着色器中uniform属性的索引/位置值
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

    }

    private int loadShader(int type, String shaderCode) {
        // 创建顶点着色器(vertex shader)  (GLES20.GL_VERTEX_SHADER)
        // 或者片段着色器(fragment shader)  (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);

        // 编译着色器的代码
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * 这里只是把简单的要形状对像与Renderer杂合写在一起了。
     */
    private void initShapes() {
        float triangleCoords[] = {
                // X Y Z
                -0.5f, -0.25f, 0,
                0.5f, -0.25f, 0,
                0.0f, 0.55016994f, 0
        };
        // 为三角形要顶点缓存分配内存，每一个坐标*4字节。
        ByteBuffer vbb = ByteBuffer.allocateDirect(triangleCoords.length * 4);
        vbb.order(ByteOrder.nativeOrder());   // 使用硬件设备端的字节序
        triangleVB = vbb.asFloatBuffer();
        triangleVB.put(triangleCoords);     // 添加坐标到缓存中。
        triangleVB.position(0);  //设置缓冲区起始位置
    }

    public void updateDraw(float x,float y,float z){
        float[] scratch = new float[16];
        Matrix.setRotateM(mMMatrix, 0, mAngle, x, y, z);
        Matrix.multiplyMM(scratch, 0, mProjMatrix, 0, mMVPMatrix, 0);

        maPositionHandle = GLES20.glGetUniformLocation(mProgram,"uMVPMatrix");
        GLES20.glUniformMatrix4fv(maPositionHandle,1,false,scratch,0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,3);
    }


}
