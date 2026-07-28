package com.example.expensetracker.sync

import android.content.Context
import android.util.Log
import com.example.expensetracker.data.ExpenseDao
import com.example.expensetracker.data.ExpenseEntity
import com.example.expensetracker.ui.CategoryData
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expenseDao: ExpenseDao
) {
    private val sharedPrefs = context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var customApp: FirebaseApp? = null
    var auth: FirebaseAuth = FirebaseAuth.getInstance()
    var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        initializeCustomApp()
    }

    private fun initializeCustomApp() {
        val apiKey = sharedPrefs.getString("fb_api_key", "")
        val appId = sharedPrefs.getString("fb_app_id", "")
        val projectId = sharedPrefs.getString("fb_project_id", "")
        if (!apiKey.isNullOrEmpty() && !appId.isNullOrEmpty() && !projectId.isNullOrEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId(appId)
                .setProjectId(projectId)
                .build()
            try {
                customApp = FirebaseApp.getInstance("CustomCloudApp")
            } catch (e: Exception) {
                customApp = FirebaseApp.initializeApp(context, options, "CustomCloudApp")
            }
            auth = FirebaseAuth.getInstance(customApp!!)
            db = FirebaseFirestore.getInstance(customApp!!)
        } else {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
        }
    }

    fun saveConfig(apiKey: String, appId: String, projectId: String) {
        sharedPrefs.edit()
            .putString("fb_api_key", apiKey)
            .putString("fb_app_id", appId)
            .putString("fb_project_id", projectId)
            .apply()
        initializeCustomApp()
    }

    fun getApiKey() = sharedPrefs.getString("fb_api_key", "") ?: ""
    fun getAppId() = sharedPrefs.getString("fb_app_id", "") ?: ""
    fun getProjectId() = sharedPrefs.getString("fb_project_id", "") ?: ""

    private val TAG = "FirebaseSyncManager"

    var isCloudSyncEnabled: Boolean
        get() = sharedPrefs.getBoolean("cloud_sync_enabled", false)
        private set(value) {
            sharedPrefs.edit().putBoolean("cloud_sync_enabled", value).apply()
        }

    var lastSyncTime: String
        get() = sharedPrefs.getString("last_sync_time", "") ?: ""
        private set(value) {
            sharedPrefs.edit().putString("last_sync_time", value).apply()
        }

    suspend fun enableCloudSync(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            isCloudSyncEnabled = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling cloud sync", e)
            false
        }
    }

    fun disableCloudSync() {
        isCloudSyncEnabled = false
    }

    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        if (!isCloudSyncEnabled) return@withContext false

        try {
            val user = auth.currentUser ?: return@withContext false
            val userId = user.uid
            val userDocRef = db.collection("users").document(userId)

            val expenses = expenseDao.getAllExpenses().first()
            val categoriesJson = sharedPrefs.getString("custom_categories", null)

            val dataToUpload = hashMapOf(
                "updatedAt" to System.currentTimeMillis()
            )
            userDocRef.set(dataToUpload, SetOptions.merge()).await()

            val transactionsBatch = db.batch()
            val transactionsCol = userDocRef.collection("transactions")
            val currentMillis = System.currentTimeMillis()

            val oldTransactions = transactionsCol.get().await()
            for (doc in oldTransactions.documents) {
                val cloudUpdatedAt = doc.getLong("updatedAt") ?: 0L
                if (cloudUpdatedAt > currentMillis) {
                    continue
                }
                transactionsBatch.delete(doc.reference)
            }

            for (expense in expenses) {
                val docId = expense.id.toString()
                val docRef = transactionsCol.document(docId)
                val expenseMap = gson.fromJson<HashMap<String, Any>>(gson.toJson(expense), object : TypeToken<HashMap<String, Any>>() {}.type)
                expenseMap["updatedAt"] = currentMillis
                transactionsBatch.set(docRef, expenseMap)
            }
            transactionsBatch.commit().await()

            val categoriesBatch = db.batch()
            val categoriesCol = userDocRef.collection("categories")

            val oldCategories = categoriesCol.get().await()
            for (doc in oldCategories.documents) {
                categoriesBatch.delete(doc.reference)
            }

            if (!categoriesJson.isNullOrBlank()) {
                val type = object : TypeToken<List<CategoryData>>() {}.type
                val list: List<CategoryData> = gson.fromJson(categoriesJson, type)

                for (cat in list) {
                    val docRef = categoriesCol.document(cat.name)
                    categoriesBatch.set(docRef, cat)
                }
            }
            categoriesBatch.commit().await()

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            lastSyncTime = formatter.format(Date())

            Log.d(TAG, "Sync to cloud completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during syncNow", e)
            false
        }
    }

    suspend fun downloadCloudToLocal(): Boolean = withContext(Dispatchers.IO) {
        if (!isCloudSyncEnabled) return@withContext false

        try {
            val user = auth.currentUser ?: return@withContext false
            val userId = user.uid
            val userDocRef = db.collection("users").document(userId)

            val transactionsSnapshot = userDocRef.collection("transactions").get().await()
            val downloadedExpenses = transactionsSnapshot.documents.mapNotNull { it.toObject(ExpenseEntity::class.java) }

            val categoriesSnapshot = userDocRef.collection("categories").get().await()
            val downloadedCategories = categoriesSnapshot.documents.mapNotNull { it.toObject(CategoryData::class.java) }

            expenseDao.clearAll()
            if (downloadedExpenses.isNotEmpty()) {
                expenseDao.insertAll(downloadedExpenses)
            }

            if (downloadedCategories.isNotEmpty()) {
                sharedPrefs.edit().putString("custom_categories", gson.toJson(downloadedCategories)).apply()
            }

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            lastSyncTime = formatter.format(Date())

            Log.d(TAG, "Restore from cloud completed successfully")
            true
        } catch(e: Exception) {
             Log.e(TAG, "Error during downloadCloudToLocal", e)
             false
        }
    }
}
