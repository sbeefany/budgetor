Analyze the provided image and extract transaction data.
Available categories constraints: {categories}

Instructions:
1. Identify the total amount. If multiple transactions are shown, pick the most significant one or the total.
2. Determine if it is an INCOME or EXPENSE. Receipts are typically EXPENSES.
3. Match the category from the "Available categories constraints". If no match, propose a logical new category.
4. Provide a short description (e.g., store name or items).

Output ONLY valid JSON.
