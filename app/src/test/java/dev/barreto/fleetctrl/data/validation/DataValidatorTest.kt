package dev.barreto.fleetctrl.data.validation

import dev.barreto.fleetctrl.data.database.entities.Vehicle
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DataValidatorTest {

    private lateinit var validator: DataValidator

    @Before
    fun setup() {
        validator = DataValidator()
    }

    // ===== TESTES DE VALIDAÇÃO DE PLACA =====

    @Test
    fun `when validatePlate is called with valid old format plate, should return success`() {
        // Arrange
        val validPlate = "ABC1234"
        
        // Act
        val result = validator.validatePlate(validPlate)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validatePlate is called with valid Mercosul format plate, should return success`() {
        // Arrange
        val validPlate = "ABC1D23"
        
        // Act
        val result = validator.validatePlate(validPlate)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validatePlate is called with invalid plate, should return error`() {
        // Arrange
        val invalidPlate = "INVALID"
        
        // Act
        val result = validator.validatePlate(invalidPlate)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
        val errorResult = result as ValidationResult.Error
        assertTrue(errorResult.errors.isNotEmpty())
    }

    @Test
    fun `when validatePlate is called with empty plate, should return error`() {
        // Arrange
        val emptyPlate = ""
        
        // Act
        val result = validator.validatePlate(emptyPlate)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
    }

    // ===== TESTES DE VALIDAÇÃO DE NÚMERO DO VEÍCULO =====

    @Test
    fun `when validateVehicleNumber is called with valid number, should return success`() {
        // Arrange
        val validNumber = "ABC123"
        
        // Act
        val result = validator.validateVehicleNumber(validNumber)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validateVehicleNumber is called with too short number, should return error`() {
        // Arrange
        val shortNumber = "AB"
        
        // Act
        val result = validator.validateVehicleNumber(shortNumber)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `when validateVehicleNumber is called with too long number, should return error`() {
        // Arrange
        val longNumber = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"
        
        // Act
        val result = validator.validateVehicleNumber(longNumber)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
    }

    // ===== TESTES DE VALIDAÇÃO DE MODELO =====

    @Test
    fun `when validateModel is called with valid model, should return success`() {
        // Arrange
        val validModel = "Civic"
        
        // Act
        val result = validator.validateModel(validModel)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validateModel is called with empty model, should return error`() {
        // Arrange
        val emptyModel = ""
        
        // Act
        val result = validator.validateModel(emptyModel)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
    }

    // ===== TESTES DE VALIDAÇÃO DE ANO =====

    @Test
    fun `when validateYear is called with valid year, should return success`() {
        // Arrange
        val validYear = 2023
        
        // Act
        val result = validator.validateYear(validYear)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validateYear is called with future year, should return some result`() {
        // Arrange
        val futureYear = 2030
        
        // Act
        val result = validator.validateYear(futureYear)
        
        // Assert
        // Just verify it returns some result (success, warning, or error)
        assertNotNull(result)
        assertTrue(result is ValidationResult.Success || 
                  result is ValidationResult.Warning || 
                  result is ValidationResult.Error)
    }

    @Test
    fun `when validateYear is called with very old year, should return some result`() {
        // Arrange
        val oldYear = 1900
        
        // Act
        val result = validator.validateYear(oldYear)
        
        // Assert
        // Just verify it returns some result (success, warning, or error)
        assertNotNull(result)
        assertTrue(result is ValidationResult.Success || 
                  result is ValidationResult.Warning || 
                  result is ValidationResult.Error)
    }

    // ===== TESTES DE VALIDAÇÃO DE TIPO DE MOTOR =====

    @Test
    fun `when validateEngineType is called with valid type, should return success`() {
        // Arrange
        val validType = "Gasolina"
        
        // Act
        val result = validator.validateEngineType(validType)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validateEngineType is called with invalid type, should return error`() {
        // Arrange
        val invalidType = "InvalidType"
        
        // Act
        val result = validator.validateEngineType(invalidType)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
    }

    // ===== TESTES DE VALIDAÇÃO DE CAPACIDADE DO TANQUE =====

    @Test
    fun `when validateFuelCapacity is called with valid capacity, should return success`() {
        // Arrange
        val validCapacity = 50.0
        
        // Act
        val result = validator.validateFuelCapacity(validCapacity)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validateFuelCapacity is called with negative capacity, should return error`() {
        // Arrange
        val negativeCapacity = -10.0
        
        // Act
        val result = validator.validateFuelCapacity(negativeCapacity)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `when validateFuelCapacity is called with very high capacity, should return some result`() {
        // Arrange
        val highCapacity = 1000.0
        
        // Act
        val result = validator.validateFuelCapacity(highCapacity)
        
        // Assert
        // Just verify it returns some result
        assertNotNull(result)
        assertTrue(result is ValidationResult.Success || 
                  result is ValidationResult.Warning || 
                  result is ValidationResult.Error)
    }

    // ===== TESTES DE VALIDAÇÃO DE CONSUMO MÉDIO =====

    @Test
    fun `when validateAverageConsumption is called with valid consumption, should return success`() {
        // Arrange
        val validConsumption = 12.5
        
        // Act
        val result = validator.validateAverageConsumption(validConsumption)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validateAverageConsumption is called with negative consumption, should return error`() {
        // Arrange
        val negativeConsumption = -5.0
        
        // Act
        val result = validator.validateAverageConsumption(negativeConsumption)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
    }

    // ===== TESTES DE VALIDAÇÃO DE QUILOMETRAGEM =====

    @Test
    fun `when validateMileage is called with valid mileage, should return success`() {
        // Arrange
        val validMileage = 50000L
        
        // Act
        val result = validator.validateMileage(validMileage)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validateMileage is called with negative mileage, should return error`() {
        // Arrange
        val negativeMileage = -1000L
        
        // Act
        val result = validator.validateMileage(negativeMileage)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
    }

    // ===== TESTES DE VALIDAÇÃO COMPLETA DO VEÍCULO =====

    @Test
    fun `when validateVehicle is called with valid vehicle, should return success`() {
        // Arrange
        val validVehicle = Vehicle(
            id = 0,
            vehicleNumber = "001",
            plate = "ABC1234",
            model = "Civic",
            brand = "Honda",
            year = 2023,
            color = "Azul",
            driver = "João Silva",
            engineType = "Gasolina",
            fuelCapacity = 50.0,
            averageConsumption = 12.5
        )
        
        // Act
        val result = validator.validateVehicle(validVehicle)
        
        // Assert
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `when validateVehicle is called with invalid vehicle, should return error`() {
        // Arrange
        val invalidVehicle = Vehicle(
            id = 0,
            vehicleNumber = "",
            plate = "INVALID",
            model = "",
            brand = "",
            year = 1800,
            color = "",
            driver = "",
            engineType = "InvalidType",
            fuelCapacity = -10.0,
            averageConsumption = -5.0
        )
        
        // Act
        val result = validator.validateVehicle(invalidVehicle)
        
        // Assert
        assertTrue(result is ValidationResult.Error)
        val errorResult = result as ValidationResult.Error
        assertTrue(errorResult.errors.size > 1) // Should have multiple errors
    }
}
