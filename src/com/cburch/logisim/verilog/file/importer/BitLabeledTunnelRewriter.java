package com.cburch.logisim.verilog.file.importer;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.BitLabeledTunnel;
import com.cburch.logisim.std.wiring.Tunnel;
import com.cburch.logisim.verilog.file.importer.routing.GridRouter;
import com.cburch.logisim.verilog.file.importer.routing.MstPlanner;
import com.cburch.logisim.verilog.file.importer.routing.RouterUtils;
import com.cburch.logisim.verilog.std.Strings;

import java.awt.*;
import java.util.*;
import java.util.List;

import static com.cburch.logisim.verilog.file.importer.VerilogJsonImporter.GRID;

/**
 * Rewrites BitLabeledTunnels (BLT) into direct wires when possible.
 * <p>
 * Strategy:
 * 1) Collect all BLTs and group them by (normalized label, normalized token list).
 * 2) Keep only groups that are "isolated" (no relevant token shared with other groups) and size >= 2.
 * 3) For each group:
 *    - Identify each BLT "mouth" (the BLT's pin location).
 *    - Identify and cache the small "stub" wires that connect the BLT mouth to the real circuit port.
 *      (Those stubs are created by TunnelPlacer as two orthogonal segments.)
 *    - Route an MST across BLT mouths using A*-Manhattan grid routing avoiding obstacles.
 *    - If ALL edges route successfully:
 *        * Add routed wires
 *        * Remove old stubs
 *        * Reconnect each real anchor -> (orthogonal) -> BLT mouth
 *        * Remove BLT components
 *      Else:
 *        * Fallback: convert BLTs to plain Tunnel components.
 * <p>
 * Notes:
 * - We route between BLT mouths (not real anchors) to avoid starting inside component bounds.
 * - We still reconnect to the real anchors so the final wiring attaches to the ports.
 */
public final class BitLabeledTunnelRewriter {

    private BitLabeledTunnelRewriter() {}

    /** Rewrite BitLabeledTunnels in the given circuit.
     * @param proj Project (for actions).
     * @param circ Circuit to rewrite.
     * @param g Graphics context (for measuring components).
     */
    public static void rewrite(Project proj, Circuit circ, Graphics g) {
        if (proj == null || circ == null) return;

        // 1) Collect BLTs
        List<TunnelInfo> all = collectBlt(circ);
        if (all.isEmpty()) return;

        // 2) Group by (labelNorm, tokensNorm)
        Map<GroupKey, List<TunnelInfo>> groups = groupByLabelAndSpecs(all);

        // 3) Select isolated groups with >= 2 members
        List<List<TunnelInfo>> rewriteGroups = new ArrayList<>();
        for (Map.Entry<GroupKey, List<TunnelInfo>> e : groups.entrySet()) {
            if (e.getValue().size() < 2) continue;
            if (isGroupIsolated(e.getKey(), groups)) rewriteGroups.add(e.getValue());
        }
        if (rewriteGroups.isEmpty()) return;

        // 4) Rewrite independently per group
        for (List<TunnelInfo> grp : rewriteGroups) {
            try {
                replaceGroupWith(proj, circ, g, grp);
            } catch (Throwable t) {
                // No abortar proceso completo por un grupo
                t.printStackTrace();
            }
        }
    }

    /** Replaces the given group of tunnels with routed wires using GridRouter.
     * If routing any edge fails, no changes are applied.
     * @param proj Project (for actions).
     * @param circ Circuit to modify.
     * @param g Graphics context (for measuring components).
     * @param grp List of TunnelInfo in the same group.
     */
    private static void replaceGroupWith(Project proj,
                                         Circuit circ,
                                         Graphics g,
                                         List<TunnelInfo> grp) {
        if (grp == null || grp.size() < 2) return;

        final int MAX_GROUP_SIZE = 24;
        if (grp.size() > MAX_GROUP_SIZE) return;

        // ---------- 1) Prepare mouths (for routing) + stub metadata (for cleanup/reconnect) ----------

        final int n = grp.size();

        // The point we route between (BLT pin location).
        List<Location> mouths = new ArrayList<>(n);

        // The real circuit port anchor at the end of the stub (best effort).
        List<Location> anchors = new ArrayList<>(n);

        // Preferred elbow point for anchor->mouth connection (from original stub).
        List<Location> elbow = new ArrayList<>(n);

        // Stubs to remove if rewrite succeeds.
        List<Wire> stubsToRemove = new ArrayList<>(n * 2);

        // Facing inferred from BLT (used for launch pads).
        List<Direction> facings = new ArrayList<>(n);

        // Build wire adjacency once per group for stub detection.
        WireIndex widx = WireIndex.build(circ);

        for (TunnelInfo ti : grp) {
            Location m = ti.mouth(); // BLT mouth
            mouths.add(m);

            Stub stub = findStubRobust(widx, m);
            anchors.add(stub.anchor());
            elbow.add(stub.elbow());
            stubsToRemove.addAll(stub.wires());

            facings.add(readFacing(ti.comp()));
        }

        // Dedup stubs (multiple BLTs can accidentally share a segment in dense drawings)
        stubsToRemove = stubsToRemove.stream().distinct().toList();

        // ---------- 2) MST by Manhattan on mouths ----------
        List<int[]> edges = MstPlanner.buildMstEdges(mouths);
        if (edges.isEmpty()) return;

        // ---------- 3) Obstacles: components + existing wires ----------
        final int WIRE_MARGIN = 1;
        List<Bounds> obstacles = RouterUtils.collectComponentBounds(circ, g, grp);
        obstacles.addAll(RouterUtils.collectWireBounds(circ, WIRE_MARGIN));

        // Index for penalizing later routes
        Set<Long> reserved = new HashSet<>();

        GridRouter router = new GridRouter(
                GRID,
                /*soft*/3, /*hard*/5,
                /*costNear*/12, /*costReserved*/6,
                obstacles, reserved
        )
                .withMaxExpansions(40_000)
                .withMaxQueue(50_000)
                .withMaxMillis(1200);

        // ---------- 4) Plan all routes; abort group if any edge fails ----------
        List<Wire> planned = new ArrayList<>(edges.size() * 4);
        boolean ok = true;

        for (int[] e : edges) {
            int i = e[0], j = e[1];
            Location mi = mouths.get(i);
            Location mj = mouths.get(j);

            Location si = RouterUtils.launchPad(mi, facings.get(i), GRID, 1);
            Location tj = RouterUtils.launchPad(mj, facings.get(j), GRID, 1);

            // Fallback rápido: intenta HV y VH recto evitando OBSTÁCULOS (incluyen wires)
            List<Location> poly = RouterUtils.tryManhattanClear(si, tj, obstacles, /*clearHard*/5);
            if (poly == null) {
                // A* acotado con bbox alrededor de si–tj y obstáculos actuales (incl. wires)
                poly = router.route(si, tj);
            }
            if (poly == null || poly.size() < 2) { ok = false; break; }

            // Reservar la ruta para penalizar futuras y añadir obstáculos dinámicos
            poly = RouterUtils.simplifyPolyline(poly, obstacles, /*clearHard*/5);
            RouterUtils.markReservedPath(reserved, poly, GRID);
            obstacles.addAll(RouterUtils.polylineAsWireBounds(poly, WIRE_MARGIN));

            // También los “puentes” desde la boca hasta el pad
            obstacles.addAll(RouterUtils.segmentAsWireBounds(mi, si, WIRE_MARGIN));
            obstacles.addAll(RouterUtils.segmentAsWireBounds(tj, mj, WIRE_MARGIN));

            // Conectar: boca->pad, polyline, pad->boca
            planned.add(Wire.create(mi, si));
            for (int k = 0; k + 1 < poly.size(); k++) {
                planned.add(Wire.create(poly.get(k), poly.get(k + 1)));
            }
            planned.add(Wire.create(tj, mj));
        }

        if (ok) {
            // ---------- Apply rewrite mutation ----------
            CircuitMutation mut = new CircuitMutation(circ);

            // 1) add routed wires between mouths
            for (Wire w : planned) mut.add(w);

            // 2) remove old stubs created by TunnelPlacer
            for (Wire w : stubsToRemove) mut.remove(w);

            // 3) reconnect each real anchor to its mouth (2 orthogonal segments)
            for (int i = 0; i < n; i++) {
                Location a = anchors.get(i);
                Location m = mouths.get(i);
                if (a == null || m == null) continue;

                Location mid = elbow.get(i);
                if (mid == null) {
                    // Stable orthogonal elbow; choose the one that produces Manhattan L.
                    mid = Location.create(m.getX(), a.getY());
                    // If that degenerates to a straight line, alternative:
                    if (mid.equals(a) || mid.equals(m)) {
                        mid = Location.create(a.getX(), m.getY());
                    }
                }

                // Ensure we don't add zero-length wires
                if (!a.equals(mid)) mut.add(Wire.create(a, mid));
                if (!mid.equals(m)) mut.add(Wire.create(mid, m));
            }

            // 4) remove BLT components
            for (TunnelInfo ti : grp) mut.remove(ti.comp());

            if (!mut.isEmpty()) {
                proj.doAction(mut.toAction(Strings.getter("rewriteBitTunnelsAction")));
            }
            return;
        }

        // ---------- Fallback: convert BLTs to plain Tunnel ----------
        CircuitMutation mut = new CircuitMutation(circ);

        for (TunnelInfo ti : grp) {
            try {
                Component old = ti.comp();
                AttributeSet asOld = old.getAttributeSet();

                // WIDTH del BLT
                BitWidth bw = (asOld != null) ? asOld.getValue(StdAttr.WIDTH) : null;
                int width = Math.max(1, bw == null ? ti.tokensNorm().size() : bw.getWidth());

                // LABEL del BLT
                String label = (asOld != null) ? asOld.getValue(StdAttr.LABEL) : SpecBuilder.makePrettyLabel(ti.tokensNorm());

                // FACING del BLT
                Direction facing = readFacing(old);

                Tunnel tunnelF = Tunnel.FACTORY;

                // Atributos del Tunnel
                AttributeSet a = tunnelF.createAttributeSet();
                try { a.setValue(StdAttr.WIDTH, BitWidth.create(width)); } catch (Throwable ignore) {}
                try { a.setValue(StdAttr.FACING, facing); } catch (Throwable ignore) {}
                if (label != null && !label.isBlank()) {
                    try { a.setValue(StdAttr.LABEL, label); } catch (Throwable ignore) {}
                }

                // Place tunnel so its pin matches BLT mouth
                Location mouth = ti.mouth();
                Component probe = tunnelF.createComponent(Location.create(0, 0), a);
                EndData end0 = probe.getEnd(0);
                int offX = end0.getLocation().getX() - probe.getLocation().getX();
                int offY = end0.getLocation().getY() - probe.getLocation().getY();
                Location place = Location.create(mouth.getX() - offX, mouth.getY() - offY);

                // Encolar: quitar BLT y añadir Tunnel
                mut.remove(old);
                mut.add(tunnelF.createComponent(place, a));
            } catch (Throwable t) {
                // falla local: continuamos con el resto
                t.printStackTrace();
            }
        }

        if (!mut.isEmpty()) {
            proj.doAction(mut.toAction(Strings.getter("rewriteBitTunnelsAction")));
        }
    }

    // --------------------------------------------------------------------------------------------
    // Data model
    // --------------------------------------------------------------------------------------------

    public record TunnelInfo(Component comp, Location mouth, String labelNorm, List<String> tokensNorm) {}
    private record GroupKey(String labelNorm, List<String> tokensNorm) {}

    // --------------------------------------------------------------------------------------------
    // BLT collection & grouping
    // --------------------------------------------------------------------------------------------

    /** Recollects all BitLabeledTunnels in the circuit.
     * @param circ Circuit to scan.
     * @return List of TunnelInfo found.
     */
    private static List<TunnelInfo> collectBlt(Circuit circ) {
        List<TunnelInfo> out = new ArrayList<>();
        for (Component c : circ.getNonWires()) {
            if (!(c.getFactory() instanceof BitLabeledTunnel)) continue;

            AttributeSet as = c.getAttributeSet();

            String csv = safe(as, BitLabeledTunnel.BIT_SPECS);
            List<String> toks = parseSpecs(csv);

            List<String> norm = new ArrayList<>(toks.size());
            for (String t : toks) norm.add(normalizeToken(t));

            String label = safe(as, StdAttr.LABEL);
            String labelNorm = (label == null) ? "" : label.trim();

            EndData e = c.getEnd(0);
            if (e == null) continue;

            // NOTE: This is the BLT pin location (tunnelPinLoc).
            Location mouth = e.getLocation();

            out.add(new TunnelInfo(c, mouth, labelNorm, norm));
        }
        return out;
    }

    private static Map<GroupKey, List<TunnelInfo>> groupByLabelAndSpecs(List<TunnelInfo> all) {
        Map<GroupKey, List<TunnelInfo>> map = new LinkedHashMap<>();
        for (TunnelInfo ti : all) {
            GroupKey k = new GroupKey(ti.labelNorm(), ti.tokensNorm());
            map.computeIfAbsent(k, __ -> new ArrayList<>()).add(ti);
        }
        return map;
    }

    /**
     * A group is "isolated" if none of its relevant tokens (not 0/1/x) appear outside the group.
     */
    private static boolean isGroupIsolated(GroupKey k, Map<GroupKey, List<TunnelInfo>> groups) {
        Set<String> relevant = new HashSet<>();
        for (String t : k.tokensNorm) {
            String r = normalizeToRelevant(t);
            if (!r.isEmpty()) relevant.add(r);
        }
        if (relevant.isEmpty()) return true;

        for (Map.Entry<GroupKey, List<TunnelInfo>> e : groups.entrySet()) {
            if (e.getKey().equals(k)) continue;
            for (String t : e.getKey().tokensNorm) {
                String r = normalizeToRelevant(t);
                if (!r.isEmpty() && relevant.contains(r)) return false;
            }
        }
        return true;
    }

    // --------------------------------------------------------------------------------------------
    // Token helpers
    // --------------------------------------------------------------------------------------------

    private static String safe(AttributeSet as, Attribute<?> attr) {
        try {
            Object v = as.getValue(attr);
            return (v == null) ? "" : String.valueOf(v);
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static List<String> parseSpecs(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String t : csv.split(",")) out.add(t.trim());
        return out;
    }

    /** Normaliza el token del CSV:
     *  - "0","1","x"/"X" → tal cual en minúscula
     *  - "N123" → "N123" (en mayúscula la 'N')
     *  - cualquier otra cosa → trim, tal cual (puedes endurecer aquí si quieres)
     */
    private static String normalizeToken(String t) {
        if (t == null) return "";
        t = t.trim();
        if (t.isEmpty()) return "";
        if ("0".equals(t) || "1".equals(t)) return t;
        if ("x".equalsIgnoreCase(t)) return "x";
        if (t.length() >= 2 && (t.charAt(0) == 'N' || t.charAt(0) == 'n')) {
            try {
                int id = Integer.parseInt(t.substring(1).trim());
                return "N" + id;
            } catch (NumberFormatException ignore) { }
        }
        return t;
    }

    private static String normalizeToRelevant(String t) {
        t = normalizeToken(t);
        if (t.isEmpty() || "0".equals(t) || "1".equals(t) || "x".equals(t)) return "";
        return t;
    }

    // --------------------------------------------------------------------------------------------
    // Facing & stub detection
    // --------------------------------------------------------------------------------------------

    private static Direction readFacing(Component c) {
        Direction f = Direction.EAST;
        try {
            AttributeSet as = c.getAttributeSet();
            Direction v = (as != null) ? as.getValue(StdAttr.FACING) : null;
            if (v != null) f = v;
        } catch (Throwable ignore) {}
        return f;
    }

    /** Represents the stub wires connecting a BLT mouth to a real anchor (port). */
    private record Stub(Location anchor, List<Wire> wires, Location elbow) {}

    /**
     * Robustly detects the typical TunnelPlacer stub:
     *   anchor --(w2)-- elbow --(w1)-- bltMouth
     * <p>
     * We choose the best candidate among all incident wires at bltMouth by:
     * - preferring 2-segment paths
     * - preferring an elbow that forms an orthogonal L
     * - preferring anchors with higher endpoint degree (often a real port junction)
     */
    private static Stub findStubRobust(WireIndex widx, Location bltMouth) {
        if (widx == null || bltMouth == null) return new Stub(bltMouth, List.of(), null);

        List<Wire> inc = widx.byEnd.getOrDefault(bltMouth, List.of());
        if (inc.isEmpty()) return new Stub(bltMouth, List.of(), null);

        Stub best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Wire w1 : inc) {
            Location p1 = otherEnd(w1, bltMouth);

            // Candidate path length 1
            Stub cand1 = new Stub(p1, List.of(w1), null);
            int score1 = scoreStub(widx, bltMouth, cand1);
            if (score1 > bestScore) { best = cand1; bestScore = score1; }

            // Try extend to length 2
            Wire w2 = widx.nextWire(p1, w1);
            if (w2 == null) continue;

            Location p2 = otherEnd(w2, p1);
            Stub cand2 = new Stub(p2, List.of(w1, w2), p1);
            int score2 = scoreStub(widx, bltMouth, cand2);
            if (score2 > bestScore) { best = cand2; bestScore = score2; }
        }

        return (best != null) ? best : new Stub(bltMouth, List.of(), null);
    }

    private static int scoreStub(WireIndex widx, Location mouth, Stub s) {
        int score = 0;

        // Prefer 2-wire stub (the normal TunnelPlacer case)
        score += (s.wires().size() == 2) ? 100 : 0;

        // Prefer orthogonal elbow
        if (s.elbow() != null) {
            Location a = s.anchor();
            Location e = s.elbow();
            Location m = mouth;

            boolean seg1 = (a.getX() == e.getX()) || (a.getY() == e.getY());
            boolean seg2 = (e.getX() == m.getX()) || (e.getY() == m.getY());
            boolean orth = (seg1 && seg2) && !((a.getX() == m.getX()) || (a.getY() == m.getY()));
            score += orth ? 40 : 0;
        }

        // Prefer anchors that look like real junctions (degree >= 2)
        score += Math.min(20, widx.degree(s.anchor()) * 5);

        return score;
    }

    private static Location otherEnd(Wire w, Location x) {
        return x.equals(w.getEnd0()) ? w.getEnd1() : w.getEnd0();
    }

    /** Local wire index for fast degree / adjacency queries. */
    private static final class WireIndex {
        final Map<Location, List<Wire>> byEnd = new HashMap<>();

        static WireIndex build(Circuit circ) {
            WireIndex idx = new WireIndex();
            for (Wire w : iterWires(circ)) {
                idx.byEnd.computeIfAbsent(w.getEnd0(), __ -> new ArrayList<>()).add(w);
                idx.byEnd.computeIfAbsent(w.getEnd1(), __ -> new ArrayList<>()).add(w);
            }
            return idx;
        }

        int degree(Location p) {
            return byEnd.getOrDefault(p, List.of()).size();
        }

        Wire nextWire(Location at, Wire prev) {
            for (Wire w : byEnd.getOrDefault(at, List.of())) {
                if (w != prev) return w;
            }
            return null;
        }
    }

    private static Iterable<Wire> iterWires(Circuit circ) {
        return circ.getWires();
    }
}
