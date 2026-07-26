import React, { useState, useMemo } from 'react';
import { Transaction, Category } from '../types';
import { LucideIcon } from './LucideIcon';
import { Search, Filter, Trash2, Calendar, FileText, Banknote, CreditCard, SendToBack } from 'lucide-react';

interface TransactionListProps {
  transactions: Transaction[];
  categories: Category[];
  onDeleteTransaction: (id: string) => void;
}

export const TransactionList: React.FC<TransactionListProps> = ({
  transactions,
  categories,
  onDeleteTransaction,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'income' | 'expense'>('all');
  const [filterCategory, setFilterCategory] = useState('all');
  const [sortBy, setSortBy] = useState<'date-desc' | 'date-asc' | 'amount-desc' | 'amount-asc'>('date-desc');

  // Format currency helper
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('hu-HU', { style: 'currency', currency: 'HUF', maximumFractionDigits: 0 }).format(val);
  };

  const getMonthName = (dateString: string) => {
    const d = new Date(dateString);
    if (isNaN(d.getTime())) return 'Ismeretlen';
    return new Intl.DateTimeFormat('hu-HU', { year: 'numeric', month: 'long' }).format(d);
  };

  const getPaymentIcon = (method?: 'cash' | 'card' | 'transfer') => {
    switch (method) {
      case 'cash': return <Banknote size={14} className="text-emerald-400" />;
      case 'card': return <CreditCard size={14} className="text-blue-400" />;
      case 'transfer': return <SendToBack size={14} className="text-purple-400" />;
      default: return null;
    }
  };

  const getPaymentName = (method?: 'cash' | 'card' | 'transfer') => {
    switch (method) {
      case 'cash': return 'Készpénz';
      case 'card': return 'Kártya';
      case 'transfer': return 'Utalás';
      default: return '';
    }
  };

  // Filter and sort transactions
  const groupedTransactions = useMemo(() => {
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

    // Grouping by month
    const groups: Record<string, Transaction[]> = {};
    result.forEach(tx => {
      const month = getMonthName(tx.date);
      if (!groups[month]) {
        groups[month] = [];
      }
      groups[month].push(tx);
    });

    return groups;
  }, [transactions, searchTerm, filterType, filterCategory, sortBy]);

  return (
    <div id="transaction_list_card" className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm">
      {/* Search and Filters Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <div>
          <h3 className="font-semibold text-slate-100 text-lg">Tranzakciók Előzménye</h3>
          <p className="text-xs text-slate-400">Rendszerezett pénzmozgások listája</p>
        </div>
        {/* Deleted "Összes törlése" button */}
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
        {Object.keys(groupedTransactions).length > 0 ? (
          <div className="space-y-6">
            {Object.entries(groupedTransactions).map(([month, monthTransactions]) => (
              <div key={month}>
                <h4 className="text-sm font-bold text-emerald-500 mb-3 border-b border-slate-800/60 pb-2 capitalize">
                  {month}
                </h4>
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-800 text-[11px] text-slate-400 uppercase font-bold tracking-wider">
                      <th className="pb-3 pl-2">Tranzakció / Kategória</th>
                      <th className="pb-3 hidden md:table-cell">Dátum</th>
                      <th className="pb-3 hidden sm:table-cell">Fizetés módja</th>
                      <th className="pb-3 text-right">Összeg</th>
                      <th className="pb-3 pr-2 text-right">Művelet</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {(monthTransactions as Transaction[]).map((tx) => {
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

                          {/* Payment Method / Notes */}
                          <td className="py-3.5 hidden sm:table-cell text-xs text-slate-400 max-w-xs">
                            <div className="flex flex-col gap-1">
                                {tx.type === 'expense' && tx.paymentMethod && (
                                  <div className="flex items-center gap-1.5" title={getPaymentName(tx.paymentMethod)}>
                                    {getPaymentIcon(tx.paymentMethod)}
                                    <span className="truncate">{getPaymentName(tx.paymentMethod)}</span>
                                  </div>
                                )}
                                {tx.notes && (
                                  <div className="flex items-center gap-1.5">
                                    <FileText size={12} className="text-slate-500" />
                                    <span className="truncate">{tx.notes}</span>
                                  </div>
                                )}
                                {!tx.paymentMethod && !tx.notes && (
                                    <span className="text-slate-600 font-mono">-</span>
                                )}
                            </div>
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
              </div>
            ))}
          </div>
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
