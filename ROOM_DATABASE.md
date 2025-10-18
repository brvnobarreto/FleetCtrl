# 🗄️ Room Database - FleetCtrl

## 📋 Visão Geral

Implementação robusta e à prova de futuro do Room Database para o aplicativo FleetCtrl, seguindo as melhores práticas de arquitetura Android.

## 🏗️ Arquitetura

### **Padrões Utilizados:**
- **Repository Pattern**: Abstração do acesso aos dados
- **Dependency Injection**: Hilt para injeção de dependência
- **MVVM**: ViewModels para gerenciamento de estado
- **Clean Architecture**: Separação de responsabilidades

### **Estrutura de Pastas:**
```
data/
├── database/
│   ├── entities/          # Entidades do banco
│   ├── daos/             # Data Access Objects
│   ├── converters/       # Conversores de tipos
│   ├── migrations/       # Migrações do banco
│   └── AppDatabase.kt    # Configuração principal
└── repositories/         # Repositories
```

## 🗃️ Entidades

### **1. Vehicle (Frota de Veículos)**
- **Campos**: ID, placa, modelo, marca, ano, cor, tipo de motor, capacidade do tanque, consumo médio
- **Relacionamentos**: One-to-Many com FuelRecord, MaintenanceRecord, DiaryEntry
- **Índices**: Placa (única), marca, modelo, ano

### **2. FuelRecord (Abastecimentos)**
- **Campos**: ID, veículo, data, tipo de combustível, quantidade, preço por litro, custo total
- **Relacionamentos**: Many-to-One com Vehicle
- **Índices**: Veículo, data, tipo de combustível

### **3. MaintenanceRecord (Manutenções)**
- **Campos**: ID, veículo, data, tipo, descrição, quilometragem, custos, oficina
- **Relacionamentos**: Many-to-One com Vehicle
- **Índices**: Veículo, data, tipo, oficina

### **4. DiaryEntry (Diário de Bordo)**
- **Campos**: ID, veículo, data, tipo, título, descrição, localização, distância, duração
- **Relacionamentos**: Many-to-One com Vehicle
- **Índices**: Veículo, data, tipo, prioridade

## 🔧 DAOs (Data Access Objects)

### **Operações Implementadas:**
- **CRUD Básico**: Create, Read, Update, Delete
- **Busca Avançada**: Pesquisa por texto, filtros múltiplos
- **Estatísticas**: Contagens, médias, totais, extremos
- **Relacionamentos**: Consultas com joins implícitos
- **Paginação**: Limite de resultados
- **Ordenação**: Múltiplos critérios

### **Recursos Especiais:**
- **Flow Support**: Observação reativa de mudanças
- **Coroutines**: Operações assíncronas
- **Validação**: Verificação de integridade
- **Performance**: Índices otimizados

## 🏪 Repositories

### **Padrão Repository:**
- **Abstração**: Interface entre ViewModels e DAOs
- **Validação**: Regras de negócio centralizadas
- **Transformação**: Conversão de dados quando necessário
- **Cache**: Gerenciamento de estado local

### **Repositories Disponíveis:**
- `VehicleRepository`: Gerenciamento de veículos
- `FuelRecordRepository`: Registros de abastecimento
- `MaintenanceRecordRepository`: Registros de manutenção
- `DiaryEntryRepository`: Entradas do diário
- `FleetRepository`: Agregação de todos os repositories

## 🔄 Migrações

### **Sistema de Migração:**
- **Versionamento**: Controle de versões do banco
- **Migrações Incrementais**: Mudanças graduais
- **Rollback**: Possibilidade de reversão
- **Validação**: Verificação de integridade

### **Exemplo de Migração:**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE vehicles ADD COLUMN newField TEXT")
    }
}
```

## 🎯 ViewModels

### **Integração com Room:**
- **StateFlow**: Estado reativo da UI
- **Coroutines**: Operações assíncronas
- **Error Handling**: Tratamento de erros
- **Loading States**: Estados de carregamento

### **Exemplo de Uso:**
```kotlin
@HiltViewModel
class FleetViewModel @Inject constructor(
    private val fleetRepository: FleetRepository
) : ViewModel() {
    
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()
    
    fun loadVehicles() {
        viewModelScope.launch {
            fleetRepository.vehicleRepository.getAllVehicles().collect { vehicleList ->
                _vehicles.value = vehicleList
            }
        }
    }
}
```

## 🔧 Configuração

### **Dependências:**
```kotlin
// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Hilt Dependency Injection
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")

// Desugaring para LocalDateTime
coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
```

### **Configuração do Banco:**
```kotlin
@Database(
    entities = [Vehicle::class, FuelRecord::class, MaintenanceRecord::class, DiaryEntry::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
```

## 🚀 Recursos Avançados

### **1. Type Converters:**
- **LocalDateTime**: Conversão para String
- **BigDecimal**: Conversão para String
- **Enums**: Conversão para String

### **2. Validação de Dados:**
- **Regras de Negócio**: Validação centralizada
- **Integridade**: Verificação de relacionamentos
- **Consistência**: Validação de cálculos

### **3. Performance:**
- **Índices**: Otimização de consultas
- **Lazy Loading**: Carregamento sob demanda
- **Pagination**: Paginação de resultados

### **4. Testes:**
- **Test Database**: Banco em memória para testes
- **Mocking**: Simulação de dependências
- **Unit Tests**: Testes unitários isolados

## 📊 Estatísticas e Relatórios

### **Métricas Disponíveis:**
- **Frota**: Total de veículos, veículos ativos, quilometragem média
- **Abastecimento**: Consumo médio, custo total, preço médio
- **Manutenção**: Custo total por tipo, frequência, próximas manutenções
- **Diário**: Distância total, duração média, eficiência

### **Consultas Complexas:**
- **Relatórios**: Agregações por período
- **Tendências**: Análise temporal
- **Comparações**: Análise comparativa
- **Alertas**: Notificações automáticas

## 🔒 Segurança e Integridade

### **Validação de Dados:**
- **Constraints**: Restrições de banco
- **Foreign Keys**: Integridade referencial
- **Unique Constraints**: Unicidade de dados
- **Check Constraints**: Validação de valores

### **Backup e Recuperação:**
- **Export Schema**: Exportação de esquema
- **Data Export**: Exportação de dados
- **Migration Scripts**: Scripts de migração
- **Rollback Plans**: Planos de reversão

## 🎯 Próximos Passos

### **Melhorias Futuras:**
1. **Sync com Servidor**: Sincronização remota
2. **Offline Support**: Suporte offline completo
3. **Data Encryption**: Criptografia de dados sensíveis
4. **Analytics**: Análise de uso e performance
5. **Backup Automático**: Backup automático de dados

### **Otimizações:**
1. **Query Optimization**: Otimização de consultas
2. **Index Tuning**: Ajuste de índices
3. **Memory Management**: Gerenciamento de memória
4. **Cache Strategy**: Estratégia de cache

## 📚 Documentação Adicional

- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://dagger.dev/hilt/)
- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

**Implementado com ❤️ para o FleetCtrl**