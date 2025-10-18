package dev.barreto.fleetctrl.viewmodels

import org.junit.Test
import org.junit.Assert.*

/**
 * Teste simples para verificar se a estrutura de testes está funcionando
 */
class SimpleViewModelTest {

    @Test
    fun `simple test should pass`() {
        // Arrange
        val expected = 2
        
        // Act
        val result = 1 + 1
        
        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `another simple test should pass`() {
        // Arrange
        val text = "Hello World"
        
        // Act
        val result = text.length
        
        // Assert
        assertEquals(11, result)
        assertTrue(text.contains("World"))
    }
}
