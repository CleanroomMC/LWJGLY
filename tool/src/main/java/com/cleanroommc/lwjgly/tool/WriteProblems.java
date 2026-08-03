package com.cleanroommc.lwjgly.tool;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class WriteProblems {

    private WriteProblems() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 6) {
            throw new IllegalArgumentException("usage: WriteProblems <lwjgl2-cp> <lwjgl3-cp> "
                    + "<shim-classes-dirs> <adapter-classes-dir> <adapter-map> <build/lwjgly/PROBLEMS.md>");
        }
        ApiIndex lwjgl2 = ApiIndex.read(args[0]);
        ApiIndex lwjgl3 = ApiIndex.read(args[1]);
        Built built = Built.read(args[2], args[3], Paths.get(args[4]));
        Path problems = Paths.get(args[5]);

        Map<String, Delta.ClassDelta> deltas = ApiDiff.diff(lwjgl2, lwjgl3);
        ProblemsDoc.write(problems, deltas, lwjgl2, lwjgl3, built);

        System.out.println("wrote " + problems);
    }
}
