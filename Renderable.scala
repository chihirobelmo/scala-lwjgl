package com.example.render

import org.lwjgl.opengl._
import org.lwjgl._
import org.lwjgl.glfw._
import org.lwjgl.system.MemoryUtil
import org.joml.Matrix4f
import org.joml.Vector3f

object Util {
    def createSRT2D(scale: Vector3f, rotation: Int, translation: Vector3f): Matrix4f = {
        val matrix = new Matrix4f()
        matrix.identity()
        matrix.scale(scale)
        matrix.rotateZ(Math.toRadians(45).toFloat)
        matrix.translate(translation)
        matrix
    }
}

trait Renderable {
    def preRender(): Renderable
    def render(): Renderable
    def end(): Renderable
}

trait Renderable2D extends Renderable {
    def srt(scale: Float, rot: Float, tran: Vector3f): Renderable2D
}

object PrimitiveShader {
    
    val vertexShaderSource =
    """
    #version 330 core
    layout(location = 0) in vec3 aPos;
    uniform mat4 srtMatrix;
    void main() {
        gl_Position = srtMatrix * vec4(aPos, 1.0);
    }
    """

    val fragmentShaderSource =
    """
    #version 330 core
    out vec4 FragColor;
    void main() {
        FragColor = vec4(1.0, 0.5, 0.2, 1.0);
    }
    """

    val vertexShader = compileShader(vertexShaderSource, GL20.GL_VERTEX_SHADER)
    val fragmentShader = compileShader(fragmentShaderSource, GL20.GL_FRAGMENT_SHADER)

    val shaderProgram = GL20.glCreateProgram()

    private def compileShader(source: String, shaderType: Int): Int = {
        val shader = GL20.glCreateShader(shaderType)
        GL20.glShaderSource(shader, source)
        GL20.glCompileShader(shader)

        // コンパイルエラーの確認
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(s"シェーダーのコンパイルに失敗: ${GL20.glGetShaderInfoLog(shader)}")
        }
        shader
    }
}

abstract class Primitive() extends Renderable2D {

    var vaoId_ = 0
    var vboId_ = 0

    private val srtMatrix = new Matrix4f()

    def init(): Unit = {
        this.vaoId_ = GL30.glGenVertexArrays()
        this.vboId_ = GL15.glGenBuffers()
    }

    private def compileShader(source: String, shaderType: Int): Int = {
        val shader = GL20.glCreateShader(shaderType)
        GL20.glShaderSource(shader, source)
        GL20.glCompileShader(shader)

        // コンパイルエラーの確認
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(s"シェーダーのコンパイルに失敗: ${GL20.glGetShaderInfoLog(shader)}")
        }
        shader
    }

    override def preRender(): Renderable = {
        GL20.glAttachShader(PrimitiveShader.shaderProgram, PrimitiveShader.vertexShader)
        GL20.glAttachShader(PrimitiveShader.shaderProgram, PrimitiveShader.fragmentShader)
        GL20.glLinkProgram(PrimitiveShader.shaderProgram)

        GL20.glUseProgram(PrimitiveShader.shaderProgram)

        // Pass the matrix to the shader
        val srtMatrixLocation = GL20.glGetUniformLocation(PrimitiveShader.shaderProgram, "srtMatrix")
        val matrixBuffer = MemoryUtil.memAllocFloat(16)
        srtMatrix.get(matrixBuffer)
        GL20.glUniformMatrix4fv(srtMatrixLocation, false, matrixBuffer)
        MemoryUtil.memFree(matrixBuffer)

        this
    }

    override def end(): Renderable = {
        GL15.glDeleteBuffers(vboId_)
        GL30.glDeleteVertexArrays(vaoId_)

        this
    }

    override def srt(scale: Float, rot: Float, tran: Vector3f): Renderable2D = {
        val scaleMatrix = new Matrix4f().scaling(scale, scale, scale)
        val rotationMatrix = new Matrix4f().rotateZ(Math.toRadians(rot).toFloat)
        val translationMatrix = new Matrix4f().translation(tran)
        srtMatrix.identity().mul(translationMatrix).mul(rotationMatrix).mul(scaleMatrix)

        this
    }
}

class Triangle() extends Primitive() {
    init()

    // 頂点データ
    val vertices: Array[Float] = Array(
        -0.5f, -0.5f, 0.0f, // 左下
        0.5f, -0.5f, 0.0f,  // 右下
        0.0f,  0.5f, 0.0f   // 上
    )

    GL30.glBindVertexArray(this.vaoId_)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboId_)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW)

    // 頂点属性の設定
    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0)
    GL20.glEnableVertexAttribArray(0)

    // バインド解除
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GL30.glBindVertexArray(0)

    override def render(): Renderable = {
        GL30.glBindVertexArray(vaoId_)
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3)
        GL30.glBindVertexArray(0)

        this
    }
}

class AirTrack() extends Primitive() {
    init()

    // 頂点データ
    val vertices: Array[Float] = Array(
        -0.5f, -0.5f, +0.0f, // Line start point
        +0.5f, -0.5f, +0.0f, // Line end point
        +0.5f, +0.5f, +0.0f, // Another line start point
        -0.5f, +0.5f, +0.0f  // Another line end point
    )

    GL30.glBindVertexArray(this.vaoId_)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboId_)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW)

    // 頂点属性の設定
    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0)
    GL20.glEnableVertexAttribArray(0)

    // バインド解除
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GL30.glBindVertexArray(0)

    override def render(): Renderable = {
        GL30.glBindVertexArray(vaoId_)
        GL11.glDrawArrays(GL11.GL_LINES, 0, vertices.length / 3)
        GL30.glBindVertexArray(0)

        this
    }
}

/*

  0 1 2 3 4 5 6 7
0 0 0 1 1 1 1 0 0  // 0x3C
1 0 1 0 0 0 0 1 0  // 0x42
2 1 0 0 0 0 0 0 1  // 0x81
3 1 0 0 0 0 0 0 1  // 0x81
4 1 1 1 1 1 1 1 1  // 0xFF
5 1 0 0 0 0 0 0 1  // 0x81
6 1 0 0 0 0 0 0 1  // 0x81
7 1 0 0 0 0 0 0 1  // 0x81

val A: Array[Int] = Array(
  0x3C, // 00111100
  0x42, // 01000010
  0x81, // 10000001
  0x81, // 10000001
  0xFF, // 11111111
  0x81, // 10000001
  0x81, // 10000001
  0x81  // 10000001
)

*/
object FontData {
    val fontWidth = 8
    val fontHeight = 8

    // 簡単なビットマップフォントデータ（例として8x8の固定幅フォント）
    val fontData: Array[Array[Int]] = Array(
        Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00), // ' ' (space)
        Array(0x18, 0x3C, 0x3C, 0x18, 0x18, 0x00, 0x18, 0x00), // '!'
        Array(0x6C, 0x6C, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00), // '"'
        Array(0x6C, 0x6C, 0xFE, 0x6C, 0xFE, 0x6C, 0x6C, 0x00), // '#'
        Array(0x18, 0x3E, 0x58, 0x3C, 0x1A, 0x7C, 0x18, 0x00), // '$'
        Array(0x00, 0x66, 0x6C, 0x18, 0x30, 0x66, 0x46, 0x00), // '%'
        Array(0x1C, 0x36, 0x1C, 0x6E, 0x3B, 0x33, 0x6E, 0x00), // '&'
        Array(0x18, 0x18, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00), // '''
        Array(0x0C, 0x18, 0x30, 0x30, 0x30, 0x18, 0x0C, 0x00), // '('
        Array(0x30, 0x18, 0x0C, 0x0C, 0x0C, 0x18, 0x30, 0x00), // ')'
        Array(0x00, 0x66, 0x3C, 0xFF, 0x3C, 0x66, 0x00, 0x00), // '*'
        Array(0x00, 0x18, 0x18, 0x7E, 0x18, 0x18, 0x00, 0x00), // '+'
        Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x18, 0x30), // ','
        Array(0x00, 0x00, 0x00, 0x7E, 0x00, 0x00, 0x00, 0x00), // '-'
        Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x18, 0x00), // '.'
        Array(0x06, 0x0C, 0x18, 0x30, 0x60, 0xC0, 0x80, 0x00), // '/'
        Array(0x3C, 0x66, 0x6E, 0x76, 0x66, 0x66, 0x3C, 0x00), // '0'
        Array(0x18, 0x38, 0x18, 0x18, 0x18, 0x18, 0x7E, 0x00), // '1'
        Array(0x3C, 0x66, 0x06, 0x1C, 0x30, 0x60, 0x7E, 0x00), // '2'
        Array(0x3C, 0x66, 0x06, 0x1C, 0x06, 0x66, 0x3C, 0x00), // '3'
        Array(0x0C, 0x1C, 0x3C, 0x6C, 0xFE, 0x0C, 0x0C, 0x00), // '4'
        Array(0x7E, 0x60, 0x7C, 0x06, 0x06, 0x66, 0x3C, 0x00), // '5'
        Array(0x3C, 0x66, 0x60, 0x7C, 0x66, 0x66, 0x3C, 0x00), // '6'
        Array(0x7E, 0x66, 0x0C, 0x18, 0x30, 0x30, 0x30, 0x00), // '7'
        Array(0x3C, 0x66, 0x66, 0x3C, 0x66, 0x66, 0x3C, 0x00), // '8'
        Array(0x3C, 0x66, 0x66, 0x3E, 0x06, 0x66, 0x3C, 0x00), // '9'
        Array(0x00, 0x18, 0x18, 0x00, 0x00, 0x18, 0x18, 0x00), // ':'
        Array(0x00, 0x18, 0x18, 0x00, 0x00, 0x18, 0x18, 0x30), // ';'
        Array(0x0E, 0x1C, 0x38, 0x70, 0x38, 0x1C, 0x0E, 0x00), // '<'
        Array(0x00, 0x00, 0x7E, 0x00, 0x00, 0x7E, 0x00, 0x00), // '='
        Array(0x70, 0x38, 0x1C, 0x0E, 0x1C, 0x38, 0x70, 0x00), // '>'
        Array(0x3C, 0x66, 0x06, 0x1C, 0x18, 0x00, 0x18, 0x00), // '?'
        Array(0x3C, 0x66, 0x6E, 0x6E, 0x60, 0x60, 0x3E, 0x00), // '@'
        Array(0x18, 0x3C, 0x66, 0x66, 0x7E, 0x66, 0x66, 0x00), // 'A'
        Array(0x7C, 0x66, 0x66, 0x7C, 0x66, 0x66, 0x7C, 0x00), // 'B'
        Array(0x3C, 0x66, 0x60, 0x60, 0x60, 0x66, 0x3C, 0x00), // 'C'
        Array(0x7C, 0x66, 0x66, 0x66, 0x66, 0x66, 0x7C, 0x00), // 'D'
        Array(0x7E, 0x60, 0x60, 0x7C, 0x60, 0x60, 0x7E, 0x00), // 'E'
        Array(0x7E, 0x60, 0x60, 0x7C, 0x60, 0x60, 0x60, 0x00), // 'F'
        Array(0x3C, 0x66, 0x60, 0x6E, 0x66, 0x66, 0x3C, 0x00), // 'G'
        Array(0x66, 0x66, 0x66, 0x7E, 0x66, 0x66, 0x66, 0x00), // 'H'
        Array(0x3C, 0x18, 0x18, 0x18, 0x18, 0x18, 0x3C, 0x00), // 'I'
        Array(0x1E, 0x0C, 0x0C, 0x0C, 0x0C, 0x6C, 0x38, 0x00), // 'J'
        Array(0x66, 0x6C, 0x78, 0x70, 0x78, 0x6C, 0x66, 0x00), // 'K'
        Array(0x60, 0x60, 0x60, 0x60, 0x60, 0x60, 0x7E, 0x00), // 'L'
        Array(0x63, 0x77, 0x7F, 0x7F, 0x6B, 0x63, 0x63, 0x00), // 'M'
        Array(0x66, 0x76, 0x7E, 0x7E, 0x6E, 0x66, 0x66, 0x00), // 'N'
        Array(0x3C, 0x66, 0x66, 0x66, 0x66, 0x66, 0x3C, 0x00), // 'O'
        Array(0x7C, 0x66, 0x66, 0x7C, 0x60, 0x60, 0x60, 0x00), // 'P'
        Array(0x3C, 0x66, 0x66, 0x66, 0x66, 0x6E, 0x3C, 0x06), // 'Q'
        Array(0x7C, 0x66, 0x66, 0x7C, 0x78, 0x6C, 0x66, 0x00), // 'R'
        Array(0x3C, 0x66, 0x30, 0x1C, 0x06, 0x66, 0x3C, 0x00), // 'S'
        Array(0x7E, 0x7E, 0x5A, 0x18, 0x18, 0x18, 0x3C, 0x00), // 'T'
        Array(0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x3C, 0x00), // 'U'
        Array(0x66, 0x66, 0x66, 0x66, 0x66, 0x3C, 0x18, 0x00), // 'V'
        Array(0x63, 0x63, 0x63, 0x6B, 0x7F, 0x77, 0x63, 0x00), // 'W'
        Array(0x66, 0x66, 0x3C, 0x18, 0x3C, 0x66, 0x66, 0x00), // 'X'
        Array(0x66, 0x66, 0x66, 0x3C, 0x18, 0x18, 0x3C, 0x00), // 'Y'
        Array(0x7F, 0x63, 0x31, 0x18, 0x0C, 0x46, 0x7F, 0x00)  // 'Z'
    )
}

class FontRenderer(x: Float, y: Float, str: String, windowWidth: Int, windowHeight: Int) extends Primitive() {

    var x_ = x
    var y_ = y
    var str_ = str
    var windowWidth_ = windowWidth
    var windowHeight_ = windowHeight

    private def drawChar(x: Float, y: Float, c: Char, windowWidth: Int, windowHeight: Int): Unit = {
        val index = c - ' '
        if (index < 0 || index >= FontData.fontData.length) return

        val data = FontData.fontData(index)
        GL11.glColor3f(1.0f, 1.0f, 1.0f)
        GL11.glBegin(GL11.GL_QUADS)
        for (i <- 0 until FontData.fontHeight) {
            for (j <- 0 until FontData.fontWidth) {
                if ((data(FontData.fontHeight - 1 - i) & (1 << (FontData.fontWidth - 1 - j))) != 0) {
                    val xPos = (x + j) / windowWidth_ * 2 - 1
                    val yPos = (y + i) / windowHeight_ * 2 - 1
                    GL11.glVertex2f(xPos, yPos)
                    GL11.glVertex2f(xPos + 2.0f / windowWidth_, yPos)
                    GL11.glVertex2f(xPos + 2.0f / windowWidth_, yPos + 2.0f / windowHeight_)
                    GL11.glVertex2f(xPos, yPos + 2.0f / windowHeight_)
                }
            }
        }
        GL11.glEnd()
    }

    override def render(): Renderable = {
        for (i <- str_.indices) {
            drawChar(x_ + i * FontData.fontWidth, y_, str_(i), windowWidth_, windowHeight_)
        }

        this
    }
}