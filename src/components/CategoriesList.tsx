import React, { useMemo, useState } from 'react';
import { Transaction, Category } from '../types';
import { LucideIcon } from './LucideIcon';
import { Sparkles, AlertTriangle, Edit2, Check, X, PlusCircle, Trash2 } from 'lucide-react';
import { useTranslation } from '../i18n';

interface CategoriesListProps {
  transactions: Transaction[];
  categories: Category[];
  onUpdateCategories: (categories: Category[]) => void;
}

export const CategoriesList: React.FC<CategoriesListProps> = ({ transactions, categories, onUpdateCategories }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editingCategories, setEditingCategories] = useState<Category[]>([...categories]);
  const { t, language } = useTranslation();

  // Format currency helper
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat(language === 'hu' ? 'hu-HU' : 'en-US', { style: 'currency', currency: 'HUF', maximumFractionDigits: 0 }).format(val);
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

  const handleEditChange = (index: number, field: keyof Category, value: string | number | undefined) => {
    const newCats = [...editingCategories];
    newCats[index] = { ...newCats[index], [field]: value };
    setEditingCategories(newCats);
  };

  const handleAddCategory = () => {
    setEditingCategories([
      ...editingCategories,
      { name: 'Új kategória', icon: 'Circle', color: '#6b7280', budget: 0 }
    ]);
  };

  const handleRemoveCategory = (index: number) => {
    const newCats = [...editingCategories];
    newCats.splice(index, 1);
    setEditingCategories(newCats);
  };

  const handleSave = () => {
    onUpdateCategories(editingCategories);
    setIsEditing(false);
  };

  const handleCancel = () => {
    setEditingCategories([...categories]);
    setIsEditing(false);
  };

  if (isEditing) {
    return (
      <div id="category_budgets_card" className="bg-slate-900 border border-emerald-500/50 rounded-2xl p-4 sm:p-6 shadow-sm">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-xl">
              <Edit2 size={20} />
            </div>
            <div>
              <h3 className="font-semibold text-slate-100 text-lg">{t('cat_list_edit_mode')}</h3>
            </div>
          </div>
        </div>

        <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2">
          {editingCategories.map((cat, idx) => (
            <div key={idx} className="p-3 bg-slate-950 border border-slate-800 rounded-xl flex flex-col gap-2">
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={cat.name}
                  onChange={(e) => handleEditChange(idx, 'name', e.target.value)}
                  className="flex-1 bg-slate-900 text-slate-100 border border-slate-700 rounded-lg px-2 py-1 text-sm focus:outline-none focus:border-emerald-500"
                  placeholder="Név"
                />
                <button
                  onClick={() => handleRemoveCategory(idx)}
                  className="text-slate-500 hover:text-rose-400 p-1"
                >
                  <Trash2 size={16} />
                </button>
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={cat.icon}
                  onChange={(e) => handleEditChange(idx, 'icon', e.target.value)}
                  className="w-1/2 bg-slate-900 text-slate-100 border border-slate-700 rounded-lg px-2 py-1 text-xs focus:outline-none focus:border-emerald-500"
                  placeholder="Ikon (Lucide)"
                />
                <input
                  type="color"
                  value={cat.color}
                  onChange={(e) => handleEditChange(idx, 'color', e.target.value)}
                  className="w-8 h-8 rounded cursor-pointer bg-slate-900 border-none p-0"
                />
                <input
                  type="number"
                  value={cat.budget || ''}
                  onChange={(e) => handleEditChange(idx, 'budget', parseFloat(e.target.value) || 0)}
                  className="flex-1 bg-slate-900 text-slate-100 border border-slate-700 rounded-lg px-2 py-1 text-xs focus:outline-none focus:border-emerald-500"
                  placeholder="Keret (opc.)"
                />
              </div>
            </div>
          ))}
        </div>

        <div className="mt-4 flex gap-2">
          <button
            onClick={handleAddCategory}
            className="flex-1 py-2 px-3 bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-xs rounded-xl transition-all flex items-center justify-center gap-1.5"
          >
            <PlusCircle size={14} /> Új
          </button>
        </div>

        <div className="mt-4 flex gap-2">
          <button
            onClick={handleCancel}
            className="flex-1 py-2.5 px-4 bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-xs rounded-xl transition-all flex items-center justify-center gap-1.5"
          >
            <X size={14} /> Mégse
          </button>
          <button
            onClick={handleSave}
            className="flex-1 py-2.5 px-4 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold text-xs rounded-xl transition-all flex items-center justify-center gap-1.5"
          >
            <Check size={14} /> Mentés
          </button>
        </div>
      </div>
    );
  }

  return (
    <div id="category_budgets_card" className="bg-slate-900 border border-slate-800 rounded-2xl p-4 sm:p-6 shadow-sm">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-amber-500/10 text-amber-400 rounded-xl">
            <AlertTriangle size={20} />
          </div>
          <div>
            <h3 className="font-semibold text-slate-100 text-lg">{t('cat_list_title')}</h3>
            <p className="text-xs text-slate-400">{t('cat_list_desc')}</p>
          </div>
        </div>
        <button
          onClick={() => {
            setEditingCategories([...categories]);
            setIsEditing(true);
          }}
          className="text-slate-400 hover:text-emerald-400 p-1.5 rounded-lg hover:bg-emerald-500/10 transition-colors"
          title="Szerkesztés"
        >
          <Edit2 size={16} />
        </button>
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
                  <span>{budget.percent.toFixed(0)}{t('cat_list_spent')}</span>
                  {isOverBudget ? (
                    <span className="text-rose-400 font-bold flex items-center gap-1">
                      Keret túllépve! (+{formatCurrency(budget.spent - budget.limit)})
                    </span>
                  ) : budget.percent > 85 ? (
                    <span className="text-amber-400 font-medium">{t('cat_list_near_limit')}</span>
                  ) : (
                    <span className="text-slate-500">{t('cat_list_safe_zone')}</span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="text-center py-8 border border-dashed border-slate-800 rounded-xl">
          <Sparkles size={24} className="text-slate-600 mb-2" />
          <p className="text-xs text-slate-500">{t('cat_list_no_budget')}</p>
        </div>
      )}
    </div>
  );
};
