"""Add real native control checks to a separate copy of the frozen CPU fixture.

The original shared Java and complete-suite inputs are never edited. Every
original cancellation/lifecycle statement is preserved; reproduction normalizes
only CRLF line endings, as the text reader used by source generation does.
The only new actions are ordinary desktop pointer clicks on native widgets;
server futures poll native state without retrying input or assigning status.
"""
from pathlib import Path

BASE='functional-tests/ae2/shared/src/main/java/com/salts_inventory_update/compat/ae2/qa/NativeCpuLifecycleQa.java'
TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge']
STATE_ANCHOR='    private static Job job;'
ACTION_ANCHOR='''            if (!menu.cpuStatusReady || !menu.cpuAvailable || !menu.hasActiveJob) return false;
            var cancel = screen.children().stream().filter(child -> child instanceof AbstractWidget)'''
RELEASE_ANCHOR='                release.run();'

FIELDS='''
    private static int controlsCycle = -1;
    private static int controlsStage;
    private static int controlsModeSteps;
    private static String controlsInitialMode;
    private static String controlsPreviousMode;
    private static final java.util.Set<String> controlsModes = new java.util.HashSet<>();
    private static final java.util.List<java.util.Map<String, Object>> controlsStates = new java.util.ArrayList<>();
    private static final java.util.List<java.util.Map<String, Object>> controlsCycles = new java.util.ArrayList<>();
    private static CompletableFuture<java.util.Map<String, Object>> controlsReading;
    private static net.minecraft.world.level.block.state.BlockState controlsAssemblerState;
'''

HELPERS=r'''
    private static Object controlsField(Object object, String name) {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try { var field = type.getDeclaredField(name); field.setAccessible(true); return field.get(object); }
            catch (NoSuchFieldException next) { }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
        return null;
    }
    private static AbstractWidget controlsWidget(Object object, String name) {
        Object value = controlsField(object, name);
        if (!(value instanceof AbstractWidget widget) || !widget.visible || !widget.active)
            throw new IllegalStateException("Native CPU widget is unavailable: " + name);
        return widget;
    }
    private static boolean controlsClientSuspended(CraftingCPUScreen<?> screen) {
        CLIENT_SUSPENDED
    }
    private static CompletableFuture<java.util.Map<String, Object>> readControls(Minecraft mc) {
        return server(mc, () -> nativeControls());
    }
    private static java.util.Map<String, Object> nativeControls() {
        return java.util.Map.of(
                "mode", job.cpu().getConfigManager().getSetting(appeng.api.config.Settings.CPU_SELECTION_MODE).name(),
                "active", job.cpu().getJobStatus() != null,
                "suspended", SERVER_SUSPENDED);
    }
    private static CompletableFuture<java.util.Map<String, Object>> prepareControlsHold(Minecraft mc, BlockPos origin) {
        return server(mc, () -> {
            if (HAS_SUSPEND) {
                // The original modern fixture suspends the real job immediately
                // after submission. Keep it held while removing only the empty
                // assembler, then use the native Resume button below. This is
                // disposable world setup, never an assigned client status.
                var level = mc.getSingleplayerServer().overworld();
                var pos = origin.offset(3, 1, 1);
                var state = level.getBlockState(pos);
                if (controlsAssemblerState != null
                        || state.getBlock() != appeng.core.definitions.AEBlocks.MOLECULAR_ASSEMBLER.block())
                    throw new IllegalStateException("Native CPU assembler prerequisite changed");
                controlsAssemblerState = state;
                if (!level.removeBlock(pos, false))
                    throw new IllegalStateException("Native CPU assembler was not held");
            }
            return nativeControls();
        });
    }
    private static void releaseControlsHold(Minecraft mc, BlockPos origin) {
        if (HAS_SUSPEND) {
            var level = mc.getSingleplayerServer().overworld();
            var pos = origin.offset(3, 1, 1);
            if (controlsAssemblerState == null || !level.getBlockState(pos).isAir()
                    || !level.setBlock(pos, controlsAssemblerState, 3))
                throw new IllegalStateException("Native CPU assembler prerequisite did not restore");
            controlsAssemblerState = null;
        }
    }
    private static void recordControls(CraftingCPUScreen<?> screen, String label,
            java.util.Map<String, Object> nativeState) {
        if (!Boolean.TRUE.equals(nativeState.get("active")) || !screen.getMenu().hasActiveJob)
            throw new IllegalStateException("CPU job was lost during native control input");
        String clientMode = screen.getMenu().getSchedulingMode().name();
        if (!clientMode.equals(nativeState.get("mode")))
            throw new IllegalStateException("Native CPU mode did not synchronize");
        controlsStates.add(java.util.Map.of("label", label, "clientMode", clientMode,
                "clientActive", screen.getMenu().hasActiveJob,
                "clientSuspended", controlsClientSuspended(screen), "server", nativeState));
    }
    private static void clickControls(AbstractWidget widget, BiConsumer<Double, Double> click) {
        // Exactly one pointer press/release is delegated to the original desktop
        // click path. Waiting stages below only read server/client state.
        click.accept(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0);
    }
    private static boolean verifyAdditionalControls(Minecraft mc, CraftingCPUScreen<?> screen,
            BlockPos origin, Path output, BiConsumer<Double, Double> click) throws Exception {
        if (controlsCycle != completedCycles) {
            controlsCycle = completedCycles; controlsStage = 0; controlsModeSteps = 0;
            controlsStates.clear(); controlsModes.clear();
        }
        if (controlsStage == 0) {
            controlsReading = prepareControlsHold(mc, origin); controlsStage = 1; return false;
        }
        if (controlsStage == 1) {
            if (!controlsReading.isDone()) return false;
            var state = controlsReading.join();
            if (!screen.getMenu().getSchedulingMode().name().equals(state.get("mode"))) {
                controlsReading = readControls(mc); return false;
            }
            if (HAS_SUSPEND) {
                if (!Boolean.TRUE.equals(state.get("suspended")))
                    throw new IllegalStateException("Original modern native job hold was not suspended");
                if (!controlsClientSuspended(screen)) {
                    controlsReading = readControls(mc); return false;
                }
                recordControls(screen, "fixture-held-suspended", state);
                clickControls(controlsWidget(screen, "suspend"), click);
                controlsReading = readControls(mc); controlsStage = 8; return false;
            }
            if (Boolean.TRUE.equals(state.get("suspended")) || controlsClientSuspended(screen))
                throw new IllegalStateException("Legacy native CPU job started suspended");
            controlsStage = 9;
        }
        if (controlsStage == 8) {
            if (!controlsReading.isDone()) return false;
            var state = controlsReading.join();
            if (!Boolean.FALSE.equals(state.get("suspended")) || controlsClientSuspended(screen)) {
                controlsReading = readControls(mc); return false;
            }
            controlsStage = 9;
        }
        if (controlsStage == 9) {
            var state = controlsReading.join();
            controlsInitialMode = (String) state.get("mode"); controlsPreviousMode = controlsInitialMode;
            controlsModes.add(controlsInitialMode); recordControls(screen, "initial-active", state);
            clickControls(controlsWidget(screen, "schedulingModeButton"), click);
            controlsReading = readControls(mc); controlsStage = 2; return false;
        }
        if (controlsStage == 2) {
            if (!controlsReading.isDone()) return false;
            var state = controlsReading.join(); String mode = (String) state.get("mode");
            if (mode.equals(controlsPreviousMode) || !screen.getMenu().getSchedulingMode().name().equals(mode)) {
                controlsReading = readControls(mc); return false;
            }
            controlsModeSteps++;
            int availableModes = appeng.api.config.CpuSelectionMode.values().length;
            if (controlsModeSteps < availableModes && !controlsModes.add(mode))
                throw new IllegalStateException("Native CPU mode repeated before reaching every option");
            if (controlsModeSteps == availableModes && !mode.equals(controlsInitialMode))
                throw new IllegalStateException("Native CPU modes did not wrap back to the original");
            recordControls(screen, "mode-" + controlsModeSteps, state); controlsPreviousMode = mode;
            if (controlsModeSteps < availableModes) {
                clickControls(controlsWidget(screen, "schedulingModeButton"), click);
                controlsReading = readControls(mc); return false;
            }
            if (controlsModes.size() != availableModes)
                throw new IllegalStateException("Not every original CPU scheduling mode was reached");
            controlsStage = 3;
        }
        if (controlsStage == 3) {
            if (HAS_SUSPEND) {
                clickControls(controlsWidget(screen, "suspend"), click);
                controlsReading = readControls(mc); controlsStage = 4; return false;
            }
            if (controlsField(screen, "suspend") != null)
                throw new IllegalStateException("Legacy pinned CPU unexpectedly adds a Suspend control");
            controlsStage = 6;
        }
        if (controlsStage == 4) {
            if (!controlsReading.isDone()) return false;
            var state = controlsReading.join();
            if (!Boolean.TRUE.equals(state.get("suspended")) || !controlsClientSuspended(screen)) {
                controlsReading = readControls(mc); return false;
            }
            recordControls(screen, "suspended", state);
            clickControls(controlsWidget(screen, "suspend"), click);
            controlsReading = readControls(mc); controlsStage = 5; return false;
        }
        if (controlsStage == 5) {
            if (!controlsReading.isDone()) return false;
            var state = controlsReading.join();
            if (!Boolean.FALSE.equals(state.get("suspended")) || controlsClientSuspended(screen)) {
                controlsReading = readControls(mc); return false;
            }
            recordControls(screen, "resumed", state); controlsStage = 6;
        }
        if (controlsStage == 6) {
            controlsCycles.add(java.util.Map.of("cycle", completedCycles + 1,
                    "modeTransitions", controlsModeSteps, "suspendAvailable", HAS_SUSPEND,
                    "hold", HAS_SUSPEND ? "native-suspend-then-declared-assembler-removal" : "original-legacy-assembler-removal",
                    "states", new java.util.ArrayList<>(controlsStates)));
            controlsStage = 7;
            if (completedCycles == 1) Files.writeString(output.resolve("cpu-controls.json"),
                    appeng.client.gui.style.ScreenStyle.GSON.toJson(java.util.Map.of(
                        "scope", "Original native CPU widgets on genuine held jobs; no client status assignment or input retries",
                        "cycles", controlsCycles)));
        }
        return controlsStage == 7;
    }
'''


def enhance(text,mc):
    text=text.replace('\r\n','\n')
    assert mc in {v.split('/')[0]for v in TARGETS}
    assert text.count(STATE_ANCHOR)==text.count(ACTION_ANCHOR)==text.count(RELEASE_ANCHOR)==1
    modern=mc!='1.20.1'
    helpers=HELPERS.replace('HAS_SUSPEND','true'if modern else'false')
    helpers=helpers.replace('SERVER_SUSPENDED','job.cpu().craftingLogic.isJobSuspended()'if modern else'false')
    helpers=helpers.replace('CLIENT_SUSPENDED',
        'Object status = controlsField(screen, "status");\n        return status instanceof appeng.menu.me.crafting.CraftingStatus value && value.isSuspended();'
        if modern else'return false;')
    value=text.replace(STATE_ANCHOR,STATE_ANCHOR+FIELDS,1)
    replacement=ACTION_ANCHOR.replace('            var cancel =',
        '            if (!verifyAdditionalControls(mc, screen, origin, output, click)) return false;\n            var cancel =')
    value=value.replace(ACTION_ANCHOR,replacement,1)
    value=value.replace(RELEASE_ANCHOR,RELEASE_ANCHOR+'\n                releaseControlsHold(mc, origin);',1)
    position=value.rfind('}');assert position>0
    return value[:position]+helpers+'\n'+value[position:]


def preserves_base(base,enhanced,mc):
    """Exact reversible insertion proof, not a broad similarity allowance."""
    assert enhanced.replace('\r\n','\n')==enhance(base,mc)
    assert base.count('Native submitted CPU has no active job')==enhanced.count('Native submitted CPU has no active job')==1
    assert base.count('Client Cancel did not cancel the server job')==enhanced.count('Client Cancel did not cancel the server job')==1
    assert base.count('if (completedCycles < 2)')==enhanced.count('if (completedCycles < 2)')==1
    return True


def validate_controls(target,controls,lifecycle,log):
    """Check native observations and the original two completed cancellations."""
    assert target in TARGETS;modern=not target.startswith('1.20.1/')
    required={'nativeJobSubmitted':True,'clientActiveStateReceived':True,'cancelClickedThroughDesktop':True,
        'clientIdleStateReceived':True,'serverCancellationVerified':True,'clientStatusFieldsAssignedByFixture':False,
        'nativeJobsSubmitted':2,'serverCancellationsVerified':2}
    assert set(lifecycle)==set(required)
    for key,value in required.items():assert type(lifecycle[key])is type(value)and lifecycle[key]==value,key
    assert set(controls)=={'scope','cycles'}and len(controls['cycles'])==2
    checks=[];modes={'ANY','PLAYER_ONLY','MACHINE_ONLY'};first=None
    for number,cycle in enumerate(controls['cycles'],1):
        assert type(cycle['cycle'])is int and cycle['cycle']==number
        assert type(cycle['modeTransitions'])is int and cycle['modeTransitions']==3
        assert cycle['suspendAvailable']is modern
        assert cycle['hold']==('native-suspend-then-declared-assembler-removal'if modern else'original-legacy-assembler-removal')
        states=cycle['states'];labels=(['fixture-held-suspended']if modern else[])+['initial-active','mode-1','mode-2','mode-3']+(['suspended','resumed']if modern else[])
        assert [v['label']for v in states]==labels
        for value in states:
            server=value['server'];mode=value['clientMode']
            assert mode in modes and mode==server['mode']
            assert value['clientActive']is True and server['active']is True
            paused=value['label']in {'fixture-held-suspended','suspended'}
            assert value['clientSuspended']is paused and server['suspended']is paused
        if modern:
            assert states[0]['clientMode']==states[1]['clientMode']
            states=states[1:]
        initial=states[0]['clientMode']
        if first is None:first=initial
        assert initial==first and states[3]['clientMode']==initial
        assert {v['clientMode']for v in states[:3]}==modes
        assert all(states[i]['clientMode']!=states[i-1]['clientMode']for i in [1,2,3])
        prefix='cycle'+str(number)+' '
        checks.append(dict(name=prefix+'native active job has synchronized original mode',passed=True))
        for i in [1,2,3]:checks.append(dict(name=prefix+'native pointer changes scheduling mode '+str(i)+' on server and client',passed=True))
        checks.append(dict(name=prefix+'all original scheduling modes wrap with persisted original value',passed=True))
        if modern:
            assert all(v['clientMode']==initial for v in states[4:])
            checks.extend([dict(name=prefix+'native Resume releases the original suspended fixture hold without completing the job',passed=True),
                dict(name=prefix+'native Suspend pauses both server job and client status',passed=True),
                dict(name=prefix+'native Resume restores both server job and client status',passed=True)])
        marker='CPU LIFECYCLE VERIFIED cycle='+str(number)+' output=256 acacia planks'
        assert log.count(marker)==1,marker
        checks.append(dict(name=prefix+'original desktop Cancel verified on native server job',passed=True))
    checks.append(dict(name='original two-job lifecycle completed without fixture-assigned client status',passed=True))
    return checks
