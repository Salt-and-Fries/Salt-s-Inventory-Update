import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.ToolProvider;

/** Parses member boundaries for reviewing AE2 additions without copying native classes. */
public final class JavaMembers {
    private static String json(String value) {
        var out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    public static void main(String[] args) throws Exception {
        var path = Path.of(args[0]);
        String source = Files.readString(path);
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            var task = (JavacTask) compiler.getTask(null, manager, null,
                    List.of("-proc:none"), null, manager.getJavaFileObjects(path));
            var positions = Trees.instance(task).getSourcePositions();
            var units = new ArrayList<String>();
            for (CompilationUnitTree unit : task.parse()) {
                var members = new ArrayList<String>();
                for (Tree definition : unit.getTypeDecls()) {
                    if (!(definition instanceof ClassTree type)) continue;
                    for (Tree member : type.getMembers()) {
                        String key;
                        if (member instanceof MethodTree method) {
                            key = method.getName() + "(" + String.join(",", method.getParameters().stream()
                                    .map(parameter -> parameter.getType().toString()).toList()) + ")";
                        } else if (member instanceof VariableTree field) {
                            key = "field:" + field.getName();
                        } else if (member instanceof ClassTree nested) {
                            key = "type:" + nested.getSimpleName();
                        } else {
                            key = "block:" + members.size();
                        }
                        int start = (int) positions.getStartPosition(unit, member);
                        int end = (int) positions.getEndPosition(unit, member);
                        members.add("{\"key\":" + json(key) + ",\"kind\":" + json(member.getKind().name())
                                + ",\"start\":" + start + ",\"end\":" + end + ",\"source\":"
                                + json(source.substring(start, end)) + "}");
                    }
                }
                units.add("{\"package\":" + json(unit.getPackageName().toString())
                        + ",\"imports\":" + json(String.join("\n", unit.getImports().stream().map(Object::toString).toList()))
                        + ",\"members\":[" + String.join(",", members) + "]}");
            }
            System.out.println("[" + String.join(",", units) + "]");
        }
    }
}
