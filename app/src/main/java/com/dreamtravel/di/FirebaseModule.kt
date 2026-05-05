package com.dreamtravel.di

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(@ApplicationContext context: Context): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.w("FirebaseModule", "Firebase not initialized, skipping Auth provider")
                return null
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseModule", "Failed to get FirebaseAuth: ${e.message}")
            null
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(@ApplicationContext context: Context): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.w("FirebaseModule", "Firebase not initialized, skipping Firestore provider")
                return null
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseModule", "Failed to get FirebaseFirestore: ${e.message}")
            null
        }
    }
}
