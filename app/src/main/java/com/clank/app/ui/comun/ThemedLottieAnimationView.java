package com.clank.app.ui.comun;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

public class ThemedLottieAnimationView extends LottieAnimationView {

    public ThemedLottieAnimationView(Context context) {
        super(context);
    }

    public ThemedLottieAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThemedLottieAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        int nightModeFlags = getContext().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean esModoOscuro = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        if (esModoOscuro) {
            setAnimation("animacion-cargando-inactivo.json");
        } else {
            setAnimation("animacion-cargando-activo.json");
        }

        setRepeatCount(LottieDrawable.INFINITE);
        playAnimation();
    }
}
