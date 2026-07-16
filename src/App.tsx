/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useEffect, useMemo } from 'react';
import { Transaction, Category } from './types';
import { DEFAULT_CATEGORIES, DEFAULT_TRANSACTIONS } from './data';
import { DashboardStats } from './components/DashboardStats';
import { Charts } from './components/Charts';
import { TransactionForm } from './components/TransactionForm';
import { TransactionList } from './components/TransactionList';
import { CategoriesList } from './components/CategoriesList';
import { Wallet, Sparkles, TrendingUp, Github, ArrowUpRight } from 'lucide-react';
import { motion } from 'motion/react';

export default function App() {
  // Load initial data from localStorage or use defaults
  const [transactions, setTransactions] = useState<Transaction[]>(() => {
    const saved = localStorage.getItem('hes_tracker_transactions');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Error parsing saved transactions', e);
      }
    }
    return DEFAULT_TRANSACTIONS;
  });

  const [categories] = useState<Category[]>(DEFAULT_CATEGORIES);

  // Sync to localStorage
  useEffect(() => {
    localStorage.setItem('hes_tracker_transactions', JSON.stringify(transactions));
  }, [transactions]);

  // Statistics calculations
  const totalIncome = useMemo(() => {
    return transactions
      .filter((t) => t.type === 'income')
      .reduce((sum, t) => sum + t.amount, 0);
  }, [transactions]);

  const totalExpenses = useMemo(() => {
    return transactions
      .filter((t) => t.type === 'expense')
      .reduce((sum, t) => sum + t.amount, 0);
  }, [transactions]);

  const totalBalance = useMemo(() => {
    return totalIncome - totalExpenses;
  }, [totalIncome, totalExpenses]);

  // Handle adding new transaction
  const handleAddTransaction = (newTx: Omit<Transaction, 'id'>) => {
    const transaction: Transaction = {
      ...newTx,
      id: crypto.randomUUID(),
    };
    setTransactions((prev) => [transaction, ...prev]);
  };

  // Handle deleting a transaction
  const handleDeleteTransaction = (id: string) => {
    setTransactions((prev) => prev.filter((t) => t.id !== id));
  };

  // Clear all transactions
  const handleClearAll = () => {
    setTransactions([]);
  };

  return (
    <div id="app_root" className="min-h-screen bg-slate-950 text-slate-100 font-sans selection:bg-emerald-500/30 selection:text-emerald-400">
      
      {/* Dynamic Background Accents */}
      <div className="absolute top-0 left-1/4 w-[400px] h-[400px] bg-emerald-500/5 rounded-full blur-[100px] pointer-events-none" />
      <div className="absolute top-20 right-1/4 w-[400px] h-[400px] bg-indigo-500/5 rounded-full blur-[100px] pointer-events-none" />

      {/* Main Container */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative">
        
        {/* Header Block */}
        <header id="app_header" className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8 border-b border-slate-900 pb-6">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-emerald-500/10 text-emerald-400 rounded-2xl flex items-center justify-center border border-emerald-500/20 shadow-inner">
              <Wallet size={24} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-2xl font-black text-slate-100 tracking-tight">HES Költségkövető</h1>
                <span className="text-[10px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2 py-0.5 rounded-full font-bold uppercase tracking-wider font-mono">
                  v1.0
                </span>
              </div>
              <p className="text-xs text-slate-400">Egyszerű, tiszta és biztonságos helyi pénzügyi menedzser</p>
            </div>
          </div>

          <div className="flex items-center gap-2 bg-slate-900/60 border border-slate-800/80 rounded-xl p-2.5">
            <Github size={16} className="text-slate-400" />
            <div className="text-right">
              <span className="text-[10px] font-bold text-slate-400 block font-mono">felhosip-web/HES-Projects</span>
              <a
                href="https://github.com/felhosip-web/HES-Projects"
                target="_blank"
                rel="noreferrer"
                className="text-[10px] text-emerald-400 hover:text-emerald-300 font-semibold flex items-center justify-end gap-0.5 transition-colors"
              >
                GitHub megnyitása <ArrowUpRight size={10} />
              </a>
            </div>
          </div>
        </header>

        {/* KPI Score Cards */}
        <DashboardStats
          totalBalance={totalBalance}
          totalIncome={totalIncome}
          totalExpenses={totalExpenses}
        />

        {/* Dynamic Interactive Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          
          {/* Main Visualizations and History List - 8/12 Columns */}
          <div className="lg:col-span-8 space-y-6">
            
            {/* Charts Section */}
            <Charts transactions={transactions} categories={categories} />

            {/* Transactions List */}
            <TransactionList
              transactions={transactions}
              categories={categories}
              onDeleteTransaction={handleDeleteTransaction}
              onClearAll={handleClearAll}
            />
          </div>

          {/* Quick Input Forms and Budgets Section - 4/12 Columns */}
          <div className="lg:col-span-4 space-y-6">
            
            {/* Quick Add Form */}
            <div id="quick_add_card" className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-xl">
                  <Sparkles size={20} />
                </div>
                <div>
                  <h3 className="font-semibold text-slate-100 text-lg">Tranzakció Rögzítése</h3>
                  <p className="text-xs text-slate-400">Vigyél fel új bevételt vagy kiadást</p>
                </div>
              </div>
              <TransactionForm categories={categories} onSubmit={handleAddTransaction} />
            </div>

            {/* Categories and Budgets Limits */}
            <CategoriesList transactions={transactions} categories={categories} />
          </div>

        </div>

      </div>
    </div>
  );
}
