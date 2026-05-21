package com.clank.app.ui.logo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.clank.app.R;
import com.clank.app.ui.comun.NavbarHost;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LogoFragment extends Fragment {

    private LogoViewModel vistaModelo;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable navegarRunnable = new Runnable() {
        @Override
        public void run() {
            navegarSiSigueEnLogo();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_logo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vistaModelo = new ViewModelProvider(this).get(LogoViewModel.class);

        handler.removeCallbacks(navegarRunnable);
        handler.postDelayed(navegarRunnable, 1000);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof NavbarHost) {
            ((NavbarHost) requireActivity()).ocultarNavbar();
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(navegarRunnable);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(navegarRunnable);
        super.onDestroy();
    }

    private void navegarSiSigueEnLogo() {
        if (!isAdded()) {
            return;
        }

        View vista = getView();

        if (vista == null) {
            return;
        }

        NavController navController = Navigation.findNavController(vista);

        if (navController.getCurrentDestination() == null) {
            return;
        }

        if (navController.getCurrentDestination().getId() != R.id.logoFragment) {
            return;
        }

        if (vistaModelo != null && vistaModelo.haySesionIniciada()) {
            navController.navigate(R.id.action_logo_a_feed);
        } else {
            navController.navigate(R.id.action_logo_a_idioma);
        }
    }
}