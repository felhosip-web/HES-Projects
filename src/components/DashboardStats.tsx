import React from 'react';
import { Wallet, ArrowDownRight, ArrowUpRight, Percent } from 'lucide-react';
import { motion } from 'motion/react';
import { useTranslation } from '../i18n';

interface DashboardStatsProps {
  totalBalance: number;
  totalIncome: number;
  totalExpenses: number;
}

export const DashboardStats: React.FC<DashboardStatsProps> = ({
  totalBalance,
  totalIncome,
  totalExpenses,
}) => {
  const { t, language } = useTranslation();
  const savingsRate = totalIncome > 0 ? Math.max(0, ((totalIncome - totalExpenses) / totalIncome) * 100) : 0;

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat(language === 'hu' ? 'hu-HU' : 'en-US', { style: 'currency', currency: 'HUF', maximumFractionDigits: 0 }).format(val);
  };

  const statItems = [
    {
      id: 'stat_balance',
      title: t('stat_balance'),
      value: formatCurrency(totalBalance),
      desc: t('stat_balance_desc'),
      icon: Wallet,
      colorClass: totalBalance >= 0 ? 'text-emerald-400 bg-emerald-500/10' : 'text-rose-400 bg-rose-500/10',
      bgColor: 'bg-slate-900',
    },
    {
      id: 'stat_income',
      title: t('stat_income'),
      value: formatCurrency(totalIncome),
      desc: t('stat_income_desc'),
      icon: ArrowUpRight,
      colorClass: 'text-emerald-400 bg-emerald-500/10',
      bgColor: 'bg-slate-900',
    },
    {
      id: 'stat_expenses',
      title: t('stat_expenses'),
      value: formatCurrency(totalExpenses),
      desc: t('stat_expenses_desc'),
      icon: ArrowDownRight,
      colorClass: 'text-rose-400 bg-rose-500/10',
      bgColor: 'bg-slate-900',
    },
    {
      id: 'stat_savings',
      title: t('stat_savings'),
      value: `${savingsRate.toFixed(1)}%`,
      desc: t('stat_savings_desc'),
      icon: Percent,
      colorClass: 'text-cyan-400 bg-cyan-500/10',
      bgColor: 'bg-slate-900',
    },
  ];

  return (
    <div id="stats_grid" className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      {statItems.map((item, idx) => {
        const Icon = item.icon;
        return (
          <motion.div
            key={item.id}
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: idx * 0.05 }}
            className={`p-4 sm:p-6 rounded-2xl border border-slate-800 ${item.bgColor} shadow-sm relative overflow-hidden`}
          >
            <div className="flex justify-between items-start">
              <div>
                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{item.title}</p>
                <h4 className="text-2xl font-black text-slate-100 mt-2 font-mono tracking-tight">{item.value}</h4>
              </div>
              <div className={`p-2.5 rounded-xl ${item.colorClass}`}>
                <Icon size={20} />
              </div>
            </div>
            <p className="text-[11px] text-slate-500 mt-3">{item.desc}</p>
            <div className="absolute bottom-0 left-0 h-1 w-full bg-slate-800 overflow-hidden">
              <div 
                className={`h-full ${item.id === 'stat_expenses' ? 'bg-rose-500' : item.id === 'stat_income' ? 'bg-emerald-500' : 'bg-slate-700'}`}
                style={{ width: item.id === 'stat_savings' ? `${savingsRate}%` : '100%' }}
              />
            </div>
          </motion.div>
        );
      })}
    </div>
  );
};
