# Agent Backlog

This file defines the atomic tasks for developing the Budgetor Telegram Bot. Tasks are broken down to be highly parallelizable across different domains (Database, Business Logic, AI, and Telegram API).

Statuses TODO-inProgress-review-complete

## Phase 1: Infrastructure & Core Domain (Foundation)
This phase sets up the base project structure.
| Task ID | Component/Area | Priority | Status | Description/Notes | Acceptance Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| CORE-1 | Infrastructure | P0 | complete | Configure Spring Boot project properties, application.yaml, and dependencies (PostgreSQL, Spring AI, Telegram API). Setup Docker compose for local DB. | Application starts and connects to local PostgreSQL. |
| CORE-2 | Database | P0 | complete | Create Flyway/Liquibase schema migrations for `categories`, `transactions`, and `goals` tables. | Migrations apply successfully on startup. |
| CORE-3 | Domain | P0 | complete | Create JPA Entities (`Category`, `Transaction`, `Goal`) and Spring Data Repositories. | Entities are correctly mapped to tables. |

## Phase 2: Independent Core Services (Highly Parallelizable)
Once Domain boundaries are defined, these services can be implemented concurrently.
| Task ID | Component/Area | Priority | Status | Description/Notes | Acceptance Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| SRV-1 | Category Service | P0 | complete | Service to load default categories from `resources/categories.json`. Handle deduplication matching for AI-created categories. | Defaults populated on startup; duplicates rejected (ignoring case). |
| SRV-2 | Transaction Service | P0 | complete | Logic to create income/expenses, calculate current balance, and handle deletions (US-2, US-3, US-4). | Service properly saves transactions and aggregates current balance. |
| SRV-3 | Goal Service | P0 | complete | Logic to manage savings targets and category budget limits. Compute progress (US-7, US-8, US-9). | Returns saved vs. target ratio and budget usage state. |
| SRV-4 | Summary Service | P0 | complete | Aggregate spending summaries (today / week / month) grouped by category (US-5). | Returns structured aggregation data. |
| SRV-5 | Tip Service | P1 | complete | Generate and retrieve basic financial tips based on spending context (US-10). | Tip string is successfully returned. |

## Phase 3: AI & Parsing Engine (Highly Parallelizable)
Can be worked on with mocked business services.
| Task ID | Component/Area | Priority | Status | Description/Notes | Acceptance Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| AI-1 | Engine Setup | P0 | complete | Configure Spring AI client for Ollama (Mistral). Provide connection properties. | Can ping the AI model locally. |
| AI-2 | Transaction Parser | P0 | complete | Implement the prompt engineering. Takes user text + available categories and returns structured JSON (amount, category, description, type). (US-1) | Reliably parses "обед 350" to `{"amount":350, "category":"Еда", "type":"EXPENSE"}`. |
| AI-3 | Photo/Vision Parser | P1 | complete | Integrate an AI Vision model to parse receipts/photos into structured expenses (US-1.1). | Given an image of a receipt, extracts JSON expense data. |

## Phase 4: Local CLI Interface
This phase ensures all user activities can be executed and tested via a local CLI environment, allowing us to test the LLM and core features without a finished Telegram Bot.
| Task ID | Component/Area | Priority | Status | Description/Notes | Acceptance Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| CLI-1 | CLI Interactivity | P0 | complete | Setup an interactive local CLI loop (e.g. using `Scanner` or Spring Shell) for local run. | App accepts interactive CLI commands when running locally. |
| CLI-2 | CLI Main Menu | P0 | complete | Implement text-based triggers for Balance, Summary, Goals, Categories, Tips. | User can trigger and view outputs for all core services in CLI. |
| CLI-3 | CLI AI Parser | P0 | complete | Route free-text input in CLI to the AI Parser and save via TransactionService. | Typing free-text expenses in CLI parses and saves them. |

## Phase 5: Telegram Bot Interface (Highly Parallelizable)
Can be developed leveraging mocked Core Services and AI.
| Task ID | Component/Area | Priority | Status | Description/Notes | Acceptance Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| BOT-1 | Bot Foundation | P0 | complete | Setup Telegram interaction handler and message router. Implement `/start`. | Bot replies to the start command. |
| BOT-2 | Main Menu UI | P0 | review | Implement the base Inline Keyboard returning Balance, Summary, Goals, Categories, Tips. | Menu is shown; button callbacks route properly. |
| BOT-3 | AI Integration | P0 | TODO | Route arbitrary free-text strings to the AI Parser -> save via Transaction Service -> respond. | User sends text; transaction saves; success message returned. |
| BOT-3.1 | Photo Input | P1 | TODO | Route photo messages to AI Vision parser -> save via Transaction Service -> respond. | User sends photo; transaction saves; success message returned. |
| BOT-4 | Cancel Button | P0 | TODO | Append "❌ Отмена" inline button on transaction confirmations. Handle the callback to rollback (US-6, US-11). | Clicking visually feedback and removes the record. |
| BOT-5 | Commands Config | P0 | TODO | Bind Telegram inline button callbacks to BalanceService, SummaryService, GoalService outputs. | Pressing "Баланс" replies with the formatted balance. |

## Phase 6: Localization & Tech Debt
| Task ID | Component/Area | Priority | Status | Description/Notes | Acceptance Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| LOC-1 | I18n | P2 | TODO | Extract hardcoded Russian strings from code (services/bot) to MessageSource or resource properties files for localization. | No hardcoded Russian strings remain in the Java codebase. |

## Phase 7: Future Features (Usual Expenses)
| Task ID | Component/Area | Priority | Status | Description/Notes | Acceptance Criteria |
| :--- | :--- | :--- | :--- | :--- | :--- |
| CORE-4 | Usual Expenses | P1 | TODO | Create JPA Entity (`UsualExpense`), Repository, and DB migration. | Entity is mapped, Flyway migration successful. |
| SRV-6 | Usual Expenses | P1 | TODO | Logic to save and fetch "usual" or frequent expenses templates. | User can define templates for frequent expenses and list them. |
| BOT-6 | Usual Expenses UI | P1 | TODO | UI elements to show and add usual expenses. | User can pick a usual expense to add it quickly. |
