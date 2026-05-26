package com.clank.app.di;

import com.google.firebase.storage.FirebaseStorage;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public final class FirebaseModule {
  @Provides
  @Singleton
  public static FirebaseStorage provideFirebaseStorage() {
    return FirebaseStorage.getInstance();
  }
}
