package com.cburch.logisim.verilog.comp.auxiliary;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public final class FactoryLookup {
    private FactoryLookup() {}

    public static ComponentFactory findFactory(Library lib, String nameOrDisplay) {
        if (lib == null || nameOrDisplay == null) return null;
        for (Tool t : lib.getTools()) {
            if (t instanceof AddTool add) {
                ComponentFactory f = add.getFactory(); // carga perezosa OK
                if (f == null) continue;
                if (nameOrDisplay.equals(f.getName()) ||
                        nameOrDisplay.equals(f.getDisplayName())) {
                    return f;
                }
            }
        }
        return null;
    }

    /** Búsqueda en varias bibliotecas por orden de preferencia. */
    public static ComponentFactory findFactory(LogisimFile file, String[] libraryNames, String nameOrDisplay) {
        if (file == null) return null;
        for (String libName : libraryNames) {
            Library lib = file.getLibrary(libName);
            ComponentFactory f = findFactory(lib, nameOrDisplay);
            if (f != null) return f;
        }
        return null;
    }
}

