package com.cleanroommc.lwjgly.tool;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public final class Resolution {

    public static boolean hasMethod(ApiIndex index, String owner, String name, String desc) {
        return findMethod(index, owner, name, desc) != null;
    }

    public static MethodNode findMethod(ApiIndex index, String owner, String name, String desc) {
        ClassNode node = index.get(owner);
        while (node != null) {
            for (MethodNode method : node.methods) {
                if (method.name.equals(name) && method.desc.equals(desc)) {
                    return method;
                }
            }
            for (String itf : node.interfaces) {
                MethodNode found = findMethod(index, itf, name, desc);
                if (found != null) {
                    return found;
                }
            }
            node = node.superName == null ? null : index.get(node.superName);
        }
        return null;
    }

    public static FieldNode findField(ApiIndex index, String owner, String name, String desc) {
        ClassNode node = index.get(owner);
        while (node != null) {
            for (FieldNode field : node.fields) {
                if (field.name.equals(name) && field.desc.equals(desc)) {
                    return field;
                }
            }
            for (String itf : node.interfaces) {
                FieldNode found = findField(index, itf, name, desc);
                if (found != null) {
                    return found;
                }
            }
            node = node.superName == null ? null : index.get(node.superName);
        }
        return null;
    }

    public static List<String> hierarchy(ApiIndex index, String owner) {
        List<String> result = new ArrayList<>();
        ClassNode node = index.get(owner);
        while (node != null) {
            result.add(node.name);
            node = node.superName == null ? null : index.get(node.superName);
        }
        return result;
    }

    private Resolution() { }
}
