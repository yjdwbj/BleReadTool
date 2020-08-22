package lcy.gles3d;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import lcy.gles3d.util.ShaderProgram;


// https://arm-software.github.io/opengl-es-sdk-for-android/simple_cube.html
// https://www.thetopsites.net/article/53338754.shtml
// https://gist.github.com/SebastianJay/3316001
public class Cube extends CubeModel {
    static final int COORDS_PER_VERTEX = 3;
    //  立方体有六个方形表面，而OpenGL只支持画三角形，因此需要画12个三角形，每个面两个。我们用定义三角形顶点的方式来定义这些顶点。
    // 参考图示： https://developer.android.com/images/opengl/ccw-square.png
    // https://learnopengl-cn.github.io/img/01/04/ndc.png
    static final float[] vertices = {  // Vertices of the 6 faces
            // 这是显卡内置的坐标系，无法改变。(-1, -1)是屏幕的左下角，(1, -1)是右下角，(0, 1)位于中上部
            // Front
            1, -1, 1, 1, 0, 0, 1, 0.25f, 0f,      // 0
            1, 1, 1, 0, 1, 0, 1, 0.25f, 0.25f,   // 1
            -1, 1, 1, 0, 0, 1, 1, 0f, 0.25f,      // 2
            -1, -1, 1, 0, 0, 0, 1, 0f, 0f,         // 3

            // Back
            -1, -1, -1, 0, 0, 1, 1, 0.5f, 0f,       // 4
            -1, 1, -1, 0, 1, 0, 1, 0.5f, 0.25f,    // 5
            1, 1, -1, 1, 0, 0, 1, 0.25f, 0.25f,   // 6
            1, -1, -1, 0, 0, 0, 1, 0.25f, 0f,      // 7

            // Left
            -1, -1, 1, 1, 0, 0, 1, 0.75f, 0f,      // 8
            -1, 1, 1, 0, 1, 0, 1, 0.75f, 0.25f,   // 9
            -1, 1, -1, 0, 0, 1, 1, 0.5f, 0.25f,    // 10
            -1, -1, -1, 0, 0, 0, 1, 0.5f, 0f,       // 11

            // Right
            1, -1, -1, 1, 0, 0, 1, 1f, 0f,         // 12
            1, 1, -1, 0, 1, 0, 1, 1f, 0.25f,      // 13
            1, 1, 1, 0, 0, 1, 1, 0.75f, 0.25f,   // 14
            1, -1, 1, 0, 0, 0, 1, 0.75f, 0f,      // 15

            // Top
            1, 1, 1, 1, 0, 0, 1, 0.25f, 0.25f,   // 16
            1, 1, -1, 0, 1, 0, 1, 0.25f, 0.5f,    // 17
            -1, 1, -1, 0, 0, 1, 1, 0f, 0.5f,       // 18
            -1, 1, 1, 0, 0, 0, 1, 0f, 0.25f,      // 19

            // Bottom
            1, -1, -1, 1, 0, 0, 1, 0.5f, 0.25f,    // 20
            1, -1, 1, 0, 1, 0, 1, 0.5f, 0.5f,     // 21
            -1, -1, 1, 0, 0, 1, 1, 0.25f, 0.5f,    // 22
            -1, -1, -1, 0, 0, 0, 1, 0.25f, 0.25f,   // 23

    };

    static final short[] indices = {  // order to draw vertices
            // Front
            0, 1, 2,
            2, 3, 0,

            // Back
            4, 5, 6,
            6, 7, 4,

            // Left
            8, 9, 10,
            10, 11, 8,

            // Right
            12, 13, 14,
            14, 15, 12,

            // Top
            16, 17, 18,
            18, 19, 16,

            // Bottom
            20, 21, 22,
            22, 23, 20

    };


    public Cube(ShaderProgram shaderProgram) {
        super("cube", shaderProgram, vertices, indices);
    }


}
