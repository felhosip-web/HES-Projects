import React, { useMemo } from 'react';
import { Transaction, Category } from '../types';
import { LucideIcon } from './LucideIcon';
import { Sparkles, AlertTriangle } from 'lucide-react';

interface CategoriesListProps {
  transactions: Transaction[];
  categories: Category[];
}

export const CategoriesList: React.FC<CategoriesListProps> = ({ transactions, categories }) => {
  // Format currency helper
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('hu-HU', { style: 'currency', currency: 'HUF', maximumFractionDigits: 0 }).format(val);
  };

  // Calculate budget utilization for categories
  const categoryBudgets = useMemo(() => {
    const expenses = transactions.filter((t) => t.type === 'expense');
    
    return categories
      .filter((c) => c.budget && c.budget > 0) // Only look at categories with a budget
      .map((cat) => {
        const spent = expenses
          .filter((t) => t.category === cat.name)
          .reduce((sum, t) => sum + t.amount, 0);

        const limit = cat.budget || 0;
        const percent = limit > 0 ? (spent / limit) * 100 : 0;

        return {
          ...cat,
          spent,
          limit,
          percent,
        };
      })
      .sort((a, b) => b.percent - a.percent); // Sort by highest usage first
  }, [transactions, categories]);

  return (
    <div id="category_budgets_card" className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-2 bg-amber-500/10 text-amber-400 rounded-xl">
          <AlertTriangle size={20} />
        </div>
        <div>
          <h3 className="font-semibold text-slate-100 text-lg">Költségkeretek & Limitek</h3>
          <p className="text-xs text-slate-400">Aktuális havi keretek kihasználtsága</p>
        </div>
      </div>

      {categoryBudgets.length > 0 ? (
        <div className="space-y-4">
          {categoryBudgets.map((budget) => {
            const isOverBudget = budget.spent > budget.limit;
            return (
              <div key={budget.name} className="space-y-1.5">
                <div className="flex justify-between items-center text-xs">
                  <div className="flex items-center gap-2">
                    <span className="p-1 rounded-lg bg-slate-950 flex items-center justify-center border border-slate-800/80" style={{ color: budget.color }}>
                      <LucideIcon name={budget.icon} size={14} />
                    </span>
                    <span className="font-bold text-slate-200">{budget.name}</span>
                  </div>
                  <div className="text-slate-400 font-mono text-[11px] text-right">
                    <span className={isOverBudget ? 'text-rose-400 font-bold' : 'text-slate-200 font-bold'}>
                      {formatCurrency(budget.spent)}
                    </span>{' '}
                    / {formatCurrency(budget.limit)}
                  </div>
                </div>

                {/* Progress bar */}
                <div className="w-full h-2 bg-slate-950 border border-slate-800 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${
                      budget.percent > 100
                        ? 'bg-rose-500'
                        : budget.percent > 85
                        ? 'bg-amber-500'
                        : 'bg-emerald-500'
                    }`}
                    style={{ width: `${Math.min(100, budget.percent)}%` }}
                  />
                </div>

                <div className="flex justify-between text-[10px] text-slate-500">
                  <span>{budget.percent.toFixed(0)}% elköltve</span>
                  {isOverBudget ? (
                    <span className="text-rose-400 font-bold flex items-center gap-1">
                      Keret túllépve! (+{formatCurrency(budget.spent - budget.limit)})
                    </span>
                  ) : budget.percent > 85 ? (
                    <span className="text-amber-400 font-medium">Közel a limithez!</span>
                  ) : (
                    <span className="text-slate-500">Biztonságos zóna</span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="text-center py-8 border border-dashed border-slate-800 rounded-xl">
          <Sparkles size={24} className="text-slate-600 mb-2" />
          <p className="text-xs text-slate-500">Nincs beállított költségkeret</p>
        </div>
      )}
    </div>
  );
};
