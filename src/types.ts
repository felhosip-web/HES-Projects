export interface Transaction {
  id: string;
  description: string;
  amount: number;
  type: 'income' | 'expense';
  category: string;
  date: string; // ISO date string (YYYY-MM-DD)
  notes?: string;
}

export interface Category {
  name: string;
  icon: string; // Lucide icon name
  color: string; // hex or tailwind class color
  budget?: number; // Optional monthly budget
}

export interface DashboardData {
  totalBalance: number;
  totalIncome: number;
  totalExpenses: number;
  savingsRate: number;
}
