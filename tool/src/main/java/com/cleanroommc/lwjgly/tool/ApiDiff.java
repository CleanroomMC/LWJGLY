package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class ApiDiff {

    private static final String LWJGL_PREFIX = "org/lwjgl/";

    private final ApiIndex lwjgl2;
    private final ApiIndex lwjgl3;
    private final PointerHandles handles;

    private ApiDiff(ApiIndex lwjgl2, ApiIndex lwjgl3) {
        this.lwjgl2 = lwjgl2;
        this.lwjgl3 = lwjgl3;
        this.handles = PointerHandles.in(lwjgl2);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: ApiDiff <lwjgl2-cp> <lwjgl3-cp> <deltas.json> <delta-report.txt>");
        }
        ApiIndex lwjgl2 = ApiIndex.read(args[0]);
        ApiIndex lwjgl3 = ApiIndex.read(args[1]);
        Map<String, Delta.ClassDelta> deltas = diff(lwjgl2, lwjgl3);

        Path json = Paths.get(args[2]);
        Path report = Paths.get(args[3]);
        Files.createDirectories(json.getParent());

        DeltaJson.write(json, deltas, args[0], args[1]);
        DeltaReport.write(report, deltas);

        System.out.println(DeltaReport.summary(deltas));
        System.out.println("wrote " + json + ", " + report);
    }

    public static Map<String, Delta.ClassDelta> diff(ApiIndex lwjgl2, ApiIndex lwjgl3) {
        return new ApiDiff(lwjgl2, lwjgl3).run();
    }

    private static List<String> rawEntryPoints(String name, String desc) {
        List<String> raw = new ArrayList<>();
        raw.add("n" + name);
        for (String renamed : Naming.candidateNames(name, desc)) {
            raw.add("n" + renamed);
        }
        return raw;
    }

    private Map<String, Delta.ClassDelta> run() {
        Map<String, Delta.ClassDelta> deltas = new TreeMap<>();
        for (ClassNode node : lwjgl2.classes().values()) {
            if (!node.name.startsWith(LWJGL_PREFIX)) {
                continue;
            }
            deltas.put(node.name, classify(node));
        }
        markVendorable(deltas);
        return deltas;
    }

    private Delta.ClassDelta classify(ClassNode node) {
        boolean sharedName = lwjgl3.has(node.name);
        List<Delta.MethodDelta> methods = new ArrayList<>();
        List<Delta.FieldDelta> fields = new ArrayList<>();

        if (sharedName) {
            for (MethodNode method : node.methods) {
                if (!ApiIndex.isCallable(method.access) || method.name.equals("<clinit>")) {
                    continue;
                }
                if (Resolution.hasMethod(lwjgl3, node.name, method.name, method.desc)) {
                    continue;
                }
                methods.add(methodDelta(node.name, method));
            }
            for (FieldNode field : node.fields) {
                if (!ApiIndex.isCallable(field.access)) {
                    continue;
                }
                if (Resolution.findField(lwjgl3, node.name, field.name, field.desc) != null) {
                    continue;
                }
                fields.add(new Delta.FieldDelta(field.name, field.desc, ApiIndex.isInlinedConstant(field)));
            }
        }

        boolean needsInjection = !methods.isEmpty()
                || fields.stream().anyMatch(f -> !f.inlinedConstant());

        Delta.Bucket bucket = !sharedName ? Delta.Bucket.SHIM
                : needsInjection ? Delta.Bucket.INJECT
                  : Delta.Bucket.IDENTICAL;

        Delta.ShimSource source = bucket == Delta.Bucket.SHIM ? Delta.ShimSource.VENDOR : null;
        return new Delta.ClassDelta(node.name, bucket, source, new ArrayList<>(), methods, fields);
    }

    private Delta.MethodDelta methodDelta(String owner, MethodNode method) {
        List<String> candidates = descriptorsNamed(owner, method.name);

        if (method.name.equals("<init>")) {
            return new Delta.MethodDelta(method.name, method.desc, candidates.isEmpty() ? Delta.Fix.NO_TARGET : Delta.Fix.MANUAL, null, candidates);
        }

        for (String candidate : candidates) {
            List<Delta.Argument> plan = CallPlan.of(method.desc, candidate, handles, false, nativeTypes(owner, method.name, candidate));
            if (plan != null) {
                return new Delta.MethodDelta(method.name, method.desc, Delta.Fix.AUTO, new Delta.Target(method.name, candidate, plan), candidates);
            }
        }

        for (String renamed : Naming.candidateNames(method.name, method.desc)) {
            for (String desc : descriptorsNamed(owner, renamed)) {
                if (Convertibility.signatureConvertibleAsHandles(method.desc, desc, handles)) {
                    return new Delta.MethodDelta(method.name, method.desc, Delta.Fix.AUTO_RENAMED,
                            new Delta.Target(renamed, desc, CallPlan.of(method.desc, desc, handles, false,
                                    nativeTypes(owner, renamed, desc))), candidates);
                }
            }
        }

        for (List<String> names : List.of(Naming.candidateNames(method.name, method.desc), List.of(method.name), rawEntryPoints(method.name, method.desc))) {
            Delta.MethodDelta lossy = lossyDelta(owner, method, candidates, names);
            if (lossy != null) {
                return lossy;
            }
        }

        if (candidates.isEmpty()) {
            List<String> renamedCandidates = new ArrayList<>();
            for (String renamed : Naming.candidateNames(method.name, method.desc)) {
                for (String desc : descriptorsNamed(owner, renamed)) {
                    renamedCandidates.add(renamed + desc);
                }
            }
            return renamedCandidates.isEmpty()
                    ? new Delta.MethodDelta(method.name, method.desc, Delta.Fix.NO_TARGET, null, List.of())
                    : new Delta.MethodDelta(method.name, method.desc, Delta.Fix.MANUAL, null, renamedCandidates);
        }
        return new Delta.MethodDelta(method.name, method.desc, Delta.Fix.MANUAL, null, candidates);
    }

    private NativeTypes nativeTypes(String owner, String name, String desc) {
        int count = Type.getMethodType(desc).getArgumentTypes().length;
        MethodNode target = Resolution.findMethod(lwjgl3, owner, name, desc);
        return NativeTypes.of(target, count);
    }

    private Delta.MethodDelta lossyDelta(String owner, MethodNode method, List<String> candidates,
                                         List<String> names) {
        for (String name : names) {
            for (String desc : descriptorsNamed(owner, name)) {
                List<Delta.Argument> plan =
                        CallPlan.of(method.desc, desc, handles, true, nativeTypes(owner, name, desc));
                if (plan != null) {
                    return new Delta.MethodDelta(method.name, method.desc, Delta.Fix.AUTO_RENAMED,
                            new Delta.Target(name, desc, plan), candidates);
                }
            }
        }
        return null;
    }

    private List<String> descriptorsNamed(String owner, String name) {
        List<String> descs = new ArrayList<>();
        for (String type : Resolution.hierarchy(lwjgl3, owner)) {
            for (MethodNode candidate : lwjgl3.get(type).methods) {
                if (candidate.name.equals(name) && ApiIndex.isCallable(candidate.access) && !descs.contains(candidate.desc)) {
                    descs.add(candidate.desc);
                }
            }
        }
        return descs;
    }

    private void markVendorable(Map<String, Delta.ClassDelta> deltas) {
        Set<String> vendorable = new LinkedHashSet<>();
        for (Map.Entry<String, Delta.ClassDelta> entry : deltas.entrySet()) {
            if (entry.getValue().bucket() == Delta.Bucket.SHIM) {
                vendorable.add(entry.getKey());
            }
        }

        Map<String, List<String>> rejections = new LinkedHashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String name : new ArrayList<>(vendorable)) {
                List<String> reasons = rejectionReasons(lwjgl2.get(name), vendorable, deltas);
                if (!reasons.isEmpty()) {
                    vendorable.remove(name);
                    rejections.put(name, reasons);
                    changed = true;
                }
            }
        }

        for (Map.Entry<String, Delta.ClassDelta> entry : deltas.entrySet()) {
            Delta.ClassDelta delta = entry.getValue();
            if (delta.bucket() != Delta.Bucket.SHIM) {
                continue;
            }
            boolean ok = vendorable.contains(entry.getKey());
            entry.setValue(new Delta.ClassDelta(
                    delta.internalName(),
                    delta.bucket(),
                    ok ? Delta.ShimSource.VENDOR : Delta.ShimSource.REWRITE,
                    ok ? List.of() : rejections.getOrDefault(entry.getKey(), List.of("depends on a rewritten class")),
                    delta.methods(),
                    delta.fields())
            );
        }
    }

    private List<String> rejectionReasons(ClassNode node, Set<String> vendorable, Map<String, Delta.ClassDelta> deltas) {
        List<String> reasons = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_NATIVE) != 0) {
                reasons.add("declares native method " + method.name + method.desc);
            }
        }
        Set<String> unresolved = new LinkedHashSet<>();
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.startsWith(LWJGL_PREFIX)) {
                    if (unavailable(call.owner, call.name, call.desc, false, vendorable, node.name, deltas)) {
                        unresolved.add(call.owner + "." + call.name + call.desc);
                    }
                } else if (insn instanceof FieldInsnNode access && access.owner.startsWith(LWJGL_PREFIX)) {
                    if (unavailable(access.owner, access.name, access.desc, true, vendorable, node.name, deltas)) {
                        unresolved.add(access.owner + "." + access.name + " : " + access.desc);
                    }
                }
            }
        }

        if (node.superName != null && node.superName.startsWith(LWJGL_PREFIX)
                && !lwjgl3.has(node.superName) && !vendorable.contains(node.superName)) {
            reasons.add("extends " + node.superName + ", which is not vendorable");
        }
        unresolved.forEach(u -> reasons.add("references " + u + ", gone from LWJGL 3"));
        return reasons;
    }

    private boolean unavailable(String owner, String name, String desc, boolean field, Set<String> vendorable,
                                String self, Map<String, Delta.ClassDelta> deltas) {
        if (owner.equals(self)) {
            return true;
        }
        if (field ? Resolution.findField(lwjgl3, owner, name, desc) != null
                : Resolution.hasMethod(lwjgl3, owner, name, desc)) {
            return true;
        }

        if (vendorable.contains(owner)) {
            return true;
        }

        if (!field) {
            Delta.ClassDelta delta = deltas.get(owner);
            if (delta != null && delta.bucket() == Delta.Bucket.INJECT) {
                return delta.methods().stream()
                        .anyMatch(m -> m.name().equals(name) && m.desc().equals(desc) && m.fix().isImplementable());
            }
        }
        return false;
    }
}
