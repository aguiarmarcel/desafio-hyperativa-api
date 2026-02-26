Segue o README atualizado com a inclusão das três rotas da API, mantendo o tom técnico, direto e justificando decisões arquiteturais:

---

# Hyperativa API – Card Import & Lookup

API para importação em lote de cartões e consulta por PAN, com foco em:

* Idempotência real
* Proteção contra duplicidade
* Alta performance de escrita
* Lookup eficiente
* Armazenamento seguro (hash + criptografia)

---

## 📌 Arquitetura

Arquitetura baseada em separação de responsabilidades:

* **Controller** → camada HTTP
* **UseCase** → regra de negócio
* **Repository** → persistência
* **CryptoService** → isolamento de criptografia
* **Banco** → responsável por garantir unicidade

Essa separação permite:

* Testabilidade
* Evolução independente
* Baixo acoplamento
* Escalabilidade

---

Perfeito! Com base nos seus controllers, dá para criar um **diagrama simples de fluxo** mostrando as rotas e seus fluxos de importação, criação e consulta de cartões, além do login. Vou sugerir um diagrama ASCII técnico e direto, que pode ser colocado no README, mantendo o estilo limpo e explicativo:

---

## 🗂 Fluxo de Rotas – Hyperativa API

```text
           ┌─────────────┐
           │   Cliente   │
           └─────┬───────┘
                 │
                 │ POST /auth
                 │ { login, password }
                 ▼
           ┌─────────────┐
           │ AuthController │
           └─────┬───────┘
                 │
                 │ retorna JWT
                 ▼
           ┌─────────────┐
           │  Cliente   │ (usa token)
           └─────┬───────┘
                 │
      ┌──────────┴───────────┐
      │                      │
      ▼                      ▼
POST /cards               GET /cards?pan=...
{ "pan": "..."}           Retorna ID se existir
      │                      │
      ▼                      ▼
┌─────────────┐          ┌─────────────┐
│ CardController │        │ CardController │
└─────┬───────┘          └─────────────┘
      │
      │
      ▼
RegisterCardUseCase  ← cria cartão
LookupCardUseCase    ← consulta cartão
ImportCardsUseCase   ← processa arquivo CSV/JSON
      │
      ▼
┌─────────────┐
│ CardRepository │
│ Banco (MySQL) │
└─────────────┘
      │
      ├── hash+encrypt PAN
      ├── UNIQUE pan_hash garante idempotência
      └── batch insert / lookup rápido
```

---

### Como o fluxo funciona:

1. **Autenticação (`POST /auth`)**

    * Retorna JWT usado para todas as outras rotas.

2. **Criar cartão (`POST /cards`)**

    * Recebe PAN → hash + encrypt → salva banco
    * Idempotente → evita duplicata

3. **Consultar cartão (`GET /cards?pan=...`)**

    * Busca por `pan_hash`
    * Retorna `id` sem expor PAN

4. **Importar cartões (`POST /cards/import`)**

    * Recebe arquivo CSV/JSON → streaming e batch insert
    * Banco garante unicidade → idempotência real
    * Retorna resumo (inseridos, duplicados, inválidos)

---

## 🔐 Modelo de Persistência

```sql
CREATE TABLE card (
  id binary(16) NOT NULL,
  pan_hash binary(32) NOT NULL,
  pan_enc varbinary(512) NOT NULL,
  pan_iv varbinary(12) NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY pan_hash (pan_hash)
);
```

**Decisões:**

* **id binary(16)** → UUID compacto, melhor performance que varchar(36), índice menor → menos IO
* **pan_hash binary(32)** → SHA-256, usado como chave lógica, permite comparação segura sem expor PAN
* **pan_enc varbinary** → armazena PAN criptografado (AES-GCM), garante confidencialidade, possibilidade de descriptografia futura
* **pan_iv** → necessário para criptografia autenticada (AES-GCM), segurança correta por registro

---

## 🔁 Idempotência Real

Garantida por **UNIQUE KEY pan_hash (pan_hash)**.

Impacto:

* Mesmo cartão nunca será inserido duas vezes
* Reprocessar o mesmo arquivo não cria duplicatas
* Sistema tolerante a reenvio
* Sem depender de lógica na aplicação. O banco é a fonte da verdade

---

## 🚫 Proteção contra duplicidade

Uso de **INSERT IGNORE** com índice único:

* Inserção duplicada → ignorada
* Nenhuma exception desnecessária
* Batch não é interrompido

Resultado: alta resiliência e processamento contínuo.

---

## 🚀 Boa Performance de Escrita

1. **Batch Insert** → `jdbcTemplate.batchUpdate(...)`

    * Reduz round trips ao banco
    * Reduz overhead de transação
    * Melhor throughput

2. **Transação por lote** → `TransactionTemplate` por batch

    * Evita transação gigante
    * Reduz lock time
    * Melhor uso de memória

3. **Índice único enxuto** → apenas em `pan_hash`

    * Menos custo de escrita
    * Menos manutenção de B-Tree
    * Melhor desempenho geral

---

## 🔎 Boa Indexação para Lookup

Lookup feito por:

```sql
WHERE pan_hash = ?
```

* O `pan_hash` é **UNIQUE** → busca O(log n), índice usado diretamente, sem full scan
* **Nunca usar função na coluna** (ex: HEX, CAST, etc) → quebraria uso do índice

---

## 🧠 Parsing Resiliente

* Extração de PAN baseada em regex
* Motivo: arquivos podem variar em padding, comentários podem existir
* Resultado: parser robusto, menos risco de falha em produção

---

## 🔒 Segurança

* Nunca armazenar PAN em texto puro
* Hash para comparação
* Criptografia para confidencialidade
* Serviço de criptografia isolado

Permite conformidade com PCI-DSS e boas práticas de segurança.

---

## 📝 Rotas da API

1. **Criar cartão (POST /cards)**

```http
POST /cards
Content-Type: application/json

{
  "pan": "1234567890123456"
}
```

* Cria um cartão novo, usando hash e criptografia AES-GCM
* Retorna `201 Created` e `id` do cartão
* Idempotente: se o cartão já existe, retorna o mesmo `id` sem criar duplicata

---

2. **Consultar cartão por PAN (GET /cards?pan=...)**

```http
GET /cards?pan=1234567890123456
```

* Consulta eficiente via índice `pan_hash`
* Retorna detalhes do cartão sem expor o PAN em texto puro

---

3. **Importar cartões em lote (POST /cards/import)**

```http
POST /cards/import
Content-Type: multipart/form-data
```

* Recebe arquivo CSV/JSON de cartões
* Processamento em batch, streaming de arquivo para memória mínima
* Idempotente e tolerante a duplicatas
* Retorna resumo: total, inseridos, duplicados, inválidos

---

## 📊 Resultado do Import

Exemplo real:

```json
{
  "totalLines": 13,
  "inserted": 8,
  "duplicates": 2,
  "invalid": 0
}
```

---

## 🧩 Escalabilidade

* Streaming de arquivos → não carrega tudo na memória
* Batch configurável (`BATCH_SIZE`)
* Banco garante unicidade
* Índices enxutos → milhões de registros processáveis
* Ajustes possíveis: batch size, pool de conexões, InnoDB

---

## 📈 Pontos Fortes

✔ Idempotente ✔ Segura ✔ Performática ✔ Resiliente a arquivo imperfeito ✔ Estruturalmente correta ✔ Banco garantindo consistência ✔ Arquitetura limpa

---

## 📌 Possíveis Evoluções

* Import assíncrono (fila + worker)
* Métricas (Micrometer / Prometheus)
* Rate limiting
* Dead-letter para arquivos inválidos
* Auditoria de importação

---

## Conclusão

* Segurança de dados sensíveis
* Integridade
* Performance de escrita
* Simplicidade estrutural
* Escalabilidade real

Banco garante consistência. Aplicação garante segurança. Arquitetura sólida para ambiente produtivo.

---