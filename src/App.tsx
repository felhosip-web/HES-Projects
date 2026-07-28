/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useEffect, useMemo } from 'react';
import { Transaction, Category } from './types';
import { DashboardStats } from './components/DashboardStats';
import { Charts } from './components/Charts';
import { TransactionForm } from './components/TransactionForm';
import { TransactionList } from './components/TransactionList';
import { CategoriesList } from './components/CategoriesList';
import { SyncSettings } from './components/SyncSettings';
import { Wallet, Sparkles, Github, ArrowUpRight } from 'lucide-react';
import { getTransactions, saveTransaction, deleteTransaction, clearTransactions, getCategories, saveCategories } from './db';
import { syncToCloud, getAutoSync } from './sync';
import { LanguageProvider, useTranslation } from './i18n';

function AppContent() {
  const { t } = useTranslation();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  const reloadData = async () => {
    try {
      const [loadedTxs, loadedCats] = await Promise.all([
        getTransactions(),
        getCategories()
      ]);
      setTransactions(loadedTxs);
      setCategories(loadedCats);
    } catch (e) {
      console.error("Error loading data from IndexedDB:", e);
    }
  };

  // Load initial data from IndexedDB and setup auto-sync
  useEffect(() => {
    const init = async () => {
      await reloadData();
      setLoading(false);

      if (getAutoSync() && navigator.onLine) {
        syncToCloud(true).then((success) => {
          if (success) reloadData();
        });
      }
    };
    init();

    const handleOnline = () => {
      if (getAutoSync()) {
        syncToCloud(true).then((success) => {
          if (success) reloadData();
        });
      }
    };

    window.addEventListener('online', handleOnline);

    const syncInterval = setInterval(() => {
      if (getAutoSync() && navigator.onLine) {
         syncToCloud(true).then((success) => {
           if (success) reloadData();
         });
      }
    }, 30 * 60 * 1000); // 30 mins

    return () => {
      window.removeEventListener('online', handleOnline);
      clearInterval(syncInterval);
    };
  }, []);

  // Sync to IndexedDB handled per-action now instead of purely through effect,
  // except maybe initial load defaults, but we handle DB operations cleanly in action handlers.

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
  const handleAddTransaction = async (newTx: Omit<Transaction, 'id'>) => {
    const transaction: Transaction = {
      ...newTx,
      id: crypto.randomUUID(),
    };
    try {
      await saveTransaction(transaction);
      setTransactions((prev) => [transaction, ...prev]);
    } catch (e) {
      console.error("Failed to save transaction:", e);
    }
  };

  // Handle deleting a transaction
  const handleDeleteTransaction = async (id: string) => {
    try {
      await deleteTransaction(id);
      setTransactions((prev) => prev.filter((t) => t.id !== id));
    } catch (e) {
      console.error("Failed to delete transaction:", e);
    }
  };

  // Update categories (used by CategoriesList in edit mode)
  const handleUpdateCategories = async (newCategories: Category[]) => {
    try {
      await saveCategories(newCategories);
      setCategories(newCategories);
    } catch (e) {
      console.error("Failed to save categories:", e);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center text-emerald-400">
        <Sparkles className="animate-pulse" size={48} />
      </div>
    );
  }

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
                <h1 className="text-2xl font-black text-slate-100 tracking-tight">{t('app_title')}</h1>
                <span className="text-[10px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2 py-0.5 rounded-full font-bold uppercase tracking-wider font-mono">
                  v1.2
                </span>
              </div>
              <p className="text-xs text-slate-400">{t('app_desc')}</p>
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
                {t('open_github')} <ArrowUpRight size={10} />
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
            />
          </div>

          {/* Quick Input Forms and Budgets Section - 4/12 Columns */}
          <div className="lg:col-span-4 space-y-6">
            
            {/* Sync Settings Component */}
            <SyncSettings onSyncComplete={reloadData} />

            {/* Quick Add Form */}
            <div id="quick_add_card" className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm mb-6">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-xl">
                  <Sparkles size={20} />
                </div>
                <div>
                  <h3 className="font-semibold text-slate-100 text-lg">{t('tx_form_title')}</h3>
                  <p className="text-xs text-slate-400">{t('tx_form_desc')}</p>
                </div>
              </div>
              <TransactionForm categories={categories} onSubmit={handleAddTransaction} />
            </div>

            {/* Categories and Budgets Limits */}
            <CategoriesList
              transactions={transactions}
              categories={categories}
              onUpdateCategories={handleUpdateCategories}
            />
          </div>

        </div>

      </div>
    </div>
  );
}


export default function App() {
  return (
    <LanguageProvider>
      <AppContent />
    </LanguageProvider>
  );
}