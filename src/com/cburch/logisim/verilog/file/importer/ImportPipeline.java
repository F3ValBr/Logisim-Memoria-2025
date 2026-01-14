package com.cburch.logisim.verilog.file.importer;

import com.cburch.logisim.circuit.Circuit;

import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.CellFactoryRegistry;
import com.cburch.logisim.verilog.comp.auxiliary.*;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleBuilder;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleImpl;
import com.cburch.logisim.verilog.file.Strings;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysJsonNetlist;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysModuleDTO;
import com.cburch.logisim.verilog.file.ui.ImportProgress;
import com.cburch.logisim.verilog.file.ui.WarningCollector;
import com.cburch.logisim.verilog.layout.LayoutUtils;
import com.cburch.logisim.verilog.layout.MemoryIndex;
import com.cburch.logisim.verilog.layout.ModuleNetIndex;
import com.cburch.logisim.verilog.layout.auxiliary.NodeSizer;
import com.cburch.logisim.verilog.layout.builder.LayoutBuilder;
import com.cburch.logisim.verilog.layout.builder.LayoutRunner;
import com.cburch.logisim.verilog.std.ComponentAdapterRegistry;
import com.cburch.logisim.verilog.std.InstanceHandle;
import com.cburch.logisim.verilog.std.adapters.wordlvl.*;
import org.eclipse.elk.graph.ElkNode;

import java.awt.Graphics;
import java.util.*;

import static com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter.circuitHasAnyComponent;

final class ImportPipeline {

    private final Project proj;
    private final VerilogModuleBuilder builder;
    private final MemoryOpAdapter memoryAdapter;
    private final ComponentAdapterRegistry adapter;
    private final NodeSizer sizer;
    private final WarningCollector xWarnings;

    private final LayoutServices layout;
    private final TunnelPlacer tunnels;
    private final ConstantPlacer constants;
    private final SpecBuilder specs;
    private final ImportProgress progress;

    /**
     * Creates a new import pipeline.
     * @param proj Project where to import.
     * @param registry Cell factory registry to use for creating components.
     * @param builder Module builder to use for interpreting modules.
     * @param memoryAdapter Memory adapter to use for memory operations.
     * @param adapter Component adapter registry to use for creating components.
     * @param sizer Node sizer to use for layout.
     * @param xWarnings Warning collector to use for reporting issues.
     * @param layout Layout services to use for layout.
     * @param tunnels Tunnel placer to use for placing tunnels.
     * @param constants Constant placer to use for placing constants.
     * @param specs Specification builder to use for analyzing specifications.
     * @param progress Status of the importing process.
     */
    ImportPipeline(Project proj,
                   CellFactoryRegistry registry,
                   VerilogModuleBuilder builder,
                   MemoryOpAdapter memoryAdapter,
                   ComponentAdapterRegistry adapter,
                   NodeSizer sizer,
                   WarningCollector xWarnings,
                   LayoutServices layout,
                   TunnelPlacer tunnels,
                   ConstantPlacer constants,
                   SpecBuilder specs,
                   ImportProgress progress) {
        this.proj = proj;
        this.builder = builder;
        this.memoryAdapter = memoryAdapter;
        this.adapter = adapter;
        this.sizer = sizer;
        this.xWarnings = xWarnings;
        this.layout = layout;
        this.tunnels = tunnels;
        this.constants = constants;
        this.specs = specs;
        this.progress = (progress != null) ? progress : new ImportProgress() {
            @Override public void onStart(String msg) {}
            @Override public void onPhase(String msg) {}
            @Override public void onDone() {}
            @Override public void onError(String msg, Throwable cause) { cause.printStackTrace(); }
        };
    }

    /** Runs the import pipeline on the given netlist, importing all modules.
     * @return The main module's circuit.
     */
    Circuit run(YosysJsonNetlist netlist) {
        Graphics g = ImporterUtils.Geom.makeScratchGraphics();

        Circuit main = null;
        Map<String, Circuit> byModule = new HashMap<>();

        // 1) Recolectar módulos
        java.util.List<YosysModuleDTO> modules = new java.util.ArrayList<>();
        for (YosysModuleDTO dto : (Iterable<YosysModuleDTO>) netlist.modules()::iterator) {
            modules.add(dto);
        }

        progress.onStart(Strings.get("import.pipeline.start", modules.size()));

        // 2) Crear circuits (o resolver conflictos de nombre)
        for (YosysModuleDTO dto : modules) {
            Circuit target = ImporterUtils.Components.ensureCircuit(proj, dto.name());
            if (target == null) {
                progress.onDone();
                g.dispose();
                return null;
            }
            byModule.put(dto.name(), target);
        }

        // 3) Importar cada módulo
        for (YosysModuleDTO dto : modules) {
            progress.onPhase(Strings.get("import.pipeline.phase.module", dto.name()));

            if (main == null) {
                main = byModule.get(dto.name());
            }

            Circuit target = byModule.get(dto.name());
            try {
                importModuleIntoCircuit(dto, target, g,
                        /*actionKey*/ "addComponentsFromImportAction");
            } catch (Throwable t) {
                String msg = Strings.get("import.pipeline.error.module", dto.name());
                progress.onError(msg, t);

                g.dispose();
                if (t instanceof RuntimeException re) throw re;
                throw new RuntimeException(t);
            }
        }

        g.dispose();
        progress.onDone();
        return main;
    }

    /** Materializes a single module by name, if it exists in the netlist and its circuit is empty.
     * @param netlist Netlist to search for the module.
     * @param moduleName Name of the module to materialize.
     */
    void materializeSingleModule(YosysJsonNetlist netlist, String moduleName) {
        Graphics g = ImporterUtils.Geom.makeScratchGraphics();
        progress.onStart(Strings.get("import.pipeline.materialize.start", moduleName));

        try {
            for (YosysModuleDTO dto : (Iterable<YosysModuleDTO>) netlist.modules()::iterator) {
                if (!moduleName.equals(dto.name())) continue;

                try {
                    // build, layout, etc.
                    Circuit target = ImporterUtils.Components.ensureCircuit(proj, moduleName);
                    if (target == null) {
                        progress.onDone();
                        g.dispose();
                        return;
                    }

                    // Si ya tiene cosas, no lo pisamos
                    if (circuitHasAnyComponent(target)) {
                        progress.onDone();
                        g.dispose();
                        return;
                    }

                    importModuleIntoCircuit(dto, target, g,
                            /*actionKey*/ "materializeModuleAction");

                    progress.onDone();
                    g.dispose();
                    return;
                } catch (Throwable t) {
                    String msg = Strings.get("import.pipeline.error.materialize", moduleName);
                    progress.onError(msg, t);
                    progress.onDone();
                    g.dispose();
                    return;
                }
            }

            // No se encontró módulo con ese nombre
            progress.onDone();
        } finally {
            try { g.dispose(); } catch (Throwable ignore) {}
        }
    }

    /** Lógica común para importar UN módulo en UN circuito. */
    private void importModuleIntoCircuit(YosysModuleDTO dto,
                                         Circuit target,
                                         Graphics g,
                                         String batchActionKey) {
        final String modName = dto.name();

        // 1) build representation
        progress.onPhase(Strings.get("import.pipeline.phase.build", modName));
        VerilogModuleImpl mod = builder.buildModule(dto);
        ModuleNetIndex netIndex = builder.buildNetIndex(mod);
        MemoryIndex memIndex = builder.buildMemoryIndex(mod);
        memoryAdapter.beginModule(memIndex, mod);

        Map<VerilogCell, VerilogCell> alias = ImporterUtils.MemoryAlias.build(mod, memIndex);

        // 2) layout (ELK)
        progress.onPhase(Strings.get("import.pipeline.phase.layout", modName));
        LayoutBuilder.Result elk = LayoutBuilder.build(proj, mod, netIndex, sizer, alias);
        try {
            LayoutRunner.run(elk.root);
            LayoutUtils.applyLayoutAndClamp(elk.root, layout.minX(), layout.minY());

            Map<VerilogCell, InstanceHandle> cellHandles = new HashMap<>();
            Map<ModulePort, LayoutServices.PortAnchor> topAnchors = new HashMap<>();

            ImportBatch batch = new ImportBatch(target);

            // 3) pins
            progress.onPhase(Strings.get("import.pipeline.phase.pins", modName));
            layout.addModulePins(proj, target, mod, elk, g, topAnchors, batch);

            // 4) cells
            progress.onPhase(Strings.get("import.pipeline.phase.cells", modName));
            for (VerilogCell cell : mod.cells()) {
                if (alias.containsKey(cell)) continue;
                ElkNode n = elk.cellNode.get(cell);
                int x = (n == null) ? layout.minX() : ImporterUtils.Geom.snap((int) Math.round(n.getX()));
                int y = (n == null) ? layout.minY() : ImporterUtils.Geom.snap((int) Math.round(n.getY()));
                InstanceHandle h = adapter.create(
                        proj, target, g, cell,
                        Location.create(x + layout.separationInputCells(), y + 10)
                );
                cellHandles.put(cell, h);
            }

            // 5) tunnels + constants (agrupados en un batch)
            progress.onPhase(Strings.get("import.pipeline.phase.tunnels", modName));
            tunnels.place(batch, mod, cellHandles, topAnchors, g, specs);
            constants.place(batch, proj, mod, cellHandles, topAnchors, g, specs);
            batch.commit(proj, batchActionKey);

            // 6) rewrite tunnels (BLT → wires / túneles plain)
            progress.onPhase(Strings.get("import.pipeline.phase.rewrite", modName));
            try {
                BitLabeledTunnelRewriter.rewrite(proj, target, g);
            } catch (Throwable t) {
                progress.onError(Strings.get("import.pipeline.error.rewrite", modName), t);
            }

            alias.clear();
            cellHandles.clear();
            topAnchors.clear();
        } finally {
            try { org.eclipse.emf.ecore.util.EcoreUtil.delete(elk.root, true); } catch (Exception ignored) {}
            elk.cellNode.clear();
            elk.portNode.clear();
            elk.cellPorts.clear();
            elk.topPorts.clear();
            elk.root = null;
        }
    }
}
