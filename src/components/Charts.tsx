import React, { useMemo, useState } from 'react';
import { Transaction, Category } from '../types';
import { TrendingUp, PieChart, Sparkles } from 'lucide-react';

interface ChartsProps {
  transactions: Transaction[];
  categories: Category[];
}

export const Charts: React.FC<ChartsProps> = ({ transactions, categories }) => {
  const [hoveredSegment, setHoveredSegment] = useState<number | null>(null);

  // 1. Calculate Category Data for the Donut Chart (Expenses only)
  const categoryData = useMemo(() => {
    const expenses = transactions.filter((t) => t.type === 'expense');
    const totalExpenses = expenses.reduce((sum, t) => sum + t.amount, 0);

    const dataMap: Record<string, number> = {};
    expenses.forEach((t) => {
      dataMap[t.category] = (dataMap[t.category] || 0) + t.amount;
    });

    const list = Object.keys(dataMap).map((catName) => {
      const catObj = categories.find((c) => c.name === catName) || {
        name: catName,
        color: '#6b7280',
      };
      return {
        name: catName,
        value: dataMap[catName],
        percentage: totalExpenses > 0 ? (dataMap[catName] / totalExpenses) * 100 : 0,
        color: catObj.color,
      };
    });

    // Sort by value descending
    return list.sort((a, b) => b.value - a.value);
  }, [transactions, categories]);

  // Donut SVG Math helper
  const donutSegments = useMemo(() => {
    let cumulativePercent = 0;
    const totalValue = categoryData.reduce((sum, item) => sum + item.value, 0);

    return categoryData.map((item) => {
      const percent = totalValue > 0 ? item.value / totalValue : 0;
      const startPercent = cumulativePercent;
      cumulativePercent += percent;

      // Coordinate calculation for SVG Path
      const getCoordinatesForPercent = (percent: number) => {
        const x = Math.cos(2 * Math.PI * percent - Math.PI / 2);
        const y = Math.sin(2 * Math.PI * percent - Math.PI / 2);
        return [x, y];
      };

      const [startX, startY] = getCoordinatesForPercent(startPercent);
      const [endX, endY] = getCoordinatesForPercent(cumulativePercent);
      const largeArcFlag = percent > 0.5 ? 1 : 0;

      // Outer radius 40, inner radius 28
      const rOuter = 40;
      const rInner = 28;

      const pathData = [
        `M ${startX * rOuter} ${startY * rOuter}`, // Move to outer start
        `A ${rOuter} ${rOuter} 0 ${largeArcFlag} 1 ${endX * rOuter} ${endY * rOuter}`, // Draw outer arc
        `L ${endX * rInner} ${endY * rInner}`, // Line to inner end
        `A ${rInner} ${rInner} 0 ${largeArcFlag} 0 ${startX * rInner} ${startY * rInner}`, // Draw inner arc backward
        `Z`, // Close path
      ].join(' ');

      return {
        ...item,
        pathData,
        centerAngle: 2 * Math.PI * (startPercent + percent / 2) - Math.PI / 2,
      };
    });
  }, [categoryData]);

  // 2. Trend Chart (Daily balances for the current month)
  // Let's summarize income and expense by day
  const trendData = useMemo(() => {
    const daysMap: Record<string, { income: number; expense: number }> = {};
    
    // Sort transactions by date
    const sortedTrans = [...transactions].sort((a, b) => a.date.localeCompare(b.date));
    
    sortedTrans.forEach((t) => {
      const dateStr = t.date;
      if (!daysMap[dateStr]) {
        daysMap[dateStr] = { income: 0, expense: 0 };
      }
      if (t.type === 'income') {
        daysMap[dateStr].income += t.amount;
      } else {
        daysMap[dateStr].expense += t.amount;
      }
    });

    const uniqueDates = Object.keys(daysMap).sort();
    let runningBalance = 0;

    return uniqueDates.map((date) => {
      const dayData = daysMap[date];
      runningBalance += dayData.income - dayData.expense;
      return {
        date: date.substring(5), // MM-DD
        income: dayData.income,
        expense: dayData.expense,
        balance: runningBalance,
      };
    });
  }, [transactions]);

  // Plot Trend Curve (pure SVG)
  const trendLinePath = useMemo(() => {
    if (trendData.length === 0) return '';
    const maxVal = Math.max(...trendData.map((d) => Math.max(d.balance, d.income, d.expense, 10000)));
    const minVal = Math.min(0, ...trendData.map((d) => d.balance));
    const range = maxVal - minVal;

    const width = 600;
    const height = 180;
    const padding = 20;

    const points = trendData.map((d, index) => {
      const x = padding + (index / (trendData.length - 1 || 1)) * (width - 2 * padding);
      const y = height - padding - ((d.balance - minVal) / range) * (height - 2 * padding);
      return { x, y };
    });

    if (points.length === 0) return '';
    
    // Create a bezier curve or simple lines
    let d = `M ${points[0].x} ${points[0].y}`;
    for (let i = 1; i < points.length; i++) {
      const p = points[i];
      d += ` L ${p.x} ${p.y}`;
    }
    return d;
  }, [trendData]);

  const trendAreaPath = useMemo(() => {
    if (trendData.length === 0) return '';
    const maxVal = Math.max(...trendData.map((d) => Math.max(d.balance, d.income, d.expense, 10000)));
    const minVal = Math.min(0, ...trendData.map((d) => d.balance));
    const range = maxVal - minVal;

    const width = 600;
    const height = 180;
    const padding = 20;

    const points = trendData.map((d, index) => {
      const x = padding + (index / (trendData.length - 1 || 1)) * (width - 2 * padding);
      const y = height - padding - ((d.balance - minVal) / range) * (height - 2 * padding);
      return { x, y };
    });

    if (points.length === 0) return '';

    const zeroY = height - padding - ((0 - minVal) / (range || 1)) * (height - 2 * padding);
    let d = `M ${points[0].x} ${zeroY}`;
    points.forEach((p) => {
      d += ` L ${p.x} ${p.y}`;
    });
    d += ` L ${points[points.length - 1].x} ${zeroY} Z`;
    return d;
  }, [trendData]);

  // Formatting helper
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('hu-HU', { style: 'currency', currency: 'HUF', maximumFractionDigits: 0 }).format(val);
  };

  return (
    <div id="charts_section" className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-8">
      {/* Trend Chart */}
      <div id="balance_trend_card" className="lg:col-span-8 bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-xl">
              <TrendingUp size={20} />
            </div>
            <div>
              <h3 className="font-semibold text-slate-100 text-lg">Egyenleg Alakulása</h3>
              <p className="text-xs text-slate-400">Kumulatív nettó egyenleg napi szinten</p>
            </div>
          </div>
        </div>

        {trendData.length > 1 ? (
          <div className="w-full relative">
            <svg viewBox="0 0 600 180" className="w-full h-auto overflow-visible">
              <defs>
                <linearGradient id="trendGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#10b981" stopOpacity="0.25" />
                  <stop offset="100%" stopColor="#10b981" stopOpacity="0.0" />
                </linearGradient>
              </defs>
              
              {/* Grid Lines */}
              <line x1="20" y1="20" x2="580" y2="20" stroke="#1e293b" strokeDasharray="3,3" />
              <line x1="20" y1="90" x2="580" y2="90" stroke="#1e293b" strokeDasharray="3,3" />
              <line x1="20" y1="160" x2="580" y2="160" stroke="#1e293b" strokeDasharray="3,3" />

              {/* Area path */}
              <path d={trendAreaPath} fill="url(#trendGradient)" />

              {/* Line path */}
              <path
                d={trendLinePath}
                fill="none"
                stroke="#10b981"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />

              {/* Points on hover/always */}
              {trendData.map((d, index) => {
                const maxVal = Math.max(...trendData.map((d) => Math.max(d.balance, 10000)));
                const minVal = Math.min(0, ...trendData.map((d) => d.balance));
                const range = maxVal - minVal;
                const width = 600;
                const height = 180;
                const padding = 20;
                const x = padding + (index / (trendData.length - 1 || 1)) * (width - 2 * padding);
                const y = height - padding - ((d.balance - minVal) / (range || 1)) * (height - 2 * padding);

                return (
                  <g key={index} className="group cursor-pointer">
                    <circle
                      cx={x}
                      cy={y}
                      r="4"
                      fill="#0f172a"
                      stroke="#10b981"
                      strokeWidth="2"
                    />
                    <circle
                      cx={x}
                      cy={y}
                      r="10"
                      fill="#10b981"
                      fillOpacity="0"
                      className="hover:fill-opacity-20 transition-all duration-200"
                    />
                    {/* Tooltip on Hover */}
                    <foreignObject
                      x={x - 60}
                      y={y - 50}
                      width="120"
                      height="45"
                      className="opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none"
                    >
                      <div className="bg-slate-950 text-[10px] text-white border border-slate-700 p-1 rounded shadow-lg text-center font-mono">
                        <div className="font-semibold text-slate-300">{d.date}</div>
                        <div className="text-emerald-400 font-bold">{formatCurrency(d.balance)}</div>
                      </div>
                    </foreignObject>
                  </g>
                );
              })}
            </svg>
            <div className="flex justify-between text-[10px] font-mono text-slate-500 mt-2 px-4">
              <span>{trendData[0]?.date || ''}</span>
              <span>Középidő</span>
              <span>{trendData[trendData.length - 1]?.date || ''}</span>
            </div>
          </div>
        ) : (
          <div className="h-40 flex flex-col items-center justify-center border border-dashed border-slate-800 rounded-xl">
            <Sparkles size={24} className="text-slate-600 mb-2" />
            <p className="text-sm text-slate-500">Kevés adat a trend megjelenítéséhez</p>
          </div>
        )}
      </div>

      {/* Category Breakdown Donut */}
      <div id="category_pie_card" className="lg:col-span-4 bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm flex flex-col">
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 bg-rose-500/10 text-rose-400 rounded-xl">
            <PieChart size={20} />
          </div>
          <div>
            <h3 className="font-semibold text-slate-100 text-lg">Kiadások Megoszlása</h3>
            <p className="text-xs text-slate-400">Kategóriák szerinti felosztás</p>
          </div>
        </div>

        {donutSegments.length > 0 ? (
          <div className="flex-1 flex flex-col justify-between">
            <div className="flex justify-center items-center relative py-2">
              <svg viewBox="-50 -50 100 100" className="w-44 h-44 overflow-visible transform rotate-[-90deg]">
                {donutSegments.map((seg, idx) => {
                  const isHovered = hoveredSegment === idx;
                  return (
                    <g
                      key={idx}
                      onMouseEnter={() => setHoveredSegment(idx)}
                      onMouseLeave={() => setHoveredSegment(null)}
                      className="cursor-pointer transition-transform duration-200"
                      style={{
                        transform: isHovered ? 'scale(1.05)' : 'scale(1)',
                        transformOrigin: '0px 0px',
                      }}
                    >
                      <path
                        d={seg.pathData}
                        fill={seg.color}
                        stroke="#0f172a"
                        strokeWidth="1"
                      />
                    </g>
                  );
                })}
              </svg>

              {/* Total display in center of Donut */}
              <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                <span className="text-[10px] text-slate-400 font-mono tracking-wider uppercase">Kiadás</span>
                <span className="text-sm font-extrabold text-slate-100">
                  {hoveredSegment !== null
                    ? formatCurrency(donutSegments[hoveredSegment].value)
                    : formatCurrency(transactions.filter((t) => t.type === 'expense').reduce((sum, t) => sum + t.amount, 0))}
                </span>
                <span className="text-[10px] text-emerald-400 font-bold">
                  {hoveredSegment !== null ? `${donutSegments[hoveredSegment].percentage.toFixed(1)}%` : 'Összesen'}
                </span>
              </div>
            </div>

            {/* Legends */}
            <div className="mt-4 grid grid-cols-2 gap-2 text-xs max-h-40 overflow-y-auto pr-1">
              {donutSegments.map((seg, idx) => (
                <button
                  key={idx}
                  onClick={() => setHoveredSegment(hoveredSegment === idx ? null : idx)}
                  className={`flex items-center gap-2 p-1.5 rounded-lg transition-colors text-left ${
                    hoveredSegment === idx ? 'bg-slate-800' : 'hover:bg-slate-800/40'
                  }`}
                >
                  <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: seg.color }} />
                  <span className="truncate text-slate-300 font-medium">{seg.name}</span>
                  <span className="ml-auto text-slate-400 font-mono text-[10px]">
                    {seg.percentage.toFixed(0)}%
                  </span>
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center border border-dashed border-slate-800 rounded-xl p-8">
            <PieChart size={30} className="text-slate-600 mb-2" />
            <p className="text-sm text-slate-500 text-center">Nincs kiadási tranzakció rögzítve</p>
          </div>
        )}
      </div>
    </div>
  );
};
