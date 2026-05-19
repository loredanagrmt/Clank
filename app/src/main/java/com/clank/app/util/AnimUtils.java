package com.clank.app.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

public class AnimUtils {

  private AnimUtils() {}

  public static void animarLike(View view) {
    ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 1f);
    ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f, 1f);
    AnimatorSet set = new AnimatorSet();
    set.playTogether(scaleX, scaleY);
    set.setDuration(250);
    set.start();
  }
}
