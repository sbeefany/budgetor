# Budgetor — Research & Support Info

> Supplementary research and analysis for the [Budgetor PRD](budgetor-prd.md).
> This document contains model comparisons, cost analysis, token economy, and other reference material.

**Version** 1.1 | 2026-02-26

---

## 1. Business Value & Success Metrics

**Why build this?**
Personal budget awareness + a hands-on Spring AI learning project. Reduce friction of expense tracking to near-zero by using natural language in Telegram.

**North Star Metric:** % of days with at least 1 logged transaction (consistency of use)

| KPI | Target (MVP) |
|-----|-------------|
| Daily logging streak | ≥ 5 days/week |
| Time to log an expense | < 10 seconds (send message → confirmed) |
| Goal tracking accuracy | User can see real-time progress vs. goals |
| AI parsing accuracy | ≥ 90% of messages correctly parsed into structured transactions |
| Spring AI integration | Successfully using Spring AI for NL parsing |

---

## 2. LLM Model Comparison & Decision

### Development: Ollama (Local) — FREE 🆓

For development, we'll use **Ollama** with a local model. Spring AI has native Ollama support.

| Model | Size | RAM Needed | Russian Support | Notes |
|-------|------|-----------|----------------|-------|
| **Llama 3.1 8B** | 4.7 GB | 8 GB | Good | Best balanced option, strong reasoning |
| **Mistral 7B** | 4.1 GB | 8 GB | Good | Fast inference, great for structured output |
| **Phi-3 3.8B** | 2.3 GB | 4 GB | Moderate | Ultra-lightweight, can run on weak hardware |

> **Recommendation for dev:** Start with **Mistral 7B** — fast, good Russian support, and excellent at following structured output instructions (which we need for parsing expenses into JSON).

### Production: Cloud LLM Comparison

| Provider | Model | Input (per 1M tokens) | Output (per 1M tokens) | Russian | Spring AI Support | Notes |
|----------|-------|----------------------|------------------------|---------|-------------------|-------|
| **Google** | Gemini 2.0 Flash | **$0.10** | **$0.40** | ✅ Good | ✅ Native | 🏆 **Cheapest**, 1M context, very fast |
| **OpenAI** | GPT-4o-mini | $0.15 | $0.60 | ✅ Good | ✅ Native | Reliable, great structured output |
| **Sber** | GigaChat | ~₽0.20/1K tokens | — | ✅ Native RU | ⚠️ Custom adapter | Best Russian, but needs custom Spring AI integration |
| **Anthropic** | Claude 3.5 Haiku | $1.00 | $5.00 | ✅ Good | ✅ Native | ❌ 10x more expensive, overkill for this task |

### Cost Estimate for Single-User MVP

Assuming ~30 messages/day, ~500 tokens/message (prompt + response):
- **Daily:** 30 × 500 = 15,000 tokens
- **Monthly:** ~450,000 tokens

| Provider | Monthly Cost |
|----------|-------------|
| Gemini 2.0 Flash | **~$0.05 (~₽5)** |
| GPT-4o-mini | ~$0.07 (~₽7) |
| GigaChat | ~₽90 (with minimum ₽600/month plan) |
| Claude 3.5 Haiku | ~$0.45 (~₽45) |

> **Recommendation for production:** **Gemini 2.0 Flash** — cheapest, excellent quality, great Russian support, native Spring AI integration. GPT-4o-mini is a close second if you prefer OpenAI.
>
> GigaChat has the best Russian support but requires a custom adapter and has a minimum ₽600/month plan, which is overkill for this volume.

---

## 3. Token Economy Analysis & Optimization Tips 💡

Not all features need AI! Here's a breakdown of **what uses tokens and what doesn't**:

### 🟢 No AI Needed (Zero tokens)

| Feature | Why no AI? |
|---------|-----------|
| US-4: Show balance | Pure DB query: `SUM(income) - SUM(expenses) + initial_balance` |
| US-5: Spending summary | DB aggregation `GROUP BY category, period` |
| US-6: Transaction confirmation | Just format the parsed result as text |
| US-9: Goal progress | Pure math: `current_savings / target × 100%` |
| US-12: Category list | Simple DB `SELECT` |

### 🟡 Core AI Use (Unavoidable, but optimizable)

| Feature | Tokens per call | Optimization |
|---------|----------------|-------------|
| US-1: Parse expense | ~300–500 | Keep prompt minimal: system prompt (~200 tokens) + user message + category list |
| US-2: Parse income | ~300–500 | Same prompt handles both income and expenses |
| US-7: Parse savings goal | ~300–500 | Same AI call, just different output structure |
| US-8: Parse budget limit | ~300–500 | Same AI call |

**Key optimization:** Use a single, compact system prompt that handles ALL message types (expense, income, goal, limit). Send existing categories as a short list so AI picks from them instead of inventing new ones.

### 🟠 Moderate AI Use (Optimize carefully)

| Feature | Risk | Optimization |
|---------|------|-------------|
| US-10: Smart tips | ⚠️ Could be expensive if sending full history | Pre-aggregate data in code, send only summaries to AI. E.g., "Food: 12,000/15,000 used, 15 days left" |
| US-14: Category suggestion | Low | Already part of parsing, no extra call needed |

### 🔴 Potentially Expensive (Consider cutting or deferring)

| Feature | Why expensive? | Recommendation |
|---------|---------------|----------------|
| US-13: Auto daily/weekly summary | Triggered automatically, could include full transaction list | **defer to v2** or do WITHOUT AI — just format DB aggregation as text |

### Summary: Token Budget per Month

| Scenario | AI Calls/Day | Tokens/Day | Tokens/Month | Cost (Gemini Flash) |
|----------|-------------|-----------|-------------|-------------------|
| Conservative (expenses only) | 10 | 5,000 | 150K | **~₽1.5** |
| Normal (expenses + goals + tips) | 30 | 15,000 | 450K | **~₽5** |
| Heavy (with auto-summaries) | 50 | 25,000 | 750K | **~₽8** |

> **Bottom line:** Token costs are negligible for a single-user bot, even on the most conservative budget. No features need to be cut for cost reasons. The main optimization is keeping prompts short and avoiding sending full transaction history to the LLM.

---

## 4. Personas / Stakeholders

| Persona | Description |
|---------|-------------|
| 👤 **Me (sole user)** | Developer & sole user, wants effortless daily expense tracking via Telegram. Values speed and simplicity over features. Learning Spring AI. |
| 🤖 **AI Assistant (bot)** | Parses natural language messages, categorizes transactions, provides balance info and tips |

---

## 5. Open Questions

| # | Status | Question | Resolution |
|---|--------|---------|------------|
| 1 | ✅ | LLM provider? | **Ollama (dev) + Gemini 2.0 Flash (prod)**, GPT-4o-mini as backup |
| 2 | ✅ | Database? | **PostgreSQL** |
| 3 | ✅ | Inline buttons? | **Yes** — main menu + cancel button |
| 4 | ✅ | Cancel flow? | **Inline "Отмена" button** on each transaction confirmation |
| 5 | ✅ | Categories? | **Default from resources file + AI creates new with dedup** |
| 6 | ✅ | Hosting for production? | **Local for development, VPS + Docker for production** |
| 7 | ✅ | Spring AI version? | **Spring AI 1.1.1** (latest stable, released 2025-12-05) |
