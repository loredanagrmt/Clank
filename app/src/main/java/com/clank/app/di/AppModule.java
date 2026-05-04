package com.clank.app.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public final class AppModule {

  @Provides
  @Singleton
  public static FirebaseAuth provideFirebaseAuth() {
    return FirebaseAuth.getInstance();
  }

  @Provides
  @Singleton
  public static FirebaseFirestore provideFirebaseFirestore() {
    return FirebaseFirestore.getInstance();
  }

  @Provides
  @Singleton
  public static FirebaseFunctions provideFirebaseFunctions() {
    return FirebaseFunctions.getInstance("us-central1");
  }
}
