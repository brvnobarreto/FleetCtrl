package dev.barreto.fleetctrl

import io.mockk.MockKAnnotations
import org.junit.Before

/**
 * Classe base para testes com configurações comuns
 */
abstract class BaseTest {
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }
}
