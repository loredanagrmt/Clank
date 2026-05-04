package com.clank.app.di;

import com.clank.app.data.repository.AuthRepository;
import com.clank.app.data.repository.ClankRepository;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@EntryPoint
@InstallIn(SingletonComponent.class)
public interface AdapterEntryPoint {

  AuthRepository authRepository();
  ClankRepository clankRepository();

}
