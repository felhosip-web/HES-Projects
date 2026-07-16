import React, { useState } from 'react';
import { Transaction, Category } from '../types';
import { PlusCircle, Check, X } from 'lucide-react';
import { LucideIcon } from './LucideIcon';

interface TransactionFormProps {
  categories: Category[];
  onSubmit: (transaction: Omit<Transaction, 'id'>) => void;
  onClose?: () => void;
}

export const TransactionForm: React.FC<TransactionFormProps> = ({
  categories,
  onSubmit,
  onClose,
}) => {
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [type, setType] = useState<'income' | 'expense'>('expense');
  const [category, setCategory] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [notes, setNotes] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Filter categories based on transaction type
  // Salay and Investments are income. Others can be both. Rest are expenses.
  const filteredCategories = categories.filter((c) => {
    if (type === 'income') {
      return c.name === 'Salary' || c.name === 'Investments' || c.name === 'Others';
    } else {
      return c.name !== 'Salary' && c.name !== 'Investments';
    }
  });

  // Automatically select first category of filtered list if current selection is invalid
  React.useEffect(() => {
    if (filteredCategories.length > 0) {
      const isSelectedValid = filteredCategories.some((c) => c.name === category);
      if (!isSelectedValid) {
        setCategory(filteredCategories[0].name);
      }
    }
  }, [type, filteredCategories, category]);

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!description.trim()) {
      newErrors.description = 'Megnevezés megadása kötelező';
    }
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      newErrors.amount = 'Érvényes, 0-nál nagyobb összeget adj meg';
    }
    if (!category) {
      newErrors.category = 'Válassz kategóriát';
    }
    if (!date) {
      newErrors.date = 'Válassz dátumot';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    onSubmit({
      description: description.trim(),
      amount: parseFloat(amount),
      type,
      category,
      date,
      notes: notes.trim() || undefined,
    });

    // Reset form
    setDescription('');
    setAmount('');
    setNotes('');
    if (onClose) onClose();
  };

  return (
    <form onSubmit={handleSubmit} id="transaction_add_form" className="space-y-4">
      {/* Type Toggle Button */}
      <div>
        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Tranzakció Típusa</label>
        <div className="grid grid-cols-2 gap-2 bg-slate-950 p-1.5 rounded-xl border border-slate-800">
          <button
            type="button"
            onClick={() => setType('expense')}
            className={`py-2 px-4 rounded-lg font-bold text-xs transition-all flex items-center justify-center gap-1.5 ${
              type === 'expense'
                ? 'bg-rose-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-100'
            }`}
          >
            Kiadás
          </button>
          <button
            type="button"
            onClick={() => setType('income')}
            className={`py-2 px-4 rounded-lg font-bold text-xs transition-all flex items-center justify-center gap-1.5 ${
              type === 'income'
                ? 'bg-emerald-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-100'
            }`}
          >
            Bevétel
          </button>
        </div>
      </div>

      {/* Description input */}
      <div>
        <label htmlFor="tx_desc" className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">Megnevezés</label>
        <input
          id="tx_desc"
          type="text"
          placeholder="Pl: Bevásárlás, Rezsi, Fizetés"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className={`w-full bg-slate-950 text-slate-100 border rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 ${
            errors.description
              ? 'border-rose-500 focus:ring-rose-500/20'
              : 'border-slate-800 focus:ring-emerald-500/20 focus:border-emerald-500/80'
          }`}
        />
        {errors.description && <p className="text-rose-500 text-xs mt-1 font-medium">{errors.description}</p>}
      </div>

      {/* Amount and Date row */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Amount */}
        <div>
          <label htmlFor="tx_amount" className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">Összeg (HUF)</label>
          <div className="relative">
            <input
              id="tx_amount"
              type="number"
              min="1"
              step="any"
              placeholder="0"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className={`w-full bg-slate-950 text-slate-100 border rounded-xl pl-4 pr-12 py-2.5 text-sm font-mono focus:outline-none focus:ring-2 ${
                errors.amount
                  ? 'border-rose-500 focus:ring-rose-500/20'
                  : 'border-slate-800 focus:ring-emerald-500/20 focus:border-emerald-500/80'
              }`}
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-500 font-sans">
              Ft
            </span>
          </div>
          {errors.amount && <p className="text-rose-500 text-xs mt-1 font-medium">{errors.amount}</p>}
        </div>

        {/* Date */}
        <div>
          <label htmlFor="tx_date" className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">Dátum</label>
          <input
            id="tx_date"
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-full bg-slate-950 text-slate-100 border border-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/80"
          />
          {errors.date && <p className="text-rose-500 text-xs mt-1 font-medium">{errors.date}</p>}
        </div>
      </div>

      {/* Category selector */}
      <div>
        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2">Kategória</label>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 max-h-40 overflow-y-auto p-1 bg-slate-950 rounded-xl border border-slate-800">
          {filteredCategories.map((cat) => {
            const isSelected = category === cat.name;
            return (
              <button
                key={cat.name}
                type="button"
                onClick={() => setCategory(cat.name)}
                className={`flex items-center gap-2 p-2 rounded-lg border text-xs text-left transition-all ${
                  isSelected
                    ? 'border-emerald-500 bg-emerald-500/10 text-emerald-400 font-semibold'
                    : 'border-slate-800 hover:border-slate-700 text-slate-400'
                }`}
              >
                <span className="p-1 rounded bg-slate-900 flex items-center justify-center" style={{ color: cat.color }}>
                  <LucideIcon name={cat.icon} size={14} />
                </span>
                <span className="truncate">{cat.name}</span>
              </button>
            );
          })}
        </div>
        {errors.category && <p className="text-rose-500 text-xs mt-1 font-medium">{errors.category}</p>}
      </div>

      {/* Notes text input */}
      <div>
        <label htmlFor="tx_notes" className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1.5">Jegyzet (Opcionális)</label>
        <textarea
          id="tx_notes"
          rows={2}
          placeholder="Pl. Bolt neve, vásárolt termék részletei"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          className="w-full bg-slate-950 text-slate-100 border border-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/80 resize-none"
        />
      </div>

      {/* Submit / Cancel Buttons */}
      <div className="flex gap-3 pt-2">
        {onClose && (
          <button
            type="button"
            onClick={onClose}
            className="flex-1 py-2.5 px-4 bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-xs rounded-xl transition-all flex items-center justify-center gap-1.5"
          >
            <X size={14} /> Mégse
          </button>
        )}
        <button
          type="submit"
          className="flex-1 py-2.5 px-4 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold text-xs rounded-xl transition-all flex items-center justify-center gap-1.5"
        >
          <Check size={14} /> Tranzakció Hozzáadása
        </button>
      </div>
    </form>
  );
};
