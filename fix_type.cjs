const fs = require('fs');
const code = fs.readFileSync('src/components/TransactionList.tsx', 'utf-8');
const fixed = code.replace(
  "{monthTransactions.map((tx) => {",
  "{(monthTransactions as Transaction[]).map((tx) => {"
);
fs.writeFileSync('src/components/TransactionList.tsx', fixed);
