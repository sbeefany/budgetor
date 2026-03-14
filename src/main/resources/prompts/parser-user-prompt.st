User text: "{text}"
Available categories constraints: {categories}

Instructions:
1. Extract the amount as a number.
2. Determine if it is an INCOME or EXPENSE. If ambiguous, assume EXPENSE.
3. Match the category from the "Available categories constraints". If the expense does not match any existing category logically, propose a short new category name.
4. Provide a very short description if applicable.

Output ONLY valid JSON.
