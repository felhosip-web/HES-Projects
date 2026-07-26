import { Transaction, Category } from './types';
import { DEFAULT_CATEGORIES, DEFAULT_TRANSACTIONS } from './data';

const DB_NAME = 'HesTrackerDB';
const DB_VERSION = 1;

const STORES = {
  TRANSACTIONS: 'transactions',
  CATEGORIES: 'categories'
};

export const initDB = (): Promise<IDBDatabase> => {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onerror = () => reject(request.error);

    request.onsuccess = () => resolve(request.result);

    request.onupgradeneeded = (event: IDBVersionChangeEvent) => {
      const db = (event.target as IDBOpenDBRequest).result;

      if (!db.objectStoreNames.contains(STORES.TRANSACTIONS)) {
        db.createObjectStore(STORES.TRANSACTIONS, { keyPath: 'id' });
      }

      if (!db.objectStoreNames.contains(STORES.CATEGORIES)) {
        db.createObjectStore(STORES.CATEGORIES, { keyPath: 'name' });
      }
    };
  });
};

export const getTransactions = async (): Promise<Transaction[]> => {
  const db = await initDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.TRANSACTIONS, 'readonly');
    const store = tx.objectStore(STORES.TRANSACTIONS);
    const request = store.getAll();

    request.onsuccess = () => {
      let result = request.result;
      if (!result || result.length === 0) {
        // Migration from localStorage if possible, or default
        const saved = localStorage.getItem('hes_tracker_transactions');
        if (saved) {
          try {
            result = JSON.parse(saved);
          } catch (e) {
            result = DEFAULT_TRANSACTIONS;
          }
        } else {
            result = DEFAULT_TRANSACTIONS;
        }
      }
      resolve(result as Transaction[]);
    };
    request.onerror = () => reject(request.error);
  });
};

export const saveTransaction = async (transaction: Transaction): Promise<void> => {
  const db = await initDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.TRANSACTIONS, 'readwrite');
    const store = tx.objectStore(STORES.TRANSACTIONS);
    const request = store.put(transaction);

    request.onsuccess = () => resolve();
    request.onerror = () => reject(request.error);
  });
};

export const deleteTransaction = async (id: string): Promise<void> => {
  const db = await initDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.TRANSACTIONS, 'readwrite');
    const store = tx.objectStore(STORES.TRANSACTIONS);
    const request = store.delete(id);

    request.onsuccess = () => resolve();
    request.onerror = () => reject(request.error);
  });
};

export const clearTransactions = async (): Promise<void> => {
  const db = await initDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.TRANSACTIONS, 'readwrite');
    const store = tx.objectStore(STORES.TRANSACTIONS);
    const request = store.clear();

    request.onsuccess = () => resolve();
    request.onerror = () => reject(request.error);
  });
};

export const getCategories = async (): Promise<Category[]> => {
  const db = await initDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.CATEGORIES, 'readonly');
    const store = tx.objectStore(STORES.CATEGORIES);
    const request = store.getAll();

    request.onsuccess = () => {
      if (!request.result || request.result.length === 0) {
        resolve(DEFAULT_CATEGORIES);
      } else {
        resolve(request.result as Category[]);
      }
    };
    request.onerror = () => reject(request.error);
  });
};

export const saveCategories = async (categories: Category[]): Promise<void> => {
  const db = await initDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.CATEGORIES, 'readwrite');
    const store = tx.objectStore(STORES.CATEGORIES);

    // Clear and re-add all, or just put them one by one. Clearing ensures deleted ones are gone.
    const clearRequest = store.clear();
    clearRequest.onsuccess = () => {
      let completed = 0;
      if (categories.length === 0) {
          resolve();
          return;
      }
      categories.forEach((cat) => {
        const putRequest = store.put(cat);
        putRequest.onsuccess = () => {
          completed++;
          if (completed === categories.length) resolve();
        };
        putRequest.onerror = () => reject(putRequest.error);
      });
    };
    clearRequest.onerror = () => reject(clearRequest.error);
  });
};
