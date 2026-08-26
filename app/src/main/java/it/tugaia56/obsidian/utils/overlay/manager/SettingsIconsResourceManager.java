package it.tugaia56.obsidian.utils.overlay.manager;

import java.io.IOException;

import it.tugaia56.obsidian.utils.ObsidianTheme;
import it.tugaia56.obsidian.utils.overlay.compiler.SettingsIconsCompiler;

/** Porting di OC's SettingsIconsResourceManager — costruisce il file res/values/Obsidian.xml
 *  dinamico (colore icone/sfondo, forma sfondo) passato all'overlay compiler. */
public class SettingsIconsResourceManager {

    public static boolean buildOverlay(
            int iconSet,
            int backgroundColor,
            int backgroundShape,
            boolean backgroundSolid,
            int iconColor
    ) throws IOException {
        String resources = "";

        if (iconSet == 6) {
            // OOS Stock: forma e colore reali, nessuna opzione utente. Le icone sono comunque
            // avvolte in un layer-list con uno sfondo trasparente (invece di un vettore nudo):
            // su questa ROM le icone delle preferenze sembrano ricevere una tinta forzata
            // dall'ImageView quando il drawable è un vettore semplice, ma non quando è un
            // LayerDrawable — lo sfondo trasparente serve solo a ottenere quel "tipo" di
            // drawable, resta invisibile.
            resources += "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<resources>\n" +
                    "    <color name=\"bg_color\">#00000000</color>\n" +
                    "    <color name=\"solid_bg_color\">#00000000</color>\n" +
                    "    <dimen name=\"top_left\">0dp</dimen>\n" +
                    "    <dimen name=\"top_right\">0dp</dimen>\n" +
                    "    <dimen name=\"bottom_left\">0dp</dimen>\n" +
                    "    <dimen name=\"bottom_right\">0dp</dimen>\n" +
                    "</resources>";
            return SettingsIconsCompiler.buildOverlay(iconSet, resources);
        } else if (iconSet == 1 || iconSet == 2 || iconSet == 4) {
            // PUI Icon Pack
            resources += "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<resources>\n" +
                    "    <color name=\"monet_color\">" +
                    getIconColor(iconColor) +
                    "</color>\n";

        } else {
            resources += "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<resources>\n" +
                    "    <color name=\"bg_color\">" +
                    getBackgroundColor(backgroundColor) +
                    "</color>\n";

            resources += "    <color name=\"solid_bg_color\">" +
                    (backgroundSolid ? getBackgroundColor(backgroundColor) : "#00000000") +
                    "</color>\n";

            resources += "    <color name=\"icon_color\">" +
                    getIconColor(iconColor) +
                    "</color>\n";

            switch (backgroundShape) {
                // Circle
                case 0 -> resources += "    <dimen name=\"top_left\">18dp</dimen>\n" +
                        "    <dimen name=\"top_right\">18dp</dimen>\n" +
                        "    <dimen name=\"bottom_left\">18dp</dimen>\n" +
                        "    <dimen name=\"bottom_right\">18dp</dimen>\n";
                // Squircle
                case 1 -> resources += "    <dimen name=\"top_left\">15dp</dimen>\n" +
                        "    <dimen name=\"top_right\">15dp</dimen>\n" +
                        "    <dimen name=\"bottom_left\">15dp</dimen>\n" +
                        "    <dimen name=\"bottom_right\">15dp</dimen>\n";
                // Rounded Square
                case 2 -> resources += "    <dimen name=\"top_left\">4.0dp</dimen>\n" +
                        "    <dimen name=\"top_right\">4.0dp</dimen>\n" +
                        "    <dimen name=\"bottom_left\">4.0dp</dimen>\n" +
                        "    <dimen name=\"bottom_right\">4.0dp</dimen>\n";
                // Teardrop
                case 3 -> resources += "    <dimen name=\"top_left\">90.0dp</dimen>\n" +
                        "    <dimen name=\"top_right\">90.0dp</dimen>\n" +
                        "    <dimen name=\"bottom_left\">90.0dp</dimen>\n" +
                        "    <dimen name=\"bottom_right\">24.0dp</dimen>\n";
                // Rhombus
                case 4 -> resources += "    <dimen name=\"top_left\">2.0dp</dimen>\n" +
                        "    <dimen name=\"top_right\">14.0dp</dimen>\n" +
                        "    <dimen name=\"bottom_left\">14.0dp</dimen>\n" +
                        "    <dimen name=\"bottom_right\">2.0dp</dimen>\n";
            }
        }

        resources += "</resources>";

        return SettingsIconsCompiler.buildOverlay(iconSet, resources);
    }

    private static String getIconColor(int iconColor) {
        return switch (iconColor) {
            // "@*android:color/accent_material_dark" (OC's original reference) resolves to a
            // ROM-defined legacy resource, not the user's Obsidian accent — on this OOS16
            // build it's orange (#FF9800), unrelated to DST_ACCENT1. Use the real accent value.
            case 0 -> String.format("#%08X", ObsidianTheme.accentColor());
            case 1 -> "#FFFFFF";
            default -> "#000000";
        };
    }

    private static String getBackgroundColor(int backgroundColor) {
        return switch (backgroundColor) {
            case 0 -> String.format("#%08X", ObsidianTheme.accentColor());
            case 1 -> "#FFFFFF";
            default -> "#000000";
        };
    }
}
