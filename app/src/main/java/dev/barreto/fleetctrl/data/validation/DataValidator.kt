package dev.barreto.fleetctrl.data.validation

import dev.barreto.fleetctrl.data.database.entities.Vehicle
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validador robusto de dados com sanitização
 */
@Singleton
class DataValidator @Inject constructor() {

    // Regex para validação de placa brasileira (formato antigo e Mercosul)
    private val plateRegex = Pattern.compile("^[A-Z]{3}[0-9]{4}$|^[A-Z]{3}[0-9][A-Z][0-9]{2}$")
    
    // Regex para validação de número do veículo (alfanumérico)
    private val vehicleNumberRegex = Pattern.compile("^[A-Z0-9]{3,20}$")
    
    // Regex para validação de nome (apenas letras, espaços e acentos)
    private val nameRegex = Pattern.compile("^[a-zA-ZÀ-ÿ\\s]{2,50}$")
    
    // Regex para validação de modelo/marca (alfanumérico com espaços)
    private val modelRegex = Pattern.compile("^[a-zA-Z0-9À-ÿ\\s\\-]{2,50}$")

    /**
     * Valida e sanitiza um veículo completo
     */
    fun validateVehicle(vehicle: Vehicle): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validação do número do veículo
        when (val numberResult = validateVehicleNumber(vehicle.vehicleNumber)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(numberResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(numberResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação da placa
        when (val plateResult = validatePlate(vehicle.plate)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(plateResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(plateResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação do modelo
        when (val modelResult = validateModel(vehicle.model)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(modelResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(modelResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação do condutor
        when (val driverResult = validateDriver(vehicle.driver)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(driverResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(driverResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação do ano
        when (val yearResult = validateYear(vehicle.year)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(yearResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(yearResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação da marca
        when (val brandResult = validateBrand(vehicle.brand)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(brandResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(brandResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação da cor
        when (val colorResult = validateColor(vehicle.color)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(colorResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(colorResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação do tipo de motor
        when (val engineResult = validateEngineType(vehicle.engineType)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(engineResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(engineResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação da capacidade do tanque
        when (val capacityResult = validateFuelCapacity(vehicle.fuelCapacity)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(capacityResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(capacityResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação do consumo médio
        when (val consumptionResult = validateAverageConsumption(vehicle.averageConsumption)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(consumptionResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(consumptionResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        // Validação da quilometragem
        when (val mileageResult = validateMileage(vehicle.currentMileage)) {
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Error -> errors.addAll(mileageResult.errors)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Warning -> warnings.addAll(mileageResult.warnings)
            is dev.barreto.fleetctrl.data.validation.ValidationResult.Success -> { /* Nada a fazer */ }
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida número do veículo
     */
    fun validateVehicleNumber(number: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (number.isBlank()) {
            errors.add("Número do veículo é obrigatório")
            return ValidationResult.Error(errors)
        }

        val sanitized = sanitizeAlphanumeric(number.uppercase())
        
        if (!vehicleNumberRegex.matcher(sanitized).matches()) {
            errors.add("Número do veículo deve conter apenas letras e números (3-20 caracteres)")
        }

        if (number != sanitized) {
            warnings.add("Número do veículo foi sanitizado: '$number' → '$sanitized'")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida placa do veículo
     */
    fun validatePlate(plate: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (plate.isBlank()) {
            errors.add("Placa é obrigatória")
            return ValidationResult.Error(errors)
        }

        val sanitized = sanitizePlate(plate.uppercase())
        
        if (!plateRegex.matcher(sanitized).matches()) {
            errors.add("Formato de placa inválido. Use formato antigo (ABC1234) ou Mercosul (ABC1D23)")
        }

        if (plate != sanitized) {
            warnings.add("Placa foi sanitizada: '$plate' → '$sanitized'")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida modelo do veículo
     */
    fun validateModel(model: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (model.isBlank()) {
            errors.add("Modelo é obrigatório")
            return ValidationResult.Error(errors)
        }

        val sanitized = sanitizeText(model.trim())
        
        if (!modelRegex.matcher(sanitized).matches()) {
            errors.add("Modelo contém caracteres inválidos")
        }

        if (model != sanitized) {
            warnings.add("Modelo foi sanitizado: '$model' → '$sanitized'")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida nome do condutor
     */
    fun validateDriver(driver: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (driver.isBlank()) {
            errors.add("Condutor é obrigatório")
            return ValidationResult.Error(errors)
        }

        val sanitized = sanitizeName(driver.trim())
        
        if (!nameRegex.matcher(sanitized).matches()) {
            errors.add("Nome do condutor contém caracteres inválidos")
        }

        if (driver != sanitized) {
            warnings.add("Nome do condutor foi sanitizado: '$driver' → '$sanitized'")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida ano do veículo
     */
    fun validateYear(year: Int): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        if (year < 1900) {
            errors.add("Ano deve ser maior que 1900")
        }
        
        if (year > currentYear + 1) {
            errors.add("Ano não pode ser maior que ${currentYear + 1}")
        }
        
        if (year < 2000) {
            warnings.add("Ano muito antigo: $year")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida marca do veículo
     */
    fun validateBrand(brand: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (brand.isBlank()) {
            errors.add("Marca é obrigatória")
            return ValidationResult.Error(errors)
        }

        val sanitized = sanitizeText(brand.trim())
        
        if (!modelRegex.matcher(sanitized).matches()) {
            errors.add("Marca contém caracteres inválidos")
        }

        if (brand != sanitized) {
            warnings.add("Marca foi sanitizada: '$brand' → '$sanitized'")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida cor do veículo
     */
    fun validateColor(color: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (color.isBlank()) {
            errors.add("Cor é obrigatória")
            return ValidationResult.Error(errors)
        }

        val sanitized = sanitizeText(color.trim())
        
        if (!modelRegex.matcher(sanitized).matches()) {
            errors.add("Cor contém caracteres inválidos")
        }

        if (color != sanitized) {
            warnings.add("Cor foi sanitizada: '$color' → '$sanitized'")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida tipo de motor
     */
    fun validateEngineType(engineType: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (engineType.isBlank()) {
            errors.add("Tipo de motor é obrigatório")
            return ValidationResult.Error(errors)
        }

        val validTypes = listOf("Gasolina", "Diesel", "Elétrico", "Híbrido", "Flex", "GNV")
        val sanitized = sanitizeText(engineType.trim())
        
        if (!validTypes.contains(sanitized)) {
            errors.add("Tipo de motor inválido. Use: ${validTypes.joinToString(", ")}")
        }

        if (engineType != sanitized) {
            warnings.add("Tipo de motor foi sanitizado: '$engineType' → '$sanitized'")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida capacidade do tanque
     */
    fun validateFuelCapacity(capacity: Double): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (capacity <= 0) {
            errors.add("Capacidade do tanque deve ser maior que zero")
        }
        
        if (capacity > 200) {
            warnings.add("Capacidade do tanque muito alta: ${capacity}L")
        }
        
        if (capacity < 20) {
            warnings.add("Capacidade do tanque muito baixa: ${capacity}L")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida consumo médio
     */
    fun validateAverageConsumption(consumption: Double): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (consumption <= 0) {
            errors.add("Consumo médio deve ser maior que zero")
        }
        
        if (consumption > 50) {
            warnings.add("Consumo médio muito alto: ${consumption} km/L")
        }
        
        if (consumption < 5) {
            warnings.add("Consumo médio muito baixo: ${consumption} km/L")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    /**
     * Valida quilometragem
     */
    fun validateMileage(mileage: Long): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (mileage < 0) {
            errors.add("Quilometragem não pode ser negativa")
        }
        
        if (mileage > 1_000_000) {
            warnings.add("Quilometragem muito alta: ${mileage} km")
        }

        return when {
            errors.isNotEmpty() -> ValidationResult.Error(errors)
            warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
            else -> ValidationResult.Success
        }
    }

    // Métodos de sanitização

    private fun sanitizeAlphanumeric(input: String): String {
        return input.replace(Regex("[^A-Z0-9]"), "")
    }

    private fun sanitizePlate(input: String): String {
        return input.replace(Regex("[^A-Z0-9]"), "")
    }

    private fun sanitizeText(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9À-ÿ\\s\\-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun sanitizeName(input: String): String {
        return input.replace(Regex("[^a-zA-ZÀ-ÿ\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

/**
 * Resultado de validação
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Warning(val warnings: List<String>) : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
}