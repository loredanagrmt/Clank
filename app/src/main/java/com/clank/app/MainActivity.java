package com.clank.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.clank.app.databinding.ActivityMainBinding;
import com.clank.app.databinding.NavbarSuperiorBinding;
import com.clank.app.ui.comun.NavbarHost;
import com.clank.app.util.GestorIdioma;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements NavbarHost {

    private ActivityMainBinding binding;
    private NavbarSuperiorBinding navbarBinding;

    // Se oculta en Logo e Idioma, Portada, Bienvenida, InicioSesion,
    // Registro, EditarPerfil, CompletarPerfil, CambiarContrasenia, BorrarCuenta, CerrarSesion, OpcionesClankPerfil,
    // OpcionesCLankDetalle, OpcionesColeccion, Borrar y Publicar)
    private static final Set<Integer> FRAGMENTS_SIN_BOTTOMBAR = new HashSet<>(Arrays.asList(
            R.id.logoFragment,
            R.id.elegirIdiomaFragment,
            R.id.inspirarFragment,
            R.id.bienvenidaFragment,
            R.id.inicioSesionFragment,
            R.id.registroFragment,
            R.id.completarPerfilFragment,
            R.id.crearFragment,
            R.id.editarClankFragment,
            R.id.olvideContrasenyaFragment,
            R.id.codigoRecuperacionContrasenyaFragment,
            R.id.nuevaContrasenyaFragment,
            R.id.cambiarContrasenyaFragment
            // R.id.detalleFragment,
            // R.id.editarPerfilFragment
    ));

    @Override
    protected void attachBaseContext(Context newBase) {
        String idioma = newBase.getSharedPreferences("clank_prefs", Context.MODE_PRIVATE)
                .getString("idioma_seleccionado", "es");

        Locale locale = Locale.forLanguageTag(idioma);
        Locale.setDefault(locale);

        Configuration config = newBase.getResources().getConfiguration();
        config = new Configuration(config);
        config.setLocale(locale);

        Context contextConLocale = newBase.createConfigurationContext(config);
        super.attachBaseContext(contextConLocale);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorIdioma.getInstance(getApplicationContext()).aplicarIdiomaGuardado();
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navbarBinding = NavbarSuperiorBinding.bind(binding.navbar.getRoot());

        configurarNavbar();

        binding.getRoot().post(this::configurarBottombar);
    }

    ///////////////////////// configuración inicial navbar /////////////////////////

    private void configurarNavbar() {
        restaurarAccionVolverPorDefecto();
    }

    private void restaurarAccionVolverPorDefecto() {
        navbarBinding.btnNavbarVolver.setOnClickListener(v ->
                obtenerNavController().navigateUp()
        );
    }

    private void prepararNavbar(String titulo, boolean mostrarVolver) {
        navbarBinding.tvNavbarTitulo.setText(titulo);

        navbarBinding.btnNavbarVolver.setVisibility(
                mostrarVolver ? View.VISIBLE : View.GONE
        );
        navbarBinding.btnNavbarVolver.setEnabled(true);
        navbarBinding.btnNavbarVolver.setAlpha(1f);
        restaurarAccionVolverPorDefecto();

        navbarBinding.btnNavbarAccion.setVisibility(View.GONE);
        navbarBinding.btnNavbarAccion.setOnClickListener(null);

        navbarBinding.btnNavbarFiltrar.setVisibility(View.GONE);
        navbarBinding.btnNavbarFiltrar.setOnClickListener(null);

        binding.navbar.getRoot().setVisibility(View.VISIBLE);
    }

    ///////////////////////// configuración bottom bar /////////////////////////

    private void configurarBottombar() {
        NavController nav = obtenerNavController();

        ///////////////////////// mostrar/ocultar y marcar pulsado /////////////////////////
        nav.addOnDestinationChangedListener((controller, destination, args) -> {
            if (FRAGMENTS_SIN_BOTTOMBAR.contains(destination.getId())) {
                binding.frameBottomBar.setVisibility(View.GONE);
            } else {
                binding.frameBottomBar.setVisibility(View.VISIBLE);
                actualizarIndicador(destination.getId());
            }
        });

        ///////////////////////// estado inicial de la bottom bar /////////////////////////
        if (nav.getCurrentDestination() != null) {
            int destinoActual = nav.getCurrentDestination().getId();

            if (FRAGMENTS_SIN_BOTTOMBAR.contains(destinoActual)) {
                binding.frameBottomBar.setVisibility(View.GONE);
            } else {
                binding.frameBottomBar.setVisibility(View.VISIBLE);
                actualizarIndicador(destinoActual);
            }
        }

        ///////////////////////// listeners /////////////////////////
        binding.bottomBar.btnNavFeed.setOnClickListener(v -> {
            if (fragmentActual() != R.id.feedFragment) {
                NavOptions opciones = new NavOptions.Builder()
                        .setPopUpTo(R.id.feedFragment, true)
                        .setLaunchSingleTop(true)
                        .build();

                nav.navigate(R.id.feedFragment, null, opciones);
            }
        });

        binding.bottomBar.btnNavCrear.setOnClickListener(v -> {
            if (fragmentActual() != R.id.crearFragment) {
                NavOptions opciones = new NavOptions.Builder()
                        .setPopUpTo(R.id.feedFragment, false)
                        .setLaunchSingleTop(true)
                        .build();

                nav.navigate(R.id.crearFragment, null, opciones);
            }
        });

        binding.bottomBar.btnNavPerfil.setOnClickListener(v -> {
            if (fragmentActual() != R.id.perfilFragment) {
                NavOptions opciones = new NavOptions.Builder()
                        .setPopUpTo(R.id.feedFragment, false)
                        .setLaunchSingleTop(true)
                        .build();

                nav.navigate(R.id.perfilFragment, null, opciones);
            }
        });
    }

    ///////////////////////// indicador al pulsar botones /////////////////////////

    private void actualizarIndicador(int fragmentId) {
        binding.bottomBar.indicadorFeed.setVisibility(View.INVISIBLE);
        binding.bottomBar.indicadorCrear.setVisibility(View.INVISIBLE);
        binding.bottomBar.indicadorPerfil.setVisibility(View.INVISIBLE);

        if (fragmentId == R.id.feedFragment) {
            binding.bottomBar.indicadorFeed.setVisibility(View.VISIBLE);
        } else if (fragmentId == R.id.crearFragment) {
            binding.bottomBar.indicadorCrear.setVisibility(View.VISIBLE);
        } else if (fragmentId == R.id.perfilFragment) {
            binding.bottomBar.indicadorPerfil.setVisibility(View.VISIBLE);
        }
    }

    ///////////////////////// métodos NavbarHost /////////////////////////

    @Override
    public void mostrarNavbar(String titulo) {
        prepararNavbar(titulo, false);
    }

    @Override
    public void mostrarNavbar(
            String titulo,
            @DrawableRes int iconoAccion,
            View.OnClickListener onAccion
    ) {
        prepararNavbar(titulo, false);

        navbarBinding.btnNavbarAccion.setImageResource(iconoAccion);
        navbarBinding.btnNavbarAccion.setOnClickListener(onAccion);
        navbarBinding.btnNavbarAccion.setVisibility(View.VISIBLE);
    }

    @Override
    public void mostrarNavbar(
            String titulo,
            @Nullable Integer iconoAccion,
            @Nullable View.OnClickListener onAccion
    ) {
        prepararNavbar(titulo, false);

        if (iconoAccion != null && onAccion != null) {
            navbarBinding.btnNavbarAccion.setImageResource(iconoAccion);
            navbarBinding.btnNavbarAccion.setOnClickListener(onAccion);
            navbarBinding.btnNavbarAccion.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void mostrarNavbarConVolver(String titulo) {
        prepararNavbar(titulo, true);
    }

    @Override
    public void mostrarNavbarConVolver(
            String titulo,
            @DrawableRes int iconoAccion,
            View.OnClickListener onAccion
    ) {
        prepararNavbar(titulo, true);

        navbarBinding.btnNavbarAccion.setImageResource(iconoAccion);
        navbarBinding.btnNavbarAccion.setOnClickListener(onAccion);
        navbarBinding.btnNavbarAccion.setVisibility(View.VISIBLE);
    }

    @Override
    public void mostrarNavbarConAccionYFiltro(
            String titulo,
            @DrawableRes int iconoAccion,
            View.OnClickListener onAccion,
            View.OnClickListener onFiltrar
    ) {
        prepararNavbar(titulo, false);

        navbarBinding.btnNavbarAccion.setImageResource(iconoAccion);
        navbarBinding.btnNavbarAccion.setOnClickListener(onAccion);
        navbarBinding.btnNavbarAccion.setVisibility(View.VISIBLE);

        navbarBinding.btnNavbarFiltrar.setOnClickListener(onFiltrar);
        navbarBinding.btnNavbarFiltrar.setVisibility(View.VISIBLE);
    }

    @Override
    public void configurarAccionVolver(@Nullable View.OnClickListener onVolver) {
        if (onVolver != null) {
            navbarBinding.btnNavbarVolver.setOnClickListener(onVolver);
        } else {
            restaurarAccionVolverPorDefecto();
        }
    }

    @Override
    public void habilitarVolverNavbar(boolean habilitado) {
        navbarBinding.btnNavbarVolver.setEnabled(habilitado);
        navbarBinding.btnNavbarVolver.setAlpha(habilitado ? 1f : 0.5f);
    }

    @Override
    public void ocultarNavbar() {
        binding.navbar.getRoot().setVisibility(View.GONE);
    }

    ///////////////////////// utilidades navegación /////////////////////////

    private NavController obtenerNavController() {
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            throw new IllegalStateException(
                    "No se ha encontrado el NavHostFragment principal."
            );
        }

        return navHostFragment.getNavController();
    }

    private int fragmentActual() {
        NavController nav = obtenerNavController();

        return nav.getCurrentDestination() != null
                ? nav.getCurrentDestination().getId()
                : -1;
    }
}