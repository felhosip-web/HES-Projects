import { useState, useEffect } from 'react';
import { Cloud, UploadCloud, DownloadCloud, RefreshCw, CheckCircle, AlertTriangle, Settings } from 'lucide-react';
import { syncToCloud, forceBackup, forceRestore, getLastSyncTime, getAutoSync, setAutoSync as saveAutoSync } from '../sync';
import { useTranslation } from '../i18n';

export function SyncSettings({ onSyncComplete }: { onSyncComplete: () => void }) {
  const { t, language, setLanguage } = useTranslation();
  const [autoSync, setAutoSync] = useState(getAutoSync());
  const [lastSync, setLastSync] = useState(getLastSyncTime());
  const [syncing, setSyncing] = useState(false);
  const [status, setStatus] = useState<{ type: 'success' | 'error' | null; message: string }>({ type: null, message: '' });

  const [showFirebaseConfig, setShowFirebaseConfig] = useState(false);
  const [firebaseConfigText, setFirebaseConfigText] = useState(() => {
      const saved = localStorage.getItem('hes_firebase_config');
      return saved ? saved : JSON.stringify({ apiKey: "", authDomain: "", projectId: "", storageBucket: "", messagingSenderId: "", appId: "" }, null, 2);
  });

  const formatTime = (ts: number) => {
    if (ts === 0) return t('sync_never_synced');
    return new Date(ts).toLocaleString(language === 'hu' ? 'hu-HU' : 'en-US');
  };

  const handleToggleAutoSync = () => {
    const newVal = !autoSync;
    setAutoSync(newVal);
    saveAutoSync(newVal);
  };

  const wrapSyncAction = async (action: () => Promise<boolean>, successMsg: string, errorMsg: string) => {
    setSyncing(true);
    setStatus({ type: null, message: '' });
    try {
      const success = await action();
      if (success) {
        setStatus({ type: 'success', message: successMsg });
        setLastSync(getLastSyncTime());
        onSyncComplete();
      } else {
        setStatus({ type: 'error', message: errorMsg });
      }
    } catch (e) {
      setStatus({ type: 'error', message: errorMsg });
    } finally {
      setSyncing(false);
      setTimeout(() => setStatus({ type: null, message: '' }), 5000);
    }
  };

  const handleSync = () => wrapSyncAction(() => syncToCloud(false), 'Szinkronizáció sikeres!', 'Szinkronizáció sikertelen. Kérjük, próbálja újra.');
  const handleBackup = () => wrapSyncAction(() => forceBackup(), 'Felhőbe mentés sikeres!', `${t('sync_backup_now')} failed.`);
  const handleRestore = () => {
    if (window.confirm('Biztosan felülírja a helyi adatokat a felhőből származó adatokkal?')) {
        wrapSyncAction(() => forceRestore(), `${t('sync_restore')} success.`, `${t('sync_restore')} failed.`);
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-sm mb-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-indigo-500/10 text-indigo-400 rounded-xl">
            <Cloud size={20} />
          </div>
          <div>
            <div className="flex items-center gap-2">
                <h3 className="font-semibold text-slate-100 text-lg">{t('sync_title')}</h3>
                <button onClick={() => setShowFirebaseConfig(true)} className="text-slate-500 hover:text-slate-300 transition-colors">
                    <Settings size={16} />
                </button>
            </div>
            <p className="text-xs text-slate-400">{t('sync_desc')}</p>
          </div>
        </div>

        {/* Language Switcher */}
        <div className="flex items-center gap-2 bg-slate-950 p-1 rounded-lg border border-slate-800">
           <button
             onClick={() => setLanguage('hu')}
             className={`px-2 py-1 text-[10px] font-bold rounded uppercase transition-colors ${language === 'hu' ? 'bg-indigo-500 text-slate-950' : 'text-slate-400 hover:text-slate-200'}`}
           >
             HU
           </button>
           <button
             onClick={() => setLanguage('en')}
             className={`px-2 py-1 text-[10px] font-bold rounded uppercase transition-colors ${language === 'en' ? 'bg-indigo-500 text-slate-950' : 'text-slate-400 hover:text-slate-200'}`}
           >
             EN
           </button>
        </div>
      </div>


      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-300">{t('sync_auto_sync')}</span>
          <button
            onClick={handleToggleAutoSync}
            className={`w-11 h-6 rounded-full transition-colors relative flex items-center px-0.5 ${autoSync ? 'bg-emerald-500' : 'bg-slate-700'}`}
          >
            <div className={`w-5 h-5 bg-white rounded-full shadow-md transform transition-transform ${autoSync ? 'translate-x-5' : 'translate-x-0'}`} />
          </button>
        </div>

        <div className="text-xs text-slate-500">
          {t('sync_last_sync')}{formatTime(lastSync)}
        </div>

        <div className="grid grid-cols-2 gap-3 mt-4">
           <button
             onClick={handleSync}
             disabled={syncing}
             className="col-span-2 flex items-center justify-center gap-2 bg-indigo-500/10 text-indigo-400 hover:bg-indigo-500/20 py-2 px-4 rounded-xl text-sm font-medium transition-colors disabled:opacity-50"
           >
              <RefreshCw size={16} className={syncing ? 'animate-spin' : ''} />
              {t('sync_now')}
           </button>

           <button
             onClick={handleBackup}
             disabled={syncing}
             className="flex items-center justify-center gap-2 bg-slate-800 text-slate-300 hover:bg-slate-700 py-2 px-4 rounded-xl text-xs font-medium transition-colors disabled:opacity-50"
           >
              <UploadCloud size={14} />
              {t('sync_backup_now')}
           </button>

           <button
             onClick={handleRestore}
             disabled={syncing}
             className="flex items-center justify-center gap-2 bg-slate-800 text-slate-300 hover:bg-slate-700 py-2 px-4 rounded-xl text-xs font-medium transition-colors disabled:opacity-50"
           >
              <DownloadCloud size={14} />
              {t('sync_restore')}
           </button>
        </div>

        {status.type && (
          <div className={`flex items-center gap-2 mt-4 p-3 rounded-xl text-sm ${status.type === 'success' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'}`}>
            {status.type === 'success' ? <CheckCircle size={16} /> : <AlertTriangle size={16} />}
            {status.message}
          </div>
        )}
      </div>

      {showFirebaseConfig && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
           <div className="bg-slate-900 p-6 rounded-2xl border border-slate-800 w-full max-w-md shadow-2xl">
               <h3 className="text-lg font-bold mb-4 text-slate-100">Felhő Beállítások (Firebase)</h3>
               <p className="text-xs text-slate-400 mb-4">Illessze be a Firebase konfigurációs JSON objektumot ide:</p>
               <textarea
                  className="w-full h-48 bg-slate-950 text-slate-300 p-3 text-xs border border-slate-800 rounded-xl focus:border-indigo-500 outline-none font-mono"
                  value={firebaseConfigText}
                  onChange={e => setFirebaseConfigText(e.target.value)}
               />
               <div className="flex justify-end gap-3 mt-6">
                   <button onClick={() => setShowFirebaseConfig(false)} className="px-4 py-2 text-sm text-slate-400 hover:text-slate-200">Mégse</button>
                   <button onClick={() => {
                       try {
                           const parsed = JSON.parse(firebaseConfigText);
                           localStorage.setItem('hes_firebase_config', JSON.stringify(parsed));
                           window.location.reload();
                       } catch(e) {
                           alert("Érvénytelen JSON formátum!");
                       }
                   }} className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 rounded-xl text-white text-sm font-medium transition-colors">Mentés & Újratöltés</button>
               </div>
           </div>
        </div>
      )}
    </div>
  );
}
