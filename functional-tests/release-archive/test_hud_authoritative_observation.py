"""Execute the real HUD observer against deterministic asynchronous queues.

No game inputs or shipping classes are replaced. The isolated fixture extracts
the production QA method unchanged and supplies only its clock/server boundary.
These are helper checks, not gameplay acceptance.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
HARNESS = ROOT / 'functional-tests/src/main/java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java'

FIXTURE = r'''
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public class HudObserverFixture {
    static class System { static long time; static long nanoTime() { return time; } }
    record Step(String name, Runnable start, BooleanSupplier ready, Runnable verify, int settleTicks) {}
    static Step step;
    static final Queue<Runnable> queue = new ArrayDeque<>();
    static int selected = 7, reads, verdicts;
    static boolean result, failRead;
    static String info;
    static final class Player { Object getInventory() { return this; } }
    static Player player() { return new Player(); }
    static int hudSelected(Object inventory) {
        reads++;
        if (failRead) throw new IllegalStateException("authoritative query failed");
        return selected;
    }
    static <T> CompletableFuture<T> server(Supplier<T> query) {
        CompletableFuture<T> future = new CompletableFuture<>();
        queue.add(() -> { try { future.complete(query.get()); } catch (Throwable t) { future.completeExceptionally(t); } });
        return future;
    }
    static void addFirst(String name, Runnable start, BooleanSupplier ready, Runnable verify, int settleTicks) {
        step = new Step(name, start, ready, verify, settleTicks);
    }
    static void record(String name, String status, String text) {
        require(name.equals("hud-server-option-off-selection-observation") && status.equals("INFO"), "observation label");
        info = text;
    }
    static void check(String name, boolean value) {
        require(name.equals("hud-server-option-off-selection"), "original assertion name");
        verdicts++; result = value;
    }
    static void require(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    static void at(long millis) { System.time = millis * 1_000_000L; }
    static void deliver() { require(queue.size() == 1, "one outstanding observation"); queue.remove().run(); }
    static boolean ready() { return step.ready().getAsBoolean(); }
    static void verify(boolean expected) {
        step.verify().run(); require(verdicts == 1 && result == expected, "original predicate verdict");
        require(info.contains("expected=8"), "unchanged expected selection");
    }
    // METHOD
    public static void main(String[] args) {
        hudServerSelected("option-off-selection", 8);
        require(step.name().equals("hud-server-option-off-selection") && step.settleTicks() == 3, "original stage and settling");
        step.start().run();
        switch (args[0]) {
            case "immediate" -> {
                selected = 8; deliver(); require(ready(), "matching first sample"); verify(true);
                require(reads == 1 && queue.isEmpty(), "no extra query after match");
            }
            case "early-snapshot" -> {
                deliver(); require(!ready(), "stale first sample must not pass");
                at(49); require(!ready() && queue.isEmpty(), "bounded sampling rate");
                at(50); require(!ready(), "new query pending");
                selected = 8; deliver(); at(51); require(ready(), "native delivery observed"); verify(true);
                require(reads == 2 && selected == 8, "only reads, no selection override");
            }
            case "no-delivery" -> {
                deliver();
                for (int ms = 0; ms < 1000; ms += 50) {
                    at(ms); require(!ready(), "incorrect value cannot pass");
                    if (!queue.isEmpty()) deliver();
                }
                at(1000); require(ready(), "finite deadline"); verify(false);
                require(selected == 7 && reads <= 20 && info.contains("expired=true"), "failed delivery retained");
            }
            case "unresponsive-server" -> {
                at(999); require(!ready() && queue.size() == 1, "pending observation");
                at(1000); require(ready(), "pending future cannot hang the stage"); verify(false);
                require(reads == 0, "no invented server reading");
            }
            case "late-match" -> {
                selected = 8; deliver(); at(1000); require(ready(), "deadline reached"); verify(false);
            }
            case "single-flight" -> {
                for (int ms = 0; ms < 999; ms++) { at(ms); require(!ready(), "unanswered query"); }
                require(queue.size() == 1 && reads == 0, "no observation queue flood");
            }
            case "read-error" -> {
                failRead = true; deliver();
                try { ready(); throw new AssertionError("failed query was swallowed"); }
                catch (CompletionException error) {
                    require(error.getCause() instanceof IllegalStateException && verdicts == 0, "failed query propagates");
                }
            }
            case "latched-success" -> {
                selected = 8; deliver(); require(ready(), "matching observation"); at(1500);
                require(ready(), "settle ticks retain completed result"); verify(true);
                require(queue.isEmpty(), "no subsequent query");
            }
            default -> throw new AssertionError("Unknown scenario");
        }
    }
}
'''


class HudAuthoritativeObservation(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix='siu-hud-observer-')
        cls.folder = Path(cls.temp.name)
        text = HARNESS.read_text(encoding='utf-8')
        begin = text.index('    private static void hudServerSelected(String label, int expected) {')
        end = text.index('    private static void hudPointerOutside()', begin)
        cls.method = text[begin:end]
        source = cls.folder / 'HudObserverFixture.java'
        source.write_text(FIXTURE.replace('// METHOD', cls.method), encoding='utf-8')
        cls.java = shutil.which('java')
        javac = shutil.which('javac')
        if not cls.java or not javac:
            raise AssertionError('A JDK is required; do not skip these observer checks')
        result = subprocess.run([javac, '-d', str(cls.folder), str(source)], capture_output=True, text=True)
        if result.returncode:
            raise AssertionError(result.stdout + result.stderr)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def trial(self, name):
        result = subprocess.run([self.java, '-cp', str(self.folder), 'HudObserverFixture', name], capture_output=True, text=True, timeout=15)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_immediate_authoritative_delivery(self): self.trial('immediate')
    def test_early_snapshot_is_refreshed_without_input_retry(self): self.trial('early-snapshot')
    def test_permanent_non_delivery_fails(self): self.trial('no-delivery')
    def test_unresponsive_server_fails_within_deadline(self): self.trial('unresponsive-server')
    def test_deadline_does_not_accept_late_matching_sample(self): self.trial('late-match')
    def test_at_most_one_query_is_in_flight(self): self.trial('single-flight')
    def test_authoritative_read_exception_is_not_swallowed(self): self.trial('read-error')
    def test_success_survives_original_settle_ticks(self): self.trial('latched-success')


if __name__ == '__main__':
    unittest.main()
