package com.clank.app.util;

import android.content.Context;
import com.clank.app.R;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class FechaUtils {

  private FechaUtils() {}

  public static String formatearFechaRelativa(Context context, Date fecha) {
    if (fecha == null) return "";
    long diferencia = Math.max(0, System.currentTimeMillis() - fecha.getTime());
    long minutos = TimeUnit.MILLISECONDS.toMinutes(diferencia);
    long horas   = TimeUnit.MILLISECONDS.toHours(diferencia);
    long dias    = TimeUnit.MILLISECONDS.toDays(diferencia);
    long meses   = dias / 30;
    long anyos   = dias / 365;

    if (minutos < 1)  return context.getString(R.string.feed_ahora);
    if (minutos < 60) return context.getString(R.string.feed_hace_minutos, minutos);
    if (horas < 24)   return context.getString(R.string.feed_hace_horas, horas);
    if (dias < 30)    return context.getString(R.string.feed_hace_dias, dias);
    if (meses < 12)   return context.getString(R.string.feed_hace_meses, meses);
    return context.getString(R.string.feed_hace_anyos, anyos);
  }
}
