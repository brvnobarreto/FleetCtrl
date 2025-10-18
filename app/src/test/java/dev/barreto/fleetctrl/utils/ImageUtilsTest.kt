package dev.barreto.fleetctrl.utils

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test

class ImageUtilsTest {

    @Test
    fun `when imageToBase64 is called with non-existent file, should return null`() {
        // Arrange
        val nonExistentPath = "/path/to/non/existent/image.jpg"
        
        // Act
        val result = ImageUtils.imageToBase64(nonExistentPath)
        
        // Assert
        assertNull(result)
    }

    @Test
    fun `when base64ToBitmap is called with invalid base64, should return null`() {
        // Arrange
        val invalidBase64 = "invalid_base64_string"
        
        // Act
        val result = ImageUtils.base64ToBitmap(invalidBase64)
        
        // Assert
        assertNull(result)
    }

    @Test
    fun `when base64ToBitmap is called with empty string, should return null`() {
        // Arrange
        val emptyBase64 = ""
        
        // Act
        val result = ImageUtils.base64ToBitmap(emptyBase64)
        
        // Assert
        assertNull(result)
    }

    @Test
    fun `when base64ToBitmap is called with malformed base64, should return null`() {
        // Arrange
        val malformedBase64 = "data:image/jpeg;base64,invalid_base64_content"
        
        // Act
        val result = ImageUtils.base64ToBitmap(malformedBase64)
        
        // Assert
        assertNull(result)
    }

    @Test
    fun `when base64ToBitmap is called with null string, should return null`() {
        // Arrange
        val nullBase64: String? = null
        
        // Act
        val result = ImageUtils.base64ToBitmap(nullBase64 ?: "")
        
        // Assert
        assertNull(result)
    }

    @Test
    fun `imageToBase64 should handle empty file path`() {
        // Arrange
        val emptyPath = ""
        
        // Act
        val result = ImageUtils.imageToBase64(emptyPath)
        
        // Assert
        assertNull(result)
    }

    @Test
    fun `imageToBase64 should handle null file path`() {
        // Arrange
        val nullPath: String? = null
        
        // Act
        val result = ImageUtils.imageToBase64(nullPath ?: "")
        
        // Assert
        assertNull(result)
    }

    @Test
    fun `imageToBase64 should handle invalid file extension`() {
        // Arrange
        val invalidPath = "/path/to/file.txt"
        
        // Act
        val result = ImageUtils.imageToBase64(invalidPath)
        
        // Assert
        assertNull(result)
    }

    @Test
    fun `imageToBase64 should handle very long file path`() {
        // Arrange
        val longPath = "/very/long/path/that/does/not/exist/and/is/very/long/image.jpg"
        
        // Act
        val result = ImageUtils.imageToBase64(longPath)
        
        // Assert
        assertNull(result)
    }
}
