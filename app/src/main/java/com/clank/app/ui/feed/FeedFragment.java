package com.clank.app.ui.feed;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clank.app.R;
import com.clank.app.adapters.FeedAdapter;
import com.clank.app.data.model.Clank;
import com.clank.app.databinding.FragmentFeedBinding;
import com.clank.app.ui.comun.NavbarHost;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.lang.reflect.Method;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FeedFragment extends Fragment {

  private static final long DURACION_MINIMA_CARGANDO_MS = 1800L;
  private static final long ESPERA_PINTADO_RECYCLER_MS = 400L;

  private FragmentFeedBinding binding;
  private FeedViewModel viewModel;
  private FeedAdapter adapter;

  private final java.util.Set<String> observadosLikes = new java.util.HashSet<>();

  private Runnable ocultarOverlayPendiente;
  private boolean feedMostradoUnaVez = false;
  private long inicioCargandoMs = 0L;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    binding = FragmentFeedBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view,
                            @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(this).get(FeedViewModel.class);

    configurarRecyclerView();
    cargarAdapter();
  }

  @Override
  public void onResume() {
    super.onResume();

    if (requireActivity() instanceof NavbarHost) {
      ((NavbarHost) requireActivity()).mostrarNavbarConAccionYFiltro(
              getString(R.string.app_name),
              R.drawable.ic_buscar,
              v -> Navigation.findNavController(requireView())
                      .navigate(R.id.action_feed_a_busqueda),
              v -> Navigation.findNavController(requireView())
                      .navigate(R.id.action_feedFragment_to_filtrosFragment)
      );
    }

    if (binding != null
            && binding.overlayCargandoFeed.getVisibility() == View.VISIBLE
            && !feedMostradoUnaVez) {
      prepararAnimacionCargando(binding.overlayCargandoFeed);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    cancelarOcultacionPendiente();

    if (adapter != null) {
      adapter.cerrar();
    }

    for (String clankId : observadosLikes) {
      viewModel.detenerListenerLike(clankId);
    }

    observadosLikes.clear();
    binding = null;
  }

  private void configurarRecyclerView() {
    binding.rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.rvFeed.setHasFixedSize(false);

    int spacing = (int) getResources().getDimension(R.dimen.feed_item_spacing);

    binding.rvFeed.addItemDecoration(new RecyclerView.ItemDecoration() {
      @Override
      public void getItemOffsets(@NonNull Rect outRect,
                                 @NonNull View view,
                                 @NonNull RecyclerView parent,
                                 @NonNull RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) > 0) {
          outRect.top = spacing;
        }
      }
    });

    feedMostradoUnaVez = false;
    inicioCargandoMs = SystemClock.elapsedRealtime();

    binding.rvFeed.setVisibility(View.INVISIBLE);
    binding.tvFeedVacio.setVisibility(View.GONE);
    binding.overlayCargandoFeed.setVisibility(View.VISIBLE);

    prepararAnimacionCargando(binding.overlayCargandoFeed);
  }

  private void cargarAdapter() {
    FirestoreRecyclerOptions<Clank> options =
            new FirestoreRecyclerOptions.Builder<Clank>()
                    .setQuery(viewModel.getFeedQuery(), Clank.class)
                    .setLifecycleOwner(getViewLifecycleOwner())
                    .build();

    adapter = new FeedAdapter(
            options,
            requireContext(),
            viewModel.getUsuarioRepository(),
            viewModel.getCurrentUserId(),

            new FeedAdapter.OnClankClickListener() {
              @Override
              public void onClankClick(String clankId) {
                entrarDetalle(clankId);
              }

              @Override
              public void onUsuarioClick(String usuarioId) {
                Bundle args = new Bundle();
                args.putString("usuarioId", usuarioId);

                Navigation.findNavController(requireView())
                        .navigate(R.id.action_feed_a_perfil, args);
              }
            },

            clankId -> viewModel.toggleLike(clankId),

            this::arrancarObservadoresLikes,

            new FeedAdapter.OnPreparacionTarjetasListener() {
              @Override
              public void alIniciarPreparacion() {
                mostrarCargandoFeed();
              }

              @Override
              public void alFinalizarPreparacion() {
                mostrarFeedCuandoEstePintado();
              }
            }
    );

    binding.rvFeed.setAdapter(adapter);
  }

  private void arrancarObservadoresLikes(List<String> clankIds,
                                         List<Integer> numLikesIniciales) {
    java.util.Set<String> idsActuales = new java.util.HashSet<>(clankIds);

    java.util.Iterator<String> iterador = observadosLikes.iterator();

    while (iterador.hasNext()) {
      String clankIdObservado = iterador.next();

      if (!idsActuales.contains(clankIdObservado)) {
        viewModel.detenerListenerLike(clankIdObservado);
        iterador.remove();
      }
    }

    for (int i = 0; i < clankIds.size(); i++) {
      String clankId = clankIds.get(i);
      int likesInicial = numLikesIniciales.get(i);

      viewModel.iniciarListenerLike(clankId, likesInicial);

      if (observadosLikes.contains(clankId)) {
        continue;
      }

      observadosLikes.add(clankId);
      observarEstadoLike(clankId);
      observarContadorLike(clankId);
    }
  }

  private void observarEstadoLike(String clankId) {
    viewModel.getEstadoLike(clankId).observe(getViewLifecycleOwner(), isLiked -> {
      if (isLiked == null || adapter == null) {
        return;
      }

      adapter.actualizarEstadoLike(clankId, isLiked);

      int pos = encontrarPosicion(clankId);

      if (pos >= 0) {
        adapter.notifyItemChanged(pos, FeedAdapter.PAYLOAD_LIKE);
      }
    });
  }

  private void observarContadorLike(String clankId) {
    viewModel.getContadorLikes(clankId).observe(getViewLifecycleOwner(), contador -> {
      if (contador == null || adapter == null) {
        return;
      }

      adapter.actualizarContadorLike(clankId, contador);

      int pos = encontrarPosicion(clankId);

      if (pos >= 0) {
        adapter.notifyItemChanged(pos, FeedAdapter.PAYLOAD_LIKE);
      }
    });
  }

  private int encontrarPosicion(String clankId) {
    if (adapter == null) {
      return -1;
    }

    for (int i = 0; i < adapter.getItemCount(); i++) {
      if (clankId.equals(adapter.getSnapshots().getSnapshot(i).getId())) {
        return i;
      }
    }

    return -1;
  }

  private void mostrarCargandoFeed() {
    if (binding == null) {
      return;
    }

    cancelarOcultacionPendiente();

    if (feedMostradoUnaVez) {
      binding.overlayCargandoFeed.setVisibility(View.GONE);
      binding.rvFeed.setVisibility(
              adapter != null && adapter.getCantidadRealFirestore() > 0
                      ? View.VISIBLE
                      : View.GONE
      );
      return;
    }

    if (inicioCargandoMs == 0L) {
      inicioCargandoMs = SystemClock.elapsedRealtime();
    }

    binding.tvFeedVacio.setVisibility(View.GONE);
    binding.rvFeed.setVisibility(View.INVISIBLE);
    binding.overlayCargandoFeed.setVisibility(View.VISIBLE);

    prepararAnimacionCargando(binding.overlayCargandoFeed);
  }

  private void mostrarFeedCuandoEstePintado() {
    if (binding == null || adapter == null) {
      return;
    }

    cancelarOcultacionPendiente();

    if (!adapter.estaPreparado()) {
      if (!feedMostradoUnaVez) {
        binding.tvFeedVacio.setVisibility(View.GONE);
        binding.rvFeed.setVisibility(View.INVISIBLE);
        binding.overlayCargandoFeed.setVisibility(View.VISIBLE);
        prepararAnimacionCargando(binding.overlayCargandoFeed);
      }
      return;
    }

    boolean hayClanks = adapter.getCantidadRealFirestore() > 0;

    if (!hayClanks) {
      long esperaRestante = calcularEsperaRestanteCargando();

      ocultarOverlayPendiente = () -> {
        if (binding == null) {
          return;
        }

        feedMostradoUnaVez = true;
        inicioCargandoMs = 0L;

        binding.rvFeed.setVisibility(View.GONE);
        binding.tvFeedVacio.setVisibility(View.VISIBLE);
        binding.overlayCargandoFeed.setVisibility(View.GONE);
      };

      binding.overlayCargandoFeed.postDelayed(
              ocultarOverlayPendiente,
              esperaRestante
      );
      return;
    }

    binding.tvFeedVacio.setVisibility(View.GONE);
    binding.rvFeed.setVisibility(View.VISIBLE);

    if (feedMostradoUnaVez) {
      binding.overlayCargandoFeed.setVisibility(View.GONE);
      return;
    }

    binding.overlayCargandoFeed.setVisibility(View.VISIBLE);
    prepararAnimacionCargando(binding.overlayCargandoFeed);

    long esperaRestante = calcularEsperaRestanteCargando();
    long esperaFinal = Math.max(ESPERA_PINTADO_RECYCLER_MS, esperaRestante);

    ocultarOverlayPendiente = () -> {
      if (binding == null || adapter == null) {
        return;
      }

      if (!adapter.estaPreparado() || adapter.getCantidadRealFirestore() <= 0) {
        binding.rvFeed.setVisibility(View.INVISIBLE);
        binding.tvFeedVacio.setVisibility(View.GONE);
        binding.overlayCargandoFeed.setVisibility(View.VISIBLE);
        prepararAnimacionCargando(binding.overlayCargandoFeed);
        return;
      }

      binding.rvFeed.post(() -> {
        if (binding == null) {
          return;
        }

        feedMostradoUnaVez = true;
        inicioCargandoMs = 0L;

        binding.rvFeed.setVisibility(View.VISIBLE);
        binding.tvFeedVacio.setVisibility(View.GONE);
        binding.overlayCargandoFeed.setVisibility(View.GONE);
      });
    };

    binding.overlayCargandoFeed.postDelayed(ocultarOverlayPendiente, esperaFinal);
  }

  private void prepararAnimacionCargando(View vista) {
    if (vista == null) {
      return;
    }

    vista.setVisibility(View.VISIBLE);
    vista.invalidate();
    vista.requestLayout();

    arrancarAnimacionSiExiste(vista);

    vista.post(() -> {
      if (binding == null || feedMostradoUnaVez) {
        return;
      }

      arrancarAnimacionSiExiste(vista);
      vista.invalidate();
    });

    vista.postDelayed(() -> {
      if (binding == null || feedMostradoUnaVez) {
        return;
      }

      arrancarAnimacionSiExiste(vista);
      vista.invalidate();
    }, 300);
  }

  private void arrancarAnimacionSiExiste(View vista) {
    if (vista == null) {
      return;
    }

    intentarInvocarAnimacion(vista);

    if (vista instanceof ViewGroup) {
      ViewGroup grupo = (ViewGroup) vista;

      for (int i = 0; i < grupo.getChildCount(); i++) {
        arrancarAnimacionSiExiste(grupo.getChildAt(i));
      }
    }
  }

  private void intentarInvocarAnimacion(View vista) {
    String nombreClase = vista.getClass().getName();

    if (!nombreClase.toLowerCase().contains("lottie")
            && !nombreClase.toLowerCase().contains("animation")) {
      return;
    }

    vista.setVisibility(View.VISIBLE);

    try {
      Method setRepeatCount = vista.getClass().getMethod("setRepeatCount", int.class);
      setRepeatCount.invoke(vista, -1);
    } catch (Exception ignored) {
    }

    try {
      Method cancelAnimation = vista.getClass().getMethod("cancelAnimation");
      cancelAnimation.invoke(vista);
    } catch (Exception ignored) {
    }

    try {
      Method setProgress = vista.getClass().getMethod("setProgress", float.class);
      setProgress.invoke(vista, 0f);
    } catch (Exception ignored) {
    }

    try {
      Method playAnimation = vista.getClass().getMethod("playAnimation");
      playAnimation.invoke(vista);
    } catch (Exception ignored) {
    }
  }

  private long calcularEsperaRestanteCargando() {
    if (inicioCargandoMs == 0L) {
      return DURACION_MINIMA_CARGANDO_MS;
    }

    long transcurrido = SystemClock.elapsedRealtime() - inicioCargandoMs;
    return Math.max(0L, DURACION_MINIMA_CARGANDO_MS - transcurrido);
  }

  private void cancelarOcultacionPendiente() {
    if (binding != null && ocultarOverlayPendiente != null) {
      binding.overlayCargandoFeed.removeCallbacks(ocultarOverlayPendiente);
      binding.rvFeed.removeCallbacks(ocultarOverlayPendiente);
    }

    ocultarOverlayPendiente = null;
  }

  private void entrarDetalle(String clankId) {
    if (clankId == null || clankId.trim().isEmpty()) {
      return;
    }

    Bundle args = new Bundle();
    args.putString("clankId", clankId);

    Navigation.findNavController(requireView())
            .navigate(R.id.action_feed_a_detalle_clank, args);
  }
}