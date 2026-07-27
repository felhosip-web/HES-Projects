import { initializeApp } from 'firebase/app';
import { getAuth, signInAnonymously } from 'firebase/auth';
import { getFirestore, doc, setDoc, getDoc } from 'firebase/firestore';
import { Transaction, Category } from './types';
import {
  getAllTransactionsSync,
  getAllCategoriesSync,
  forceSaveTransactions,
  forceSaveCategories
} from './db';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "dummy-api-key",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "dummy.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "dummy-project",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "dummy.appspot.com",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "00000000000",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:00000000000:web:abcdef123456"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

export const getLastSyncTime = (): number => {
  const time = localStorage.getItem('hes_last_sync');
  return time ? parseInt(time, 10) : 0;
};

export const setLastSyncTime = (time: number) => {
  localStorage.setItem('hes_last_sync', time.toString());
};

export const getAutoSync = (): boolean => {
  return localStorage.getItem('hes_auto_sync') === 'true';
};

export const setAutoSync = (enabled: boolean) => {
  localStorage.setItem('hes_auto_sync', enabled.toString());
};

const authenticate = async () => {
    if (!auth.currentUser) {
        await signInAnonymously(auth);
    }
    return auth.currentUser;
};

interface SyncData {
    transactions: Transaction[];
    categories: Category[];
}

// Simple last-write-wins merge
const mergeRecords = <T extends { updatedAt?: number, deleted?: boolean }>(
    local: T[],
    cloud: T[],
    key: keyof T
): T[] => {
    const map = new Map<any, T>();

    // Add all cloud records
    cloud.forEach(c => map.set(c[key], c));

    // Overwrite if local is newer
    local.forEach(l => {
        const c = map.get(l[key]);
        const lTime = l.updatedAt || 0;
        const cTime = c?.updatedAt || 0;

        if (!c || lTime > cTime) {
            map.set(l[key], l);
        }
    });

    return Array.from(map.values());
};

export const syncToCloud = async (isAutoSync: boolean = false): Promise<boolean> => {
    try {
        if (!navigator.onLine) {
            console.log("Offline. Skipping sync.");
            return false;
        }

        const user = await authenticate();
        if (!user) return false;

        const docRef = doc(db, 'users', user.uid);

        // 1. Fetch Cloud Data
        const docSnap = await getDoc(docRef);
        let cloudData: SyncData = { transactions: [], categories: [] };
        if (docSnap.exists()) {
            cloudData = docSnap.data() as SyncData;
        }

        // 2. Fetch Local Data (all records including deleted)
        const localTransactions = await getAllTransactionsSync();
        const localCategories = await getAllCategoriesSync();

        // 3. Merge
        const mergedTransactions = mergeRecords(localTransactions, cloudData.transactions || [], 'id');
        const mergedCategories = mergeRecords(localCategories, cloudData.categories || [], 'name');

        // 4. Push back to cloud
        await setDoc(docRef, {
            transactions: mergedTransactions,
            categories: mergedCategories,
            lastSync: Date.now()
        }, { merge: true });

        // 5. Save merged data locally
        await forceSaveTransactions(mergedTransactions);
        await forceSaveCategories(mergedCategories);

        setLastSyncTime(Date.now());

        return true;
    } catch (e) {
        console.error("Sync error:", e);
        return false;
    }
};

export const forceBackup = async (): Promise<boolean> => {
    try {
        if (!navigator.onLine) return false;
        const user = await authenticate();
        if (!user) return false;

        const localTransactions = await getAllTransactionsSync();
        const localCategories = await getAllCategoriesSync();

        const docRef = doc(db, 'users', user.uid);
        await setDoc(docRef, {
            transactions: localTransactions,
            categories: localCategories,
            lastSync: Date.now()
        }, { merge: true });

        setLastSyncTime(Date.now());
        return true;
    } catch(e) {
        console.error("Force backup error:", e);
        return false;
    }
};

export const forceRestore = async (): Promise<boolean> => {
    try {
        if (!navigator.onLine) return false;
        const user = await authenticate();
        if (!user) return false;

        const docRef = doc(db, 'users', user.uid);
        const docSnap = await getDoc(docRef);

        if (docSnap.exists()) {
            const data = docSnap.data() as SyncData;
            await forceSaveTransactions(data.transactions || []);
            await forceSaveCategories(data.categories || []);
            setLastSyncTime(Date.now());
            return true;
        }
        return false;
    } catch(e) {
         console.error("Force restore error:", e);
         return false;
    }
};
