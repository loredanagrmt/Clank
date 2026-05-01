package com.clank.app.data.repository;

import com.google.firebase.auth.FirebaseAuth;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthRepository {

  private final FirebaseAuth auth;

  @Inject
  public AuthRepository() {
    this.auth = FirebaseAuth.getInstance();
  }

  public String getUid() {
    return auth.getCurrentUser() != null
            ? auth.getCurrentUser().getUid()
            : null;
  }
}