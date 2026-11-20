package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.QuranBookmarkDoc
import com.example.data.model.UserPreferencesDoc
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String): String {
        val currentUid = auth.currentUser?.uid
            ?: throw IllegalStateException("User is not signed in.")
        check(currentUid == expectedUid) { "Active account changed." }
        return currentUid
    }

    fun observeBookmarks(userId: String): Flow<List<QuranBookmarkDoc>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/bookmarks"
        emitAll(
            db.collection("users").document(uid).collection("bookmarks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QuranBookmarkDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePreferences(userId: String): Flow<UserPreferencesDoc?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/preferences/user_settings"
        emitAll(
            db.collection("users").document(uid).collection("preferences")
                .document("user_settings")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserPreferencesDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertBookmark(userId: String, bookmarkId: String, doc: QuranBookmarkDoc): Result<Unit> {
        val docPath = "users/$userId/bookmarks/$bookmarkId"
        return try {
            val uid = requireUserId(userId)
            require(bookmarkId.isNotBlank()) { "Bookmark ID cannot be empty" }
            val ref = db.collection("users").document(uid)
                .collection("bookmarks").document(bookmarkId)
            ref.set(doc.toCreateMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBookmark(userId: String, bookmarkId: String): Result<Unit> {
        val docPath = "users/$userId/bookmarks/$bookmarkId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid)
                .collection("bookmarks").document(bookmarkId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun savePreferences(userId: String, doc: UserPreferencesDoc): Result<Unit> {
        val docPath = "users/$userId/preferences/user_settings"
        return try {
            val uid = requireUserId(userId)
            val ref = db.collection("users").document(uid)
                .collection("preferences").document("user_settings")
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(doc.toUpdateMap()).await()
            } else {
                ref.set(doc.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
