package com.cburch.logisim.verilog.std.adapters;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.StdAttr;

import java.awt.*;

/** Núcleo reusable para “construir” celdas Yosys a partir de combinaciones Logisim (vainilla 2.7.1). */
public final class ComponentComposer {
    public static AttributeSet attrsWithWidthAndLabel(ComponentFactory f, int width, String label){
        AttributeSet a = f.createAttributeSet();
        try { a.setValue(StdAttr.WIDTH, BitWidth.create(Math.max(1, width))); } catch (Exception ignore) {}
        try { a.setValue(StdAttr.LABEL, label); } catch (Exception ignore) {}
        return a;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void setByNameParsed(AttributeSet as, String key, String text) {
        for (Attribute attr : as.getAttributes()) {
            if (attr.getName().equalsIgnoreCase(key)) {
                try { as.setValue(attr, attr.parse(text)); } catch (Exception ignore) {}
                return;
            }
        }
    }
    public static String hexAllOnes(int width){
        int n = Math.max(1,(width+3)/4);
        StringBuilder sb = new StringBuilder("0x");
        sb.append("F".repeat(n));
        return sb.toString();
    }

    /* === Pines “aproximados” (vainilla: formas estáticas) para uso en pinMap si lo necesitas === */
    public static Location pinComparatorEQ(Component cmp){ Location c=cmp.getLocation(); return Location.create(c.getX()+30, c.getY()); }
    public static Location pinNotOut(Component not){ Location c=not.getLocation(); return Location.create(c.getX()+10, c.getY()); }
}
