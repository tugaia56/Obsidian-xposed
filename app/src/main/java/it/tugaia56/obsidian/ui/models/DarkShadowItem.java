package it.tugaia56.obsidian.ui.models;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class DarkShadowItem {
    private final String name;
    private final String overlayName;
    private final List<String> packages;
    private final List<String> resourceNames;
    private final Map<String, Integer> adjustColors;
    private int color;
    private boolean enabled;
    /** Riga bloccata (es. "Attivo" quando "Collega all'Accento" è attivo) — mostrata
     *  attenuata e non toccabile, invece di lasciare che il picker prevalga in silenzio. */
    private boolean locked;

    public DarkShadowItem(String name, String overlayName, List<String> packages,
                          List<String> resourceNames, Map<String, Integer> adjustColors, int color, boolean enabled) {
        this.name          = name;
        this.overlayName   = overlayName;
        this.packages      = packages;
        this.resourceNames = resourceNames;
        this.adjustColors  = adjustColors != null ? adjustColors : new LinkedHashMap<>();
        this.color         = color;
        this.enabled       = enabled;
    }

    public String             getName()          { return name; }
    public String             getOverlayName()   { return overlayName; }
    public List<String>       getPackages()      { return packages; }
    public List<String>       getResourceNames() { return resourceNames; }
    public Map<String, Integer> getAdjustColors(){ return adjustColors; }
    public int                getColor()         { return color; }
    public boolean            isEnabled()        { return enabled; }
    public boolean            isLocked()         { return locked; }
    public void               setColor(int c)    { this.color = c; }
    public void               setEnabled(boolean e) { this.enabled = e; }
    public void               setLocked(boolean l)  { this.locked = l; }
}
