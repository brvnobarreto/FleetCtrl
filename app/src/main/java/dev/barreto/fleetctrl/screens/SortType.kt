package dev.barreto.fleetctrl.screens

/**
 * Tipos de ordenação para registros de atividade e abastecimento
 */
enum class SortType(val displayName: String) {
    DATE_DESC("Data (Mais recente)"),
    DATE_ASC("Data (Mais antiga)"),
    DISTANCE_DESC("Distância (Maior)"),
    DISTANCE_ASC("Distância (Menor)"),
    MILEAGE_DESC("Quilometragem (Maior)"),
    MILEAGE_ASC("Quilometragem (Menor)")
}