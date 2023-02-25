package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

private const val BYTE_LENGTH = 8
private val endBytes = String(arrayOf(0, 0, 3).map { it.toChar() }.toCharArray())

fun main() {
    do {
        var task = getInputString("Task (hide, show, exit):")
        println(
            when (task) {
                "exit" -> "Bye!"
                "show" -> showMessage()
                "hide" -> hideMessage()
                else -> "Wrong task: $task"
            }
        )
    } while (task != "exit")
}

private fun getInputString(message: String): String {
    println(message)
    return readln()
}

private fun showMessage(): String {
    val inputImageFile = getInputString("Input image file:")
    val password = getInputString("Password:")
    return try {
        "Message:\n${processShow(inputImageFile, password)}"
    } catch (e: IOException) {
        "Can't read input file!"
    }
}

private fun processShow(inputImageFile: String, password: String): String {
    val inputImage = ImageIO.read(File(inputImageFile))
    val byteArray = mutableListOf<Int>()
    var i = 0
    var currentByte = 0
    for (y in 0 until inputImage.height) {
        for (x in 0 until inputImage.width) {
            val bit = Color(inputImage.getRGB(x, y)).blue and 1
            currentByte = currentByte or (bit shl getBitPosition(i))
            i++
            if (i % BYTE_LENGTH == 0) {
                byteArray.add(currentByte)
                currentByte = 0
                if (isMessageReady(byteArray)) {
                    val message = String(byteArray.map { it.toChar() }.toCharArray()).substring(
                        0,
                        byteArray.size - 3
                    )
                    return transformMessage(message, password)
                }
            }
        }
    }
    return ""
}

private fun isMessageReady(byteArray: List<Int>) =
    String(byteArray.map { it.toChar() }.toCharArray()).endsWith(endBytes);

private fun hideMessage(): String {
    val inputImageFile = getInputString("Input image file:")
    val outputImageFile = getInputString("Output image file:")
    val message = getInputString("Message to hide:")
    val password = getInputString("Password:")
    return try {
        processHide(inputImageFile, outputImageFile, transformMessage(message, password))
    } catch (e: IOException) {
        "Can't read input file!"
    }
}

fun transformMessage(message: String, password: String): String {
    val messageBytes = message.toByteArray(Charsets.UTF_8)
    val passwordBytes = password.toByteArray(Charsets.UTF_8)
    val outputBytes = mutableListOf<Int>()
    for ((i, c) in messageBytes.withIndex()) {
        outputBytes.add(c.toInt() xor passwordBytes[i % passwordBytes.size].toInt())
    }
    return String(outputBytes.map { it.toChar() }.toCharArray())
}

private fun processHide(
    inputImageFile: String,
    outputImageFile: String,
    message: String
): String {
    val inputImage = ImageIO.read(File(inputImageFile))
    val bytes = (message + endBytes).toByteArray(Charsets.UTF_8)
    if (bytes.size * 8 > inputImage.width * inputImage.height) {
        return "The input image is not large enough to hold this message."
    }
    println("Input Image: $inputImageFile")
    println("Output Image: $outputImageFile")
    processImage(inputImage, bytes, outputImageFile)
    return "Message saved in $outputImageFile image."
}

private fun processImage(
    inputImage: BufferedImage,
    bytes: ByteArray,
    outputImageFile: String
) {
    val outputImage = BufferedImage(inputImage.width, inputImage.height, inputImage.type)
    var i = 0
    for (y in 0 until inputImage.height) {
        for (x in 0 until inputImage.width) {
            processPixel(inputImage, x, y, bytes, i, outputImage)
            i++
        }
    }
    ImageIO.write(outputImage, "png", File(outputImageFile))
}

private fun processPixel(
    inputImage: BufferedImage,
    x: Int,
    y: Int,
    bytes: ByteArray,
    i: Int,
    outputImage: BufferedImage
) {
    val inputColor = Color(inputImage.getRGB(x, y))
    val outputColor = Color(
        inputColor.red,
        inputColor.green,
        getBlue(inputColor.blue, bytes, i)
    )
    outputImage.setRGB(x, y, outputColor.rgb)
}

private fun getBlue(blue: Int, messageBytes: ByteArray, i: Int) =
    if (getByteIndex(i) < messageBytes.size) {
        val byte = messageBytes[getByteIndex(i)]
        val bit = byte.toInt() shr getBitPosition(i) and 1
        if (bit == 1) blue or 1 else blue and 254
    } else {
        blue
    }

private fun getByteIndex(i: Int) = i / BYTE_LENGTH

private fun getBitPosition(i: Int) = 7 - i % BYTE_LENGTH