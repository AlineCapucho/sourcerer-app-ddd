# Sourcerer App — DDD Architecture

Refatoração do [sourcerer-app](https://github.com/sourcerer-io/sourcerer-app/) utilizando Domain-Driven Design (DDD).

## Disclaimer

Este projeto é baseado no [sourcerer-io/sourcerer-app](https://github.com/sourcerer-io/sourcerer-app/), uma aplicação CLI em Kotlin que analisa repositórios Git e gera perfis de desenvolvedores. O código original é de autoria da Sourcerer Inc. e está licenciado sob MIT.

A refatoração foi realizada com fins acadêmicos para comparação de métricas de qualidade (ex: SonarQube) entre diferentes padrões arquiteturais. O escopo funcional é idêntico ao projeto original — apenas a organização arquitetural foi alterada.

## Objetivo

Verificar como métricas de qualidade de software variam conforme diferentes padrões arquiteturais, mantendo exatamente o mesmo escopo funcional entre as versões.

## Arquitetura

```
UI / CLI
    |
    v
Infrastructure (frameworks, adaptadores)
    |
    v
Application (use cases, DTOs, mappers)
    |
    v
Domain (entidades, value objects, agregados, serviços, eventos)
```

## Estrutura do Projeto

```
src/main/kotlin/app/
├── Main.kt                              # Composition Root (DI manual)
│
├── domain/                              # Camada central — sem dependências externas
│   ├── repository/                      # Bounded Context: Análise de Repositório
│   │   ├── aggregate/                   # Agregados
│   │   │   └── RepoAggregate.kt
│   │   ├── entity/                      # Entidades com identidade
│   │   │   ├── Author.kt
│   │   │   ├── Commit.kt
│   │   │   └── Repo.kt
│   │   ├── event/                       # Eventos de domínio
│   │   │   ├── AuthorsDiscoveredEvent.kt
│   │   │   ├── CommitProcessedEvent.kt
│   │   │   ├── FactsCalculatedEvent.kt
│   │   │   └── RepoHashingCompletedEvent.kt
│   │   ├── factory/                     # Factories para criação complexa
│   │   │   ├── CommitFactory.kt
│   │   │   └── RepoFactory.kt
│   │   ├── port/                        # Ports (interfaces para ACL)
│   │   │   ├── GitRepositoryPort.kt
│   │   │   └── ServerApiPort.kt
│   │   ├── repository/                  # Interfaces de repositório
│   │   │   ├── AuthorDistanceRepository.kt
│   │   │   ├── AuthorRepository.kt
│   │   │   ├── CommitRepository.kt
│   │   │   ├── FactRepository.kt
│   │   │   └── RepoRepository.kt
│   │   ├── service/                     # Domain Services (interfaces)
│   │   │   ├── AuthorDistanceService.kt
│   │   │   ├── CodeLongevityService.kt
│   │   │   ├── CommitExtractionService.kt
│   │   │   ├── FactCalculationService.kt
│   │   │   └── MetaHashingService.kt
│   │   ├── valueobject/                 # Objetos de valor (imutáveis)
│   │   │   ├── AuthorDistance.kt
│   │   │   ├── CommitStats.kt
│   │   │   ├── DiffContent.kt
│   │   │   ├── DiffFile.kt
│   │   │   ├── DiffRange.kt
│   │   │   ├── Fact.kt
│   │   │   ├── ProcessEntry.kt
│   │   │   ├── Rehash.kt
│   │   │   └── RepoMeta.kt
│   │   └── FactCodes.kt
│   ├── user/                            # Bounded Context: Usuário/Configuração
│   │   ├── aggregate/
│   │   │   └── UserAggregate.kt
│   │   ├── entity/
│   │   │   └── User.kt
│   │   ├── repository/
│   │   │   └── UserRepository.kt
│   │   └── valueobject/
│   │       ├── Credentials.kt
│   │       ├── LocalRepo.kt
│   │       └── UserEmail.kt
│   └── shared/                          # Shared Kernel
│       ├── event/
│       │   ├── DomainEvent.kt
│       │   ├── EventDispatcher.kt
│       │   └── EventHandler.kt
│       └── valueobject/
│           └── Email.kt
│
├── application/                         # Camada de aplicação (orquestração)
│   ├── dto/                             # Data Transfer Objects
│   │   ├── CommitDTO.kt
│   │   ├── FactDTO.kt
│   │   └── RepoDTO.kt
│   ├── mapper/                          # Mappers entre camadas
│   │   ├── CommitMapper.kt
│   │   └── RepoMapper.kt
│   └── usecase/                         # Casos de uso
│       ├── AddRepositoryUseCase.kt
│       ├── AuthenticateUserUseCase.kt
│       ├── HashRepositoryUseCase.kt
│       ├── ListRepositoriesUseCase.kt
│       ├── RemoveRepositoryUseCase.kt
│       └── ServiceFactory.kt
│
└── infrastructure/                      # Camada de infraestrutura (frameworks)
    ├── api/                             # ACL: servidor HTTP (Fuel + Protobuf)
    │   ├── ServerApiAdapter.kt
    │   └── proto/
    │       ├── ProtoCommitMapper.kt
    │       ├── ProtoFactMapper.kt
    │       └── ProtoRepoMapper.kt
    ├── cli/                             # Comandos CLI (JCommander)
    │   ├── CommandAdd.kt
    │   ├── CommandConfig.kt
    │   ├── CommandList.kt
    │   ├── CommandRemove.kt
    │   └── Options.kt
    ├── config/                          # Persistência de configuração (Jackson/YAML)
    │   ├── FileHelper.kt
    │   ├── FileUserRepository.kt
    │   ├── PasswordHelper.kt
    │   └── UiHelper.kt
    ├── event/                           # Implementação do despachante de eventos
    │   └── InMemoryEventDispatcher.kt
    ├── extractor/                       # Detecção de linguagem e cálculo de fatos
    │   ├── DefaultAuthorDistanceService.kt
    │   ├── DefaultCommitExtractionService.kt
    │   ├── DefaultFactCalculationService.kt
    │   ├── DefaultMetaHashingService.kt
    │   ├── DefaultServiceFactory.kt
    │   └── LanguageDetector.kt
    ├── git/                             # ACL: JGit
    │   ├── JGitCommitStream.kt
    │   ├── JGitPathStream.kt
    │   ├── JGitRepositoryAdapter.kt
    │   └── VendorConventions.kt
    ├── persistence/                     # Implementações de repositório (via API)
    │   ├── ApiAuthorDistanceRepository.kt
    │   ├── ApiAuthorRepository.kt
    │   ├── ApiCommitRepository.kt
    │   ├── ApiFactRepository.kt
    │   └── ApiRepoRepository.kt
    ├── ui/                              # Console UI (State Machine)
    │   ├── AuthState.kt
    │   ├── CloseState.kt
    │   ├── ConsoleState.kt
    │   ├── ConsoleUi.kt
    │   ├── Context.kt
    │   ├── OpenState.kt
    │   └── UpdateRepoState.kt
    ├── Analytics.kt
    └── Logger.kt
```

## Preceitos DDD Seguidos

### 1. Desenvolvimento Guiado ao Domínio

Forma de desenvolver software com o foco no coração da aplicação (domínio), tendo como objetivo entender suas regras, processos e complexidades, separando esses de outros pontos complexos do software.

### 2. Bounded Contexts (Contextos Delimitados)

Divisão explícita em contextos com linguagem própria. "Contexto é Rei" — cada contexto define seus próprios modelos:

- **Contexto de Análise de Repositório** (Core Domain): extração de commits, stats, fatos
- **Contexto de Usuário/Configuração** (Suporte): credenciais, repos locais
- **Shared Kernel**: conceitos compartilhados (Email, eventos base)

### 3. Entidades com Identidade e Expressividade

Entidades possuem identidade única, unicidade, e métodos com intenção de negócio (ex: `changeName` em vez de `setName`). Construtores que fazem sentido e autovalidação.

### 4. Objetos de Valor Imutáveis

Objetos de valor são imutáveis, não têm id, não têm getter e setter para os atributos, possuem autovalidação. Podem ser manipulados via métodos que retornam novas instâncias (ex: `toString`).

### 5. Agregados como Unidade de Consistência

Conjunto de objetos associados com propósito de mudança de dados. Fortemente acoplados internamente. Todo tipo de agregado tem um repositório, com relação um para um.

### 6. Domain Services Stateless

Operação sem estado que realiza uma tarefa do domínio. Não é um agregado, não é um objeto de valor. Qualquer transformação ou processo que não é uma responsabilidade natural de uma entidade ou objeto de valor.

### 7. Eventos de Domínio

Capturam uma ocorrência de algo que aconteceu no domínio. Todo evento deve ser representado por algo que aconteceu no passado. Utilizados para notificar outros bounded contexts de uma mudança de estado. Componentes: Evento, Handler, Despachante de eventos.

### 8. Factories para Criação Complexa

Responsabilidade de criar instâncias complexas e agregados em um objeto separado. Interface que encapsula toda a criação. Não fazem referência a objetos concretos que estão sendo instanciados.

### 9. Anticorruption Layer (ACL)

Cenário onde o cliente (downstream) cria uma camada intermediária que se comunica com o contexto upstream, a fim de atender o seu próprio modelo de domínio. Aplicado para JGit e Protobuf.

### 10. Separação de Entidade de Negócio e Entidade de Persistência

Entidade focada em negócio na pasta Entity. Entidade focada em persistência na pasta infra (Model, Modelo de persistência). Evita complexidade acidental.

### 11. Direção de Dependência: De Fora para Dentro

```
infrastructure/ → application/ → domain/
     (fora)         (meio)        (dentro)
```

O domínio não depende de nada externo. A aplicação depende apenas do domínio. A infraestrutura depende de ambos.

### 12. Composição na Raiz

A injeção de dependência é feita manualmente no `Main.kt` — o único lugar onde classes concretas de infraestrutura são instanciadas e conectadas.

## Validação Arquitetural com ArchUnit

O projeto inclui 24 regras ArchUnit que validam automaticamente os preceitos DDD:

- Camadas respeitam a direção de dependência
- Domínio e aplicação não importam frameworks
- Bounded contexts são isolados entre si
- Repositórios e services no domínio são interfaces
- Eventos implementam DomainEvent
- Frameworks confinados à infraestrutura (JGit, Protobuf, Fuel, Jackson, Sentry, RxJava)

```
src/test/kotlin/test/tests/architecture/
├── LayerDependencyRulesTest.kt      # 4 regras de camadas
├── DomainModelRulesTest.kt          # 7 regras de modelo de domínio
├── BoundedContextRulesTest.kt       # 6 regras de bounded contexts
└── InfrastructureRulesTest.kt       # 7 regras de confinamento de frameworks
```

## Stack Tecnológica

| Componente | Tecnologia |
|---|---|
| Linguagem | Kotlin 1.2.31 |
| Build | Gradle |
| Serialização | Protocol Buffers 3.5.1 |
| HTTP Client | Fuel 1.12.1 |
| Git | JGit 4.9.0 |
| Reactive | RxJava 2.1.12 |
| CLI | JCommander 1.72 |
| Testes | Spek 1.1.5 + ArchUnit 1.0.1 |
| Logging | Sentry 1.7.3 |

## Como Executar

```bash
# Build
./do.sh build_jar

# Executar diretamente
java -jar build/libs/sourcerer-app-ddd.jar

# Adicionar repositório
java -jar build/libs/sourcerer-app-ddd.jar add /path/to/repo

# Listar repositórios
java -jar build/libs/sourcerer-app-ddd.jar list
```

## Licença

MIT — veja [LICENSE.md](LICENSE.md).
