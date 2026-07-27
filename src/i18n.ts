import React, { createContext, useContext, useState, useEffect } from 'react';

type TranslationKey = string;

const en: Record<TranslationKey, string> = {
  "app_title": "HES Expense Tracker",
  "app_desc": "Simple, clean and secure local financial manager",
  "open_github": "Open GitHub",
  "stat_balance": "Current Balance",
  "stat_balance_desc": "Difference between all income and expenses",
  "stat_income": "Total Income",
  "stat_income_desc": "Total incoming amounts",
  "stat_expenses": "Total Expenses",
  "stat_expenses_desc": "Total outgoing amounts",
  "stat_savings": "Savings Rate",
  "stat_savings_desc": "Saved portion of income",
  "charts_expenses_by_category": "Expenses by Category",
  "charts_distribution": "Distribution",
  "charts_cash_flow": "Cash Flow Trend",
  "charts_last_30_days": "Last 30 days",
  "tx_list_history": "Transaction History",
  "tx_list_all_items": "All items",
  "tx_list_search_placeholder": "Search description...",
  "tx_list_all_types": "All types",
  "tx_list_expenses": "Expenses",
  "tx_list_income": "Income",
  "tx_list_all_categories": "All categories",
  "tx_list_sort_newest": "Newest first",
  "tx_list_sort_oldest": "Oldest first",
  "tx_list_sort_highest": "Highest amount",
  "tx_list_sort_lowest": "Lowest amount",
  "tx_list_no_tx": "No transactions found",
  "tx_list_no_tx_desc": "Try modifying your search or filters.",
  "payment_cash": "Cash",
  "payment_card": "Card",
  "payment_transfer": "Transfer",
  "tx_form_title": "Record Transaction",
  "tx_form_desc": "Add new income or expense",
  "tx_form_type_expense": "Expense",
  "tx_form_type_income": "Income",
  "tx_form_desc_label": "Description",
  "tx_form_desc_placeholder": "E.g. Weekly grocery shopping",
  "tx_form_amount_label": "Amount (HUF)",
  "tx_form_amount_placeholder": "0",
  "tx_form_date_label": "Date",
  "tx_form_payment_method": "Payment Method",
  "tx_form_notes": "Notes (optional)",
  "tx_form_notes_placeholder": "Additional details...",
  "tx_form_category": "Category",
  "tx_form_save": "Save Transaction",
  "cat_list_title": "Budgets & Limits",
  "cat_list_desc": "Current monthly limits utilization",
  "cat_list_spent": "% spent",
  "cat_list_over_budget": "Over budget!",
  "cat_list_near_limit": "Near limit!",
  "cat_list_safe_zone": "Safe zone",
  "cat_list_no_budget": "No budgets set",
  "cat_list_edit_mode": "Edit Categories & Budgets",
  "cat_list_edit_desc": "Change names, limits and colors",
  "cat_list_add_new": "Add New",
  "cat_list_cancel": "Cancel",
  "cat_list_save": "Save",
  "sync_title": "Cloud Sync",
  "sync_desc": "Backup data to HES servers",
  "sync_auto_sync": "Auto Sync",
  "sync_auto_desc": "Sync in background every 30 min",
  "sync_last_sync": "Last sync: ",
  "sync_never_synced": "Never synced",
  "sync_backup_now": "Backup Now",
  "sync_restore": "Restore Data",
  "sync_syncing": "Syncing...",
  "cat_food": "Food & Dining",
  "cat_shopping": "Shopping",
  "cat_housing": "Housing & Rent",
  "cat_transportation": "Transportation",
  "cat_entertainment": "Entertainment",
  "cat_utilities": "Utilities",
  "cat_healthcare": "Healthcare",
  "cat_salary": "Salary",
  "cat_investments": "Investments",
  "cat_others": "Others",
  "month_unknown": "Unknown"
};

const hu: Record<TranslationKey, string> = {
  "app_title": "HES Költségkövető",
  "app_desc": "Egyszerű, tiszta és biztonságos helyi pénzügyi menedzser",
  "open_github": "GitHub megnyitása",
  "stat_balance": "Aktuális Egyenleg",
  "stat_balance_desc": "Összes bevétel és kiadás különbsége",
  "stat_income": "Összes Bevétel",
  "stat_income_desc": "Bejövő összegek összesen",
  "stat_expenses": "Összes Kiadás",
  "stat_expenses_desc": "Kimenő tételek összesen",
  "stat_savings": "Megtakarítási Ráta",
  "stat_savings_desc": "Bevétel megtakarított hányada",
  "charts_expenses_by_category": "Kiadások Kategóriánként",
  "charts_distribution": "Megoszlás",
  "charts_cash_flow": "Pénzforgalom Trend",
  "charts_last_30_days": "Elmúlt 30 nap",
  "tx_list_history": "Tranzakció Előzmények",
  "tx_list_all_items": "Összes tétel",
  "tx_list_search_placeholder": "Keresés leírásban...",
  "tx_list_all_types": "Minden típus",
  "tx_list_expenses": "Kiadások",
  "tx_list_income": "Bevételek",
  "tx_list_all_categories": "Minden kategória",
  "tx_list_sort_newest": "Legújabb elöl",
  "tx_list_sort_oldest": "Legrégebbi elöl",
  "tx_list_sort_highest": "Legnagyobb összeg",
  "tx_list_sort_lowest": "Legkisebb összeg",
  "tx_list_no_tx": "Nincs találat",
  "tx_list_no_tx_desc": "Próbáld módosítani a keresést vagy a szűrőket.",
  "payment_cash": "Készpénz",
  "payment_card": "Kártya",
  "payment_transfer": "Utalás",
  "tx_form_title": "Tranzakció Rögzítése",
  "tx_form_desc": "Vigyél fel új bevételt vagy kiadást",
  "tx_form_type_expense": "Kiadás",
  "tx_form_type_income": "Bevétel",
  "tx_form_desc_label": "Leírás",
  "tx_form_desc_placeholder": "Pl. Heti nagybevásárlás",
  "tx_form_amount_label": "Összeg (HUF)",
  "tx_form_amount_placeholder": "0",
  "tx_form_date_label": "Dátum",
  "tx_form_payment_method": "Fizetési mód",
  "tx_form_notes": "Megjegyzés (opcionális)",
  "tx_form_notes_placeholder": "További részletek...",
  "tx_form_category": "Kategória",
  "tx_form_save": "Mentés",
  "cat_list_title": "Költségkeretek & Limitek",
  "cat_list_desc": "Aktuális havi keretek kihasználtsága",
  "cat_list_spent": "% elköltve",
  "cat_list_over_budget": "Keret túllépve!",
  "cat_list_near_limit": "Közel a limithez!",
  "cat_list_safe_zone": "Biztonságos zóna",
  "cat_list_no_budget": "Nincs beállított költségkeret",
  "cat_list_edit_mode": "Kategóriák és Keretek Szerkesztése",
  "cat_list_edit_desc": "Módosítsd a neveket, limiteket és színeket",
  "cat_list_add_new": "Új",
  "cat_list_cancel": "Mégse",
  "cat_list_save": "Mentés",
  "sync_title": "Felhő Szinkronizáció",
  "sync_desc": "Adatok mentése a HES szervereire",
  "sync_auto_sync": "Auto. Szinkron",
  "sync_auto_desc": "Háttérmentés 30 percenként",
  "sync_last_sync": "Utolsó szinkron: ",
  "sync_never_synced": "Még sosem volt szinkronizálva",
  "sync_backup_now": "Azonnali Mentés",
  "sync_restore": "Visszaállítás",
  "sync_syncing": "Szinkronizálás...",
  "cat_food": "Étkezés",
  "cat_shopping": "Vásárlás",
  "cat_housing": "Lakhatás",
  "cat_transportation": "Közlekedés",
  "cat_entertainment": "Szórakozás",
  "cat_utilities": "Rezsi",
  "cat_healthcare": "Egészségügy",
  "cat_salary": "Fizetés",
  "cat_investments": "Befektetés",
  "cat_others": "Egyéb",
  "month_unknown": "Ismeretlen"
};

const translations = { en, hu };

type Language = keyof typeof translations;

interface LanguageContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: (key: TranslationKey) => string;
}

export const LanguageContext = createContext<LanguageContextType>({
  language: 'hu',
  setLanguage: () => {},
  t: (key) => hu[key] || key,
});

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [language, setLanguageState] = useState<Language>('hu');

  useEffect(() => {
    const saved = localStorage.getItem('hes_tracker_lang');
    if (saved === 'en' || saved === 'hu') {
      setLanguageState(saved);
    }
  }, []);

  const setLanguage = (lang: Language) => {
    setLanguageState(lang);
    localStorage.setItem('hes_tracker_lang', lang);
  };

  const t = (key: TranslationKey): string => {
    return translations[language][key] || key;
  };

  return React.createElement(LanguageContext.Provider, { value: { language, setLanguage, t } }, children);
};

export const useTranslation = () => useContext(LanguageContext);
