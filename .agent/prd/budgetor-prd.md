# PRD — Budgetor: Telegram Budget Bot

**Version** 1.2 | 2026-02-26 | Author: AI Analyst + sbeefany

> 📎 Supporting research (LLM comparisons, token economy, cost analysis): [budgetor-research.md](budgetor-research.md)

---

## 1. Problem / Opportunity

Tracking personal expenses is tedious — most people forget to log spending or give up on complicated budgeting apps. There's an opportunity to make it effortless by meeting the user where they already are: **Telegram**.

- 😩 Traditional budget apps have too many screens and fields — high friction
- 📱 Telegram is always open — no context switching
- 🤖 Natural language input (Spring AI) makes logging feel like texting a friend
- 🎯 No quick way to see "am I on track?" for financial goals
-  Smart tips can improve financial habits over time

---

## 2. Scope

### ✅ In Scope (MVP)
- Telegram bot as the sole interface
- Single user, no authentication (bot token + single chat ID)
- Natural language expense/income input parsed by **Spring AI**
- Transaction categories: default categories from resources + AI can create new ones (with deduplication)
- Set initial balance
- Log income and expenses
- Financial goals: **savings targets** and **budget limits per category**
- View current balance and spending summary
- Basic financial tips
- Single currency: **₽ (rubles)**
- **PostgreSQL** database
- **Telegram inline buttons** for commands and **"Отмена" cancel button** after each transaction
- Development with **Ollama** (local), production with **Gemini 2.0 Flash**

### ❌ Out of Scope (MVP)
- Multi-user / authentication
- Multiple currencies
- Bank integrations / auto-import
- Receipt scanning / photo recognition
- Web or mobile UI
- Export to CSV/Excel
- Recurring transactions (auto-repeat)
- Shared budgets / family accounts

---

## 3. Categories System

### Default Categories (loaded from `resources/categories.json`)

**Expenses:**
🍔 Еда · 🏠 Жильё · 🚗 Транспорт · 🎭 Развлечения · 👕 Одежда · 💊 Здоровье · 📚 Образование · 🛒 Покупки · 📱 Связь и интернет · ❓ Другое

**Income:**
💼 Зарплата · 💻 Фриланс · 🎁 Подарки · 📈 Инвестиции · ❓ Другое

### AI Category Rules
1. AI receives existing categories with each parsing request
2. AI MUST pick from existing categories when possible
3. If no category fits, AI can suggest a new one
4. System checks for duplicates before creating (case-insensitive)
5. New categories are persisted to DB and included in future prompts

---

## 4. Functional Requirements

### Must Have (P0)

| # | User Story |
|---|-----------|
| US-1 | Send a free-text message like _"обед 350"_ — bot parses it into a structured expense (amount, category, description) |
| US-2 | Log income by sending messages like _"зарплата 80000"_ |
| US-3 | Set initial balance |
| US-4 | See current balance (remaining money after all income and expenses) |
| US-5 | See spending summary (today / week / month) grouped by category |
| US-6 | Bot confirms each transaction with **"Отмена" inline button** to cancel |
| US-7 | Set a savings goal: _"накопить 100000 к июлю"_ |
| US-8 | Set a budget limit per category: _"на еду не больше 15000 в месяц"_ |
| US-9 | See goal progress — saved vs. target, budget limit usage |

### Should Have (P1)

| # | User Story |
|---|-----------|
| US-10 | Receive tips based on spending patterns |
| US-11 | Edit or delete the last transaction |
| US-12 | See category list with spending per category |

### Could Have (P2)

| # | User Story |
|---|-----------|
| US-13 | Auto daily/weekly summary (without AI — pure DB aggregation) |
| US-14 | Bot suggests category if unsure, user confirms via inline buttons |
| US-15 | Emoji reactions as quick confirmations |

---

## 5. Non-Functional Requirements

| Area | Requirement |
|------|------------|
| **Tech Stack** | Java 17+, Spring Boot, **Spring AI 1.1.1**, Telegram Bot API |
| **AI (Dev)** | Ollama + Mistral 7B (local, free) |
| **AI (Prod)** | Gemini 2.0 Flash or GPT-4o-mini |
| **Database** | PostgreSQL |
| **Build** | Gradle |
| **Performance** | Bot response < 3 seconds (including AI parsing) |
| **Deployment** | Local for development; **VPS + Docker** for production |
| **Data** | All data stored locally, no external sharing |
| **Language** | Bot understands Russian and English input |

---

## 6. High-Level Architecture

```
┌──────────────┐     ┌──────────────────────────────────┐
│   Telegram    │────▶│        Spring Boot Backend        │
│   User Chat   │◀────│                                  │
└──────────────┘     │  ┌───────────┐  ┌─────────────┐  │
                     │  │ Telegram   │  │  Spring AI   │  │
                     │  │ Bot API    │──│  (Ollama /   │  │
                     │  │ Handler    │  │   Gemini)    │  │
                     │  └─────┬─────┘  └─────────────┘  │
                     │        │                          │
                     │  ┌─────▼──────────────────────┐  │
                     │  │   Service Layer             │  │
                     │  │  • TransactionService       │  │
                     │  │  • GoalService              │  │
                     │  │  • CategoryService          │  │
                     │  │  • BalanceService            │  │
                     │  │  • TipService                │  │
                     │  └─────┬──────────────────────┘  │
                     │        │                          │
                     │  ┌─────▼──────┐                  │
                     │  │ PostgreSQL │                  │
                     │  └────────────┘                  │
                     └──────────────────────────────────┘
```

**Flow:**
1. User sends Telegram message → Bot handler receives update
2. Message routed to Spring AI for NL parsing → returns structured JSON
3. Service layer validates category (dedup check), processes transaction, updates balance
4. Bot sends confirmation with inline **"Отмена"** button
5. For commands (balance, summary, goals) — inline keyboard, no AI needed

---

## 7. Telegram Bot Interface

### Inline Keyboard (Main Menu)
```
[ 💰 Баланс ] [ 📊 Сводка ] [ 🎯 Цели ] [ 📋 Категории ] [ 💡 Совет ]
```

### Transaction Confirmation
```
✅ Записано:
  📝 Тип: Расход
  💰 Сумма: 350 ₽
  📂 Категория: Еда
  📄 Описание: обед

  [ ❌ Отмена ]
```

---

## 8. Risks & Dependencies

| # | Type | Description |
|---|------|-------------|
| 1 | **Dependency** | Ollama for dev, Gemini/OpenAI API key for prod |
| 2 | **Risk** | AI parsing accuracy for Russian slang — mitigate with good prompt + cancel button |
| 3 | **Assumption** | Single user — no user management needed |
| 4 | **Assumption** | MVP runs locally or on a simple VPS |
| 5 | **Dependency** | Telegram Bot API token (@BotFather) |
| 6 | **Dependency** | PostgreSQL (local or Docker) |
| 7 | **Risk** | Spring AI + Ollama may have quirks with Russian — test early |
