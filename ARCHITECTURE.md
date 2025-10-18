# 🏗️ Arquitetura do Sistema de Organizações

## 📐 Visão Geral

A arquitetura foi projetada para ser **robusta**, **escalável** e **fácil de manter**, seguindo princípios SOLID e padrões de design modernos.

## 🎯 Hierarquia de Dados

```
Organization (Organização)
    ├── id: String (UUID)
    ├── code: String (6 caracteres únicos)
    ├── name: String
    ├── description: String
    ├── ownerId: String
    ├── requiresApproval: Boolean
    └── Members (Membros via user_organizations)
        └── Vehicles (Veículos)
            ├── Fuel Records (Abastecimentos)
            ├── Activity Records (Atividades)
            └── Maintenance Records (Manutenções)
```

## 🔄 Fluxo de Entrada em Organização

### Entrada Direta (requiresApproval = false)
```
1. User solicita entrada com código
2. Sistema valida código e usuário
3. Cria vínculo user_organizations
4. User tem acesso imediato
```

### Entrada com Aprovação (requiresApproval = true)
```
1. User solicita entrada com código
2. Sistema cria notificação JOIN_REQUEST para owner
3. Owner recebe notificação
4. Owner aprova ou rejeita
5. Sistema cria vínculo (se aprovado)
6. Sistema envia notificação de resultado para user
```

## 🛡️ Camadas de Segurança

### 1. Firestore Rules (Primeira Linha de Defesa)
- Validações no servidor
- Impossível burlar via cliente
- Controle granular de permissões

### 2. Repository (Segunda Linha de Defesa)
- Validações de input
- Verificações de duplicação
- Limites e quotas
- Transações atômicas

### 3. ViewModel (Terceira Linha de Defesa)
- Autenticação de usuário
- Estados da UI
- Tratamento de erros

## 📊 Estrutura de Dados

### Organizations
```kotlin
{
  id: String,              // UUID único
  code: String,            // Código de 6 caracteres (ex: "ABC123")
  name: String,            // Nome da organização
  description: String,     // Descrição
  ownerId: String,         // UID do owner
  ownerEmail: String,      // Email do owner
  isActive: Boolean,       // Status da organização
  maxMembers: Int?,        // Limite de membros (null = ilimitado)
  requiresApproval: Boolean, // Se requer aprovação manual
  createdAt: Date,         // Data de criação
  updatedAt: Date          // Última atualização
}
```

### User_Organizations (Vínculos)
```kotlin
{
  userId: String,          // UID do usuário
  organizationId: String,  // ID da organização
  role: String,            // "owner" | "editor" | "viewer"
  isActive: Boolean,       // Status do vínculo
  joinedAt: Date          // Data de entrada
}
```

### Notifications
```kotlin
{
  id: String,              // ID único
  userId: String,          // Destinatário
  organizationId: String,  // Organização relacionada
  type: String,            // "JOIN_REQUEST" | "JOIN_APPROVED" | "JOIN_REJECTED"
  title: String,           // Título
  message: String,         // Mensagem
  relatedUserId: String,   // Usuário relacionado (solicitante/aprovador)
  isRead: Boolean,         // Se foi lida
  isActionable: Boolean,   // Se requer ação
  createdAt: Date         // Data de criação
}
```

## 🔐 Permissões por Role

### Owner (Dono)
- Criar/editar/deletar organização
- Adicionar/remover membros
- Aprovar/rejeitar solicitações
- Alterar roles de membros
- Todas as permissões de Editor

### Editor
- Criar/editar/deletar veículos
- Criar/editar/deletar registros (fuel, activity, maintenance)
- Ver todos os dados da organização

### Viewer (Padrão)
- Ver todos os dados da organização
- Não pode criar ou editar nada

## 🚀 Escalabilidade

### Horizontal Scaling
- Firestore escala automaticamente
- Sem limite de leitura/escrita simultâneas
- Sharding automático de dados

### Vertical Scaling
- Cache local automático
- Queries otimizadas com índices
- Paginação em listas grandes

### Performance Optimizations
1. **Índices Compostos**: Queries complexas otimizadas
2. **Limit Queries**: Sempre limitamos resultados
3. **Local Cache**: Firestore cacheia dados automaticamente
4. **Batch Operations**: Operações em lote quando possível

## 🐛 Robustez e Confiabilidade

### Validações em Múltiplas Camadas
```
Input → Repository → Firestore Rules → Database
  ✓        ✓              ✓              ✓
```

### Tratamento de Erros
- Try-catch em todas as operações
- Mensagens de erro claras e específicas
- Logs detalhados para debugging
- Rollback automático em falhas

### Prevenção de Bugs Comuns
1. ✅ **Duplicação**: Verifica se user já é membro
2. ✅ **Solicitações Pendentes**: Verifica se já existe solicitação
3. ✅ **Limites**: Respeita maxMembers
4. ✅ **Códigos Únicos**: Garante unicidade do código
5. ✅ **Validação de Input**: Sanitiza todos os inputs
6. ✅ **Estados Inválidos**: Valida estado antes de operações

## 🔄 Padrões de Design

### Repository Pattern
- Abstrai fonte de dados (Firestore)
- Facilita testes unitários
- Centraliza lógica de dados

### MVVM (Model-View-ViewModel)
- Separação de concerns
- UI reativa com StateFlow
- Facilita manutenção

### Single Source of Truth
- Firestore é a fonte única de verdade
- Dados locais são cache
- Sincronização automática

## 📱 Integração com Room (Futuro)

```kotlin
// Modelo Room (Cache Local)
@Entity(tableName = "organizations_cache")
data class OrganizationCache(
    @PrimaryKey val id: String,
    val data: String, // JSON serializado
    val cachedAt: Long
)
```

## 🎨 Melhores Práticas Implementadas

1. ✅ **Princípio da Responsabilidade Única**: Cada classe tem uma responsabilidade
2. ✅ **Dependency Injection**: Hilt para DI
3. ✅ **Imutabilidade**: Data classes com val
4. ✅ **Null Safety**: Kotlin null safety
5. ✅ **Coroutines**: Async/await para operações de rede
6. ✅ **StateFlow**: Estados reativos
7. ✅ **Documentação**: Código bem documentado

## 🧪 Testabilidade

### Unit Tests
```kotlin
// Repository é facilmente testável
@Test
fun `createOrganization should throw when name is blank`() = runTest {
    val repository = OrganizationRepositoryV2(mockFirestore, mockAuth)
    
    assertThrows<IllegalArgumentException> {
        repository.createOrganization(
            name = "",
            description = "Test",
            ownerId = "user1",
            ownerEmail = "test@test.com"
        )
    }
}
```

### Integration Tests
- Testa fluxo completo com Firestore Emulator
- Valida regras de segurança
- Testa cenários de erro

## 📈 Monitoramento

### Logs
- DEBUG: Operações normais
- ERROR: Falhas e exceções
- Timestamps em todas as operações

### Métricas (Firebase Analytics)
- Organizações criadas
- Solicitações enviadas/aprovadas/rejeitadas
- Tempo médio de aprovação
- Taxa de rejeição

## 🔮 Futuras Melhorias

1. **Cache Offline**: Room para cache local robusto
2. **Sincronização**: Sync automático em background
3. **Roles Customizáveis**: Permitir criar roles personalizados
4. **Auditoria**: Log de todas as ações (quem fez o quê e quando)
5. **Convites por Email**: Enviar convite via email além do código
6. **Organizações Privadas**: Organizações não descobríveis
7. **Analytics**: Dashboard de métricas da organização
