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

export const getAllTransactionsSync = async (): Promise<Transaction[]> => {
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

export const getTransactions = async (): Promise<Transaction[]> => {
  const allTxs = await getAllTransactionsSync();
  return allTxs.filter(t => !t.deleted);
};

export const saveTransaction = async (transaction: Transaction): Promise<void> => {
  const db = await initDB();

  if (!transaction.updatedAt) {
    transaction.updatedAt = Date.now();
  }

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

  // To do a soft delete, we first fetch the existing transaction
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.TRANSACTIONS, 'readwrite');
    const store = tx.objectStore(STORES.TRANSACTIONS);

    const getRequest = store.get(id);
    getRequest.onsuccess = () => {
      const transaction = getRequest.result;
      if (transaction) {
        transaction.deleted = true;
        transaction.updatedAt = Date.now();
        const putRequest = store.put(transaction);
        putRequest.onsuccess = () => resolve();
        putRequest.onerror = () => reject(putRequest.error);
      } else {
        // Fallback to true delete if somehow it doesn't exist but we wanted to delete?
        // Actually, just resolve if it's already gone.
        resolve();
      }
    };
    getRequest.onerror = () => reject(getRequest.error);
  });
};

export const forceSaveTransactions = async (transactions: Transaction[]): Promise<void> => {
    const db = await initDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORES.TRANSACTIONS, 'readwrite');
      const store = tx.objectStore(STORES.TRANSACTIONS);

      const clearRequest = store.clear();
      clearRequest.onsuccess = () => {
        let completed = 0;
        if (transactions.length === 0) {
            resolve();
            return;
        }
        transactions.forEach((item) => {
          const putRequest = store.put(item);
          putRequest.onsuccess = () => {
            completed++;
            if (completed === transactions.length) resolve();
          };
          putRequest.onerror = () => reject(putRequest.error);
        });
      };
      clearRequest.onerror = () => reject(clearRequest.error);
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

export const getAllCategoriesSync = async (): Promise<Category[]> => {
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

export const getCategories = async (): Promise<Category[]> => {
    const allCats = await getAllCategoriesSync();
    return allCats.filter(c => !c.deleted);
};

export const saveCategories = async (categories: Category[]): Promise<void> => {
  const db = await initDB();

  // Fetch existing to diff and soft-delete removed ones
  const existingCategories = await getAllCategoriesSync();
  const existingMap = new Map(existingCategories.map(c => [c.name, c]));

  const now = Date.now();
  const toSave: Category[] = [];

  const incomingMap = new Map(categories.map(c => [c.name, c]));

  // 1. Process incoming categories (new or updated)
  for (const cat of categories) {
    const existing = existingMap.get(cat.name);
    if (!existing) {
        // New category
        toSave.push({ ...cat, updatedAt: now });
    } else {
        // Check if anything changed (simple object equality is tricky, let's just always update if they click save,
        // or we could deep compare. For simplicity, we just update timestamp if we are "saving" it).
        // A better approach is to only update timestamp if properties actually changed.
        const changed = existing.icon !== cat.icon || existing.color !== cat.color || existing.budget !== cat.budget || existing.deleted;
        if (changed) {
            toSave.push({ ...cat, updatedAt: now, deleted: false });
        } else {
            toSave.push(existing); // keep old timestamp
        }
    }
  }

  // 2. Mark removed categories as deleted
  for (const [name, existing] of existingMap.entries()) {
      if (!incomingMap.has(name) && !existing.deleted) {
          toSave.push({ ...existing, deleted: true, updatedAt: now });
      }
      // if it was already deleted and not in incoming, it stays deleted
      if (!incomingMap.has(name) && existing.deleted) {
          toSave.push(existing);
      }
  }

  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORES.CATEGORIES, 'readwrite');
    const store = tx.objectStore(STORES.CATEGORIES);

    // We no longer clear the store! We use soft deletes.
    let completed = 0;
    if (toSave.length === 0) {
        resolve();
        return;
    }
    toSave.forEach((cat) => {
      const putRequest = store.put(cat);
      putRequest.onsuccess = () => {
        completed++;
        if (completed === toSave.length) resolve();
      };
      putRequest.onerror = () => reject(putRequest.error);
    });
  });
};

export const forceSaveCategories = async (categories: Category[]): Promise<void> => {
    const db = await initDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORES.CATEGORIES, 'readwrite');
      const store = tx.objectStore(STORES.CATEGORIES);

      const clearRequest = store.clear();
      clearRequest.onsuccess = () => {
        let completed = 0;
        if (categories.length === 0) {
            resolve();
            return;
        }
        categories.forEach((item) => {
          const putRequest = store.put(item);
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
