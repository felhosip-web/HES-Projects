import React, { useState, useMemo } from 'react';
import { Transaction, Category } from '../types';
import { Search, Trash2, Calendar, FileText, Filter, RefreshCw } from 'lucide-react';
import { LucideIcon } from './LucideIcon';

interface TransactionListProps {
  transactions: Transaction[];
  categories: Category[];
  onDeleteTransaction: (id: string) => void;
  onClearAll: () => void;
}

export const TransactionList: React.FC<TransactionListProps> = ({
  transactions,
  categories,
  onDeleteTransaction,
  onClearAll,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'income' | 'expense'>('all');
  const [filterCategory, setFilterCategory] = useState('all');
  const [sortBy, setSortBy] = useState<'date-desc' | 'date-asc' | 'amount-desc' | 'amount-asc'>('date-desc');

  // Format currency helper
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('hu-HU', { style: 'currency', currency: 'HUF', maximumFractionDigits: 0 }).format(val);
  };

  // Filter and sort transactions
  const processedTransactions = useMemo(() => {
    let result = [...transactions];

    // Search term filter
    if (searchTerm.trim() !== '') {
      const term = searchTerm.toLowerCase();
      result = result.filter(
        (t) =>
          t.description.toLowerCase().includes(term) ||
          (t.notes && t.notes.toLowerCase().includes(term))
      );
    }

    // Type filter
    if (filterType !== 'all') {
      result = result.filter((t) => t.type === filterType);
    }

    // Category filter
    if (filterCategory !== 'all') {
      result = result.filter((t) => t.category === filterCategory);
    }

    // Sorting
    result.sort((a, b) => {
      if (sortBy === 'date-desc') return b.date.localeCompare(a.date);
      if (sortBy === 'date-asc') return a.date.localeCompare(b.date);
      if (sortBy === 'amount-desc') return b.amount - a.amount;
      if (sortBy === 'amount-asc') return a.amount - b.amount;
      return 0;
    });

    return result;
  }, [transactions, searchTerm, filterType, filterCategory, sortBy]);

  return (
    <div id="transaction_list_card" className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm">
      {/* Search and Filters Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <div>
          <h3 className="font-semibold text-slate-100 text-lg">Tranzakciók Előzménye</h3>
          <p className="text-xs text-slate-400">Rendszerezett pénzmozgások listája</p>
        </div>

        {/* Action button to clear */}
        {transactions.length > 0 && (
          <button
            onClick={() => {
              if (window.confirm('Biztosan törölni szeretné az ÖSSZES tranzakciót?')) {
                onClearAll();
              }
            }}
            className="self-start md:self-auto text-xs text-rose-400 hover:text-rose-300 font-bold transition-colors flex items-center gap-1 bg-rose-500/10 hover:bg-rose-500/20 px-3 py-1.5 rounded-lg"
          >
            Összes törlése
          </button>
        )}
      </div>

      {/* Filter Control Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
        {/* Search */}
        <div className="relative">
          <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500">
            <Search size={16} />
          </span>
          <input
            type="text"
            placeholder="Keresés..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-slate-950 text-slate-100 border border-slate-800 rounded-xl pl-10 pr-4 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/80"
          />
        </div>

        {/* Type Filter */}
        <div className="relative">
          <select
            value={filterType}
            onChange={(e: any) => setFilterType(e.target.value)}
            className="w-full bg-slate-950 text-slate-300 border border-slate-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/80 appearance-none cursor-pointer"
          >
            <option value="all">Minden típus</option>
            <option value="expense">Kiadások</option>
            <option value="income">Bevételek</option>
          </select>
        </div>

        {/* Category Filter */}
        <div className="relative">
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            className="w-full bg-slate-950 text-slate-300 border border-slate-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/80 appearance-none cursor-pointer"
          >
            <option value="all">Minden kategória</option>
            {categories.map((c) => (
              <option key={c.name} value={c.name}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        {/* Sorting selection */}
        <div className="relative">
          <select
            value={sortBy}
            onChange={(e: any) => setSortBy(e.target.value)}
            className="w-full bg-slate-950 text-slate-300 border border-slate-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500/80 appearance-none cursor-pointer"
          >
            <option value="date-desc">Legújabb elöl</option>
            <option value="date-asc">Legrégebbi elöl</option>
            <option value="amount-desc">Összeg szerint csökkenő</option>
            <option value="amount-asc">Összeg szerint növekvő</option>
          </select>
        </div>
      </div>

      {/* Transactions Container */}
      <div className="overflow-x-auto">
        {processedTransactions.length > 0 ? (
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] text-slate-400 uppercase font-bold tracking-wider">
                <th className="pb-3 pl-2">Tranzakció / Kategória</th>
                <th className="pb-3 hidden md:table-cell">Dátum</th>
                <th className="pb-3 hidden sm:table-cell">Jegyzet</th>
                <th className="pb-3 text-right">Összeg</th>
                <th className="pb-3 pr-2 text-right">Művelet</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {processedTransactions.map((tx) => {
                const catObj = categories.find((c) => c.name === tx.category) || {
                  name: tx.category,
                  icon: 'DollarSign',
                  color: '#6b7280',
                };
                return (
                  <tr key={tx.id} className="group hover:bg-slate-800/20 transition-colors">
                    {/* Item and category */}
                    <td className="py-3.5 pl-2">
                      <div className="flex items-center gap-3">
                        <div
                          className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 bg-slate-950 border border-slate-800"
                          style={{ color: catObj.color }}
                        >
                          <LucideIcon name={catObj.icon} size={16} />
                        </div>
                        <div className="min-w-0">
                          <p className="font-bold text-slate-100 text-sm truncate">{tx.description}</p>
                          <span className="text-[10px] text-slate-400 font-medium bg-slate-950 border border-slate-800/80 px-2 py-0.5 rounded-full mt-1 inline-block">
                            {tx.category}
                          </span>
                        </div>
                      </div>
                    </td>

                    {/* Date */}
                    <td className="py-3.5 hidden md:table-cell text-xs font-mono text-slate-400">
                      <div className="flex items-center gap-1.5">
                        <Calendar size={12} className="text-slate-500" />
                        {tx.date}
                      </div>
                    </td>

                    {/* Notes */}
                    <td className="py-3.5 hidden sm:table-cell text-xs text-slate-400 max-w-xs truncate">
                      {tx.notes ? (
                        <div className="flex items-center gap-1.5">
                          <FileText size={12} className="text-slate-500" />
                          <span className="truncate">{tx.notes}</span>
                        </div>
                      ) : (
                        <span className="text-slate-600 font-mono">-</span>
                      )}
                    </td>

                    {/* Amount */}
                    <td className="py-3.5 text-right font-mono text-sm font-bold">
                      <span className={tx.type === 'income' ? 'text-emerald-400' : 'text-rose-400'}>
                        {tx.type === 'income' ? '+' : '-'}
                        {formatCurrency(tx.amount)}
                      </span>
                    </td>

                    {/* Delete action */}
                    <td className="py-3.5 pr-2 text-right">
                      <button
                        onClick={() => onDeleteTransaction(tx.id)}
                        className="text-slate-500 hover:text-rose-400 p-1.5 rounded-lg hover:bg-rose-500/10 transition-all opacity-0 group-hover:opacity-100 focus:opacity-100"
                        title="Törlés"
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        ) : (
          <div className="text-center py-12 flex flex-col items-center justify-center">
            <Filter size={32} className="text-slate-700 mb-2" />
            <p className="text-sm text-slate-400 font-medium">Nincs a szűrésnek megfelelő tranzakció</p>
            <p className="text-xs text-slate-500 mt-1">Próbálkozz más szűréssel vagy vigyél fel újat!</p>
          </div>
        )}
      </div>
    </div>
  );
};
