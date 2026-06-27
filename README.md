# Conversor CSV Energía XXI

Aplicación Android que convierte ficheros CSV de consumo eléctrico (formato distribuidora) al formato requerido por servicios externos.

## Funcionalidades

- Selección de fichero CSV de consumo
- Validación completa: detecta horas faltantes, duplicadas y días con datos incompletos
- Conversión de Wh a kWh
- Selección entre método de obtención **Real** (lectura de contador) o **Estimado** (estimación de la distribuidora)
- Guardado del archivo convertido con nombre sugerido `consumption_YYYY_MM_DD__YYYY_MM_DD.csv`
- Multidioma: Español, Catalán, Gallego, Euskera, Leonés y Aranés
- Sin permisos de Internet ni datos personales
- Código abierto (GPL-3.0)

## Capturas

*(Pendiente)*

## Compilar

Con Android Studio: abre la carpeta `Android/`, sincroniza Gradle y ejecuta.

Desde terminal:

```bash
./gradlew assembleDebug
```

La APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Licencia

GNU General Public License v3.0.
