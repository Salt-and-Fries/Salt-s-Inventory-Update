import com.salts_inventory_update.compat.ae2.qa.NativeAtlasCoverage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Executes the shipped QA coverage gate against reviewed and deliberately broken captures. */
public final class VerifyAe2AtlasCoverage {
    public static void main(String[] args) throws Exception {
        Path atlas = Path.of(args[0]);
        String minecraft = args[1];
        Path scratch = Path.of(args[2]).toAbsolutePath();
        Files.createDirectories(scratch);
        var rows = Files.readAllLines(atlas.resolve("manifest.csv"));
        Files.write(scratch.resolve("manifest.csv"), rows);
        for (String row : rows.subList(1, rows.size())) {
            String file = row.split(",")[9];
            Files.writeString(scratch.resolve(file), "coverage fixture");
            Files.writeString(scratch.resolve(file.replace(".png", ".geometry.json")), "{}");
        }
        NativeAtlasCoverage.verify(scratch, minecraft);
        var duplicate = new java.util.ArrayList<>(rows); duplicate.add(rows.get(1));
        Files.write(scratch.resolve("manifest.csv"), duplicate);
        rejects(() -> NativeAtlasCoverage.verify(scratch, minecraft), "duplicate");
        Files.write(scratch.resolve("manifest.csv"), rows.subList(0, rows.size() - 1));
        rejects(() -> NativeAtlasCoverage.verify(scratch, minecraft), "missing supported presentation");
        Files.write(scratch.resolve("manifest.csv"), rows);
        rejects(() -> NativeAtlasCoverage.verify(scratch, minecraft.equals("1.20.1") ? "1.21.1" : "1.20.1"),
                "wrong upstream feature profile");
        String first = rows.get(1).split(",")[9];
        Files.delete(scratch.resolve(first.replace(".png", ".geometry.json")));
        rejects(() -> NativeAtlasCoverage.verify(scratch, minecraft), "missing geometry");
        System.out.println("Native atlas coverage: 5 checks passed for " + minecraft);
    }
    interface Check { void run() throws Exception; }
    private static void rejects(Check check, String name) throws Exception {
        try { check.run(); }
        catch (IllegalStateException expected) { return; }
        throw new AssertionError("Coverage gate accepted " + name);
    }
}
