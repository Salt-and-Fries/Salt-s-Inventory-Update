package com.salts_inventory_update.compat.ae2.qa;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.PlayerSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

/** Shared opt-in live-job test: no client synchronization fields are assigned by this fixture. */
final class NativeCpuLifecycleQa {
    private record Job(ICraftingService service, CraftingCPUCluster cpu, IActionSource source,
            Future<ICraftingPlan> plan) { }
    private static int stage;
    private static int completedCycles;
    private static long started;
    private static CompletableFuture<Job> preparation;
    private static CompletableFuture<Void> submission;
    private static CompletableFuture<Boolean> cancellation;
    private static Job job;

    private static <T> CompletableFuture<T> server(Minecraft mc, Supplier<T> action) {
        var result = new CompletableFuture<T>();
        mc.getSingleplayerServer().execute(() -> {
            try { result.complete(action.get()); }
            catch (Throwable failure) { result.completeExceptionally(failure); }
        });
        return result;
    }

    /** Returns false while waiting; throws on a real job, action or synchronization failure. */
    static boolean verify(Minecraft mc, CraftingCPUScreen<?> screen, BlockPos origin,
            Path output, BiConsumer<Double, Double> click, Consumer<CraftingCPUCluster> hold,
            Runnable release) throws Exception {
        if (stage == 6) return true;
        if (started == 0) started = System.currentTimeMillis();
        if (System.currentTimeMillis() - started > 120_000)
            throw new IllegalStateException("Live crafting CPU lifecycle timed out at stage " + stage);
        var menu = screen.getMenu();
        if (stage == 0) {
            if (!menu.cpuStatusReady || !menu.cpuAvailable || menu.hasActiveJob)
                throw new IllegalStateException("Live CPU fixture requires synchronized available idle CPU");
            preparation = server(mc, () -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                var block = (CraftingBlockEntity) player.level().getBlockEntity(origin.offset(4, 0, 1));
                var cpu = block.getCluster();
                var node = block.getMainNode().getNode();
                var service = block.getMainNode().getGrid().getCraftingService();
                var source = new PlayerSource(player, block);
                var requester = new ICraftingSimulationRequester() {
                    @Override public IActionSource getActionSource() { return source; }
                    @Override public appeng.api.networking.IGridNode getGridNode() { return node; }
                };
                var plan = service.beginCraftingCalculation(player.level(), requester,
                        AEItemKey.of(Items.ACACIA_PLANKS), 256, CalculationStrategy.REPORT_MISSING_ITEMS);
                return new Job(service, cpu, source, plan);
            });
            stage = 1;
        } else if (stage == 1) {
            if (!preparation.isDone()) return false;
            job = preparation.join();
            if (!job.plan().isDone()) return false;
            var plan = job.plan().get();
            if (plan.simulation() || plan.patternTimes().isEmpty())
                throw new IllegalStateException("Live CPU fixture requires a complete native crafting plan: missing="
                        + plan.missingItems() + ", patterns=" + plan.patternTimes().size());
            submission = server(mc, () -> {
                var result = job.service().submitJob(plan, null, job.cpu(), true, job.source());
                if (!result.successful())
                    throw new IllegalStateException("Native crafting submission failed: " + result.errorCode());
                // Hold a genuine submitted job so completion speed cannot race the client assertion.
                hold.accept(job.cpu());
                if (job.cpu().getJobStatus() == null)
                    throw new IllegalStateException("Native submitted CPU has no active job");
                return null;
            });
            stage = 2;
        } else if (stage == 2) {
            if (!submission.isDone()) return false;
            submission.join();
            if (!menu.cpuStatusReady || !menu.cpuAvailable || !menu.hasActiveJob) return false;
            var cancel = screen.children().stream().filter(child -> child instanceof AbstractWidget)
                    .map(child -> (AbstractWidget) child)
                    .filter(widget -> widget.visible && widget.active && widget.getMessage().getString().equals("Cancel"))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Active CPU has no enabled Cancel button"));
            click.accept(cancel.getX() + cancel.getWidth() / 2.0, cancel.getY() + cancel.getHeight() / 2.0);
            stage = 3;
        } else if (stage == 3) {
            if (!menu.cpuStatusReady || !menu.cpuAvailable || menu.hasActiveJob) return false;
            cancellation = server(mc, () -> {
                boolean cancelled = job.cpu().getJobStatus() == null;
                release.run();
                return cancelled;
            });
            stage = 4;
        } else if (stage == 4) {
            if (!cancellation.isDone()) return false;
            if (!cancellation.join()) throw new IllegalStateException("Client Cancel did not cancel the server job");
            completedCycles++;
            org.slf4j.LoggerFactory.getLogger(NativeCpuLifecycleQa.class)
                    .info("CPU LIFECYCLE VERIFIED cycle={} output=256 acacia planks", completedCycles);
            if (completedCycles < 2) {
                stage = 0;
                return false;
            }
            Files.writeString(output.resolve("cpu-lifecycle.json"),
                    "{\"nativeJobSubmitted\":true,\"clientActiveStateReceived\":true,"
                    + "\"cancelClickedThroughDesktop\":true,\"clientIdleStateReceived\":true,"
                    + "\"serverCancellationVerified\":true,\"clientStatusFieldsAssignedByFixture\":false,"
                    + "\"nativeJobsSubmitted\":2,\"serverCancellationsVerified\":2}\n");
            stage = 6;
            return true;
        }
        return false;
    }
}
