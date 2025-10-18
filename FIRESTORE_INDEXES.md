# Índices Necessários do Firestore

## ⚠️ IMPORTANTE

Para garantir performance e evitar erros, os seguintes índices compostos devem ser criados no Firestore Console.

## 📋 Índices Compostos Necessários

### 1. Notificações - Busca por usuário e ordenação
**Coleção:** `notifications`
**Campos:**
- `userId` (Ascending)
- `createdAt` (Descending)

**Console Command:**
```bash
gcloud firestore indexes composite create \
  --collection-group=notifications \
  --field-config field-path=userId,order=ascending \
  --field-config field-path=createdAt,order=descending
```

### 2. Notificações - Busca de solicitações pendentes
**Coleção:** `notifications`
**Campos:**
- `organizationId` (Ascending)
- `relatedUserId` (Ascending)
- `type` (Ascending)
- `isRead` (Ascending)

**Console Command:**
```bash
gcloud firestore indexes composite create \
  --collection-group=notifications \
  --field-config field-path=organizationId,order=ascending \
  --field-config field-path=relatedUserId,order=ascending \
  --field-config field-path=type,order=ascending \
  --field-config field-path=isRead,order=ascending
```

### 3. User Organizations - Busca de membros ativos
**Coleção:** `user_organizations`
**Campos:**
- `userId` (Ascending)
- `isActive` (Ascending)

**Console Command:**
```bash
gcloud firestore indexes composite create \
  --collection-group=user_organizations \
  --field-config field-path=userId,order=ascending \
  --field-config field-path=isActive,order=ascending
```

### 4. User Organizations - Contagem de membros
**Coleção:** `user_organizations`
**Campos:**
- `organizationId` (Ascending)
- `isActive` (Ascending)

**Console Command:**
```bash
gcloud firestore indexes composite create \
  --collection-group=user_organizations \
  --field-config field-path=organizationId,order=ascending \
  --field-config field-path=isActive,order=ascending
```

## 🔧 Como Criar os Índices

### Opção 1: Automático (Recomendado)
1. Execute o app
2. Quando ocorrer um erro de índice faltando, clique no link fornecido no logcat
3. O Firebase Console criará o índice automaticamente

### Opção 2: Manual
1. Acesse [Firebase Console](https://console.firebase.google.com/)
2. Selecione seu projeto
3. Vá em "Firestore Database" > "Indexes"
4. Clique em "Create Index"
5. Adicione os campos conforme especificado acima

### Opção 3: CLI
Execute os comandos acima usando o gcloud CLI

## 📊 Índices Simples (Criados Automaticamente)

Os seguintes índices simples são criados automaticamente pelo Firestore:
- `organizations.code`
- `organizations.ownerId`
- `user_organizations.userId`
- `user_organizations.organizationId`
- `notifications.userId`
- `notifications.type`

## ⚡ Performance Tips

1. **Evite queries complexas**: Quanto mais campos no índice, mais lenta a escrita
2. **Use `limit()`**: Sempre limite o número de resultados em queries
3. **Cache local**: O Firestore automaticamente cacheia dados localmente
4. **Paginação**: Para listas grandes, use paginação com `startAfter()`

## 🛡️ Segurança

Os índices não afetam as regras de segurança. As Firestore Rules são aplicadas **antes** da query ser executada, garantindo que apenas dados autorizados sejam acessados.

## 📝 Notas

- Índices levam alguns minutos para serem criados
- Não delete índices sem verificar se estão em uso
- Monitore o uso de índices no Firebase Console
