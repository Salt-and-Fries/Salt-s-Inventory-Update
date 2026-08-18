param(
    [string] $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
)

$ErrorActionPreference = 'Stop'

$failures = [System.Collections.Generic.List[string]]::new()

function Add-Failure {
    param([string] $Message)
    $failures.Add($Message)
    Write-Host "FAIL $Message" -ForegroundColor Red
}

function Assert-File {
    param(
        [string] $Path,
        [string] $Label
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Add-Failure "$Label missing: $Path"
        return $false
    }

    return $true
}

function Assert-Contains {
    param(
        [string] $Path,
        [string] $Text,
        [string] $Label
    )

    if (-not (Assert-File -Path $Path -Label $Label)) {
        return
    }

    $content = Get-Content -LiteralPath $Path -Raw
    if (-not $content.Contains($Text)) {
        Add-Failure "$Label missing text '$Text' in $Path"
    } else {
        Write-Host "PASS $Label" -ForegroundColor Green
    }
}

function Assert-NotContains {
    param(
        [string] $Path,
        [string] $Text,
        [string] $Label
    )

    if (-not (Assert-File -Path $Path -Label $Label)) {
        return
    }

    $content = Get-Content -LiteralPath $Path -Raw
    if ($content.Contains($Text)) {
        Add-Failure "$Label unexpectedly found text '$Text' in $Path"
    } else {
        Write-Host "PASS $Label" -ForegroundColor Green
    }
}

function Assert-Count {
    param(
        [string] $Path,
        [string] $Text,
        [int] $Expected,
        [string] $Label
    )

    if (-not (Assert-File -Path $Path -Label $Label)) {
        return
    }

    $content = Get-Content -LiteralPath $Path -Raw
    $count = ([regex]::Matches($content, [regex]::Escape($Text))).Count
    if ($count -ne $Expected) {
        Add-Failure "$Label expected $Expected occurrence(s) of '$Text' but found $count in $Path"
    } else {
        Write-Host "PASS $Label" -ForegroundColor Green
    }
}

function Assert-Matches {
    param(
        [string] $Path,
        [string] $Pattern,
        [string] $Label
    )

    if (-not (Assert-File -Path $Path -Label $Label)) {
        return
    }

    $content = Get-Content -LiteralPath $Path -Raw
    if (-not [regex]::IsMatch($content, $Pattern)) {
        Add-Failure "$Label missing pattern '$Pattern' in $Path"
    } else {
        Write-Host "PASS $Label" -ForegroundColor Green
    }
}

$versions = @(
    '1.20.1',
    '1.21.1',
    '1.21.11',
    '26.1.2',
    '26.2'
)

$loaderMatrix = @(
    @{ Version = '1.20.1'; Loader = 'fabric'; LoaderClass = 'SaltsInventoryUpdateFabric.java'; ClientClass = 'SaltsInventoryUpdateFabricClient.java'; Metadata = 'fabric.mod.json'; MetadataKind = 'fabric' },
    @{ Version = '1.20.1'; Loader = 'forge'; LoaderClass = 'SaltsInventoryUpdateForge.java'; Metadata = 'META-INF/mods.toml'; MetadataKind = 'toml' },
    @{ Version = '1.21.1'; Loader = 'fabric'; LoaderClass = 'SaltsInventoryUpdateFabric.java'; ClientClass = 'SaltsInventoryUpdateFabricClient.java'; Metadata = 'fabric.mod.json'; MetadataKind = 'fabric' },
    @{ Version = '1.21.1'; Loader = 'neoforge'; LoaderClass = 'SaltsInventoryUpdateNeoForge.java'; Metadata = 'META-INF/neoforge.mods.toml'; MetadataKind = 'toml' },
    @{ Version = '1.21.11'; Loader = 'fabric'; LoaderClass = 'SaltsInventoryUpdateFabric.java'; ClientClass = 'SaltsInventoryUpdateFabricClient.java'; Metadata = 'fabric.mod.json'; MetadataKind = 'fabric' },
    @{ Version = '1.21.11'; Loader = 'neoforge'; LoaderClass = 'SaltsInventoryUpdateNeoForge.java'; Metadata = 'META-INF/neoforge.mods.toml'; MetadataKind = 'toml' },
    @{ Version = '26.1.2'; Loader = 'fabric'; LoaderClass = 'SaltsInventoryUpdateFabric.java'; ClientClass = 'SaltsInventoryUpdateFabricClient.java'; Metadata = 'fabric.mod.json'; MetadataKind = 'fabric' },
    @{ Version = '26.1.2'; Loader = 'neoforge'; LoaderClass = 'SaltsInventoryUpdateNeoForge.java'; Metadata = 'META-INF/neoforge.mods.toml'; MetadataKind = 'toml' },
    @{ Version = '26.2'; Loader = 'fabric'; LoaderClass = 'SaltsInventoryUpdateFabric.java'; ClientClass = 'SaltsInventoryUpdateFabricClient.java'; Metadata = 'fabric.mod.json'; MetadataKind = 'fabric' },
    @{ Version = '26.2'; Loader = 'neoforge'; LoaderClass = 'SaltsInventoryUpdateNeoForge.java'; Metadata = 'META-INF/neoforge.mods.toml'; MetadataKind = 'toml' }
)

$baseMenus = @(
    'GENERIC_9x1',
    'GENERIC_9x2',
    'GENERIC_9x3',
    'GENERIC_9x4',
    'GENERIC_9x5',
    'GENERIC_9x6',
    'GENERIC_3x3',
    'ANVIL',
    'BEACON',
    'BLAST_FURNACE',
    'BREWING_STAND',
    'CRAFTING',
    'ENCHANTMENT',
    'FURNACE',
    'GRINDSTONE',
    'HOPPER',
    'LOOM',
    'MERCHANT',
    'SHULKER_BOX',
    'SMITHING',
    'SMOKER',
    'CARTOGRAPHY_TABLE',
    'STONECUTTER'
)

$payloads = @(
    'InventorySlotPurchasePayload',
    'InventoryExpansionSyncPayload',
    'DesktopHelloPayload',
    'DesktopHelloAckPayload',
    'DesktopModePayload',
    'DesktopClickPayload',
    'DesktopQuickMovePayload',
    'DesktopButtonPayload',
    'DesktopPlaceRecipePayload',
    'DesktopJeiTransferPayload',
    'DesktopRenamePayload',
    'DesktopCustomPayload',
    'DesktopCloseSessionPayload',
    'DesktopSessionPinPayload',
    'DesktopSessionVisibilityPayload',
    'DesktopOpenLinkedSourcesPayload',
    'DesktopOpenSessionPayload',
    'DesktopSlotPayload',
    'DesktopDataPayload',
    'DesktopCarriedPayload',
    'DesktopGhostRecipePayload',
    'DesktopSessionClosedPayload',
    'DesktopMerchantOffersPayload'
)

foreach ($version in $versions) {
    Write-Host "== $version shared source ==" -ForegroundColor Cyan

    $fabricRoot = Join-Path $RepoRoot "versions\$version\fabric\src\main\java\com\salts_inventory_update"
    $client = Join-Path $fabricRoot 'client\InventoryDesktopScreen.java'
    $server = Join-Path $fabricRoot 'server\DesktopContainerSessions.java'
    $packets = Join-Path $fabricRoot 'network\DesktopPackets.java'
    $windowedClient = Join-Path $fabricRoot 'client\WindowedInventoryClient.java'
    $config = Join-Path $fabricRoot 'client\SaltsInventoryConfig.java'
    $configScreen = Join-Path $fabricRoot 'client\SaltsInventoryConfigScreen.java'
    $containerClient = Join-Path $fabricRoot 'client\DesktopContainerClient.java'
    $stateStore = Join-Path $fabricRoot 'client\DesktopWindowStateStore.java'
    $inventoryMixin = Join-Path $fabricRoot 'mixin\InventoryExpansionInventoryMixin.java'
    $inventorySlot = Join-Path $fabricRoot 'inventory\InventoryExpansionSlot.java'
    $serverPlayerMixin = Join-Path $fabricRoot 'mixin\server\ServerPlayerInventoryExpansionMixin.java'
    $deathMixin = if ($version -eq '1.21.1' -or $version -eq '1.21.11') {
        Join-Path $fabricRoot 'mixin\server\ServerPlayerMixin.java'
    } else {
        $serverPlayerMixin
    }
    $tomsPayloads = Join-Path $fabricRoot 'compat\toms_storage\TomsStoragePayloads.java'
    $language = Join-Path $RepoRoot "versions\$version\common\src\main\resources\assets\salts_inventory_update\lang\en_us.json"

    $menus = [System.Collections.Generic.List[string]]::new()
    $baseMenus | ForEach-Object { $menus.Add($_) }
    if ($version -ne '1.20.1') {
        $menus.Add('CRAFTER_3x3')
    }

    foreach ($menu in $menus) {
        Assert-Contains -Path $client -Text "MenuType.$menu" -Label "$version client menu $menu"
        Assert-Contains -Path $server -Text "MenuType.$menu" -Label "$version server menu $menu"
    }

    foreach ($payload in $payloads) {
        Assert-Contains -Path $packets -Text $payload -Label "$version packet $payload"
    }

    Assert-Contains -Path $packets -Text 'desktop_ready' -Label "$version legacy protocol detector"
    Assert-Matches -Path $packets -Pattern 'Desktop(?:SourceLink|LinkSessions|Link)Payload' -Label "$version authenticated link mutation payload"
    Assert-Matches -Path $packets -Pattern '(?:DesktopAuthenticatedPayload|MutationStamp|sessionToken)' -Label "$version session mutation authorization"
    Assert-NotContains -Path $packets -Text 'List<Integer> recipeSlotIds' -Label "$version recipe transfer does not trust client slot IDs"
    Assert-NotContains -Path $packets -Text 'DesktopJeiTransferRequirement' -Label "$version recipe transfer does not trust client ingredients"

    Assert-Contains -Path $packets -Text 'PIN_MODE_GHOST_PINNED' -Label "$version packet pin modes"
    Assert-Contains -Path $packets -Text 'QUICK_TARGET_HOTBAR' -Label "$version quick move target"
    Assert-Contains -Path $windowedClient -Text 'GLFW_KEY_C' -Label "$version character keybind"
    Assert-Contains -Path $windowedClient -Text '"key.salts_inventory_update.mouse_focus"' -Label "$version mouse focus keybind registration"
    Assert-Contains -Path $windowedClient -Text 'GLFW_KEY_LEFT_ALT' -Label "$version mouse focus default key"
    Assert-Contains -Path $windowedClient -Text 'getBoundKeyOf(mouseFocusKey)' -Label "$version configurable mouse focus polling"
    Assert-Contains -Path $windowedClient -Text 'isKeyModifierActive(mouseFocusKey)' -Label "$version mouse focus modifier polling"
    Assert-Contains -Path $windowedClient -Text 'mouseFocusKey.isDefault() && isAltDown(minecraft)' -Label "$version default right Alt compatibility"
    Assert-Contains -Path $windowedClient -Text 'screen == null && isMouseFocusKeyDown(minecraft)' -Label "$version hotbar-only custom focus key"
    Assert-Contains -Path $windowedClient -Text 'isHotbarOnly() && !mouseFocusDown' -Label "$version hotbar-only custom focus release"
    Assert-Count -Path $client -Text 'InstructionsLine.mouseFocus(' -Expected 2 -Label "$version dynamic mouse focus help variants"
    Assert-Contains -Path $client -Text 'WindowedInventoryClient.mouseFocusKeyName()' -Label "$version live mouse focus help label"
    Assert-Contains -Path $language -Text '"key.salts_inventory_update.mouse_focus"' -Label "$version mouse focus translation"
    Assert-Contains -Path $windowedClient -Text '"saltsinventory"' -Label "$version client command root"
    Assert-Contains -Path $windowedClient -Text '"config"' -Label "$version config command"
    Assert-Contains -Path $windowedClient -Text 'FunctionalTestHarness' -Label "$version functional hook"
    Assert-Contains -Path $config -Text 'enableMod' -Label "$version config enableMod"
    Assert-Contains -Path $config -Text 'expandableInventory' -Label "$version config expandableInventory"
    Assert-Contains -Path $config -Text 'enableGhostPins' -Label "$version config ghost pins"
    Assert-Contains -Path $config -Text 'globalPins' -Label "$version config global pins"
    Assert-Contains -Path $configScreen -Text '"global_pins"' -Label "$version global pins config screen"
    Assert-Contains -Path $language -Text '"config.salts_inventory_update.global_pins"' -Label "$version global pins translation"
    Assert-Contains -Path $stateStore -Text 'globalWindows' -Label "$version global window state partition"
    Assert-Contains -Path $stateStore -Text 'source:block:' -Label "$version block window pins remain world scoped"
    Assert-Contains -Path $stateStore -Text 'source:chest:' -Label "$version chest window pins remain world scoped"

    Assert-Contains -Path $server -Text 'BoundedLinkGraph' -Label "$version server-owned bounded link graph"
    Assert-Contains -Path $server -Text 'BoundedTransferPlanner' -Label "$version bounded shared recipe-transfer planner"
    Assert-Contains -Path $server -Text 'SecureRandom' -Label "$version cryptographic session nonces"
    Assert-Contains -Path $server -Text 'TokenBucket' -Label "$version bounded request rates"
    Assert-Contains -Path $server -Text 'MAX_DESKTOP_SESSIONS' -Label "$version server session aggregate cap"
    Assert-Contains -Path $server -Text 'WeakReference' -Label "$version exact block-entity source identity"
    Assert-Matches -Path $server -Pattern 'PROTOCOL_REJECT' -Label "$version once-only protocol rejection state"
    Assert-Contains -Path $server -Text 'capabilityForSession' -Label "$version session-derived base capability gate"
    Assert-Matches -Path $server -Pattern '(?:serverHandlerCallbackDepth|handlerCallbackDepth|callbackDepth)' -Label "$version nested handler callback tracking"
    Assert-Matches -Path $server -Pattern '(?:serverHandlerBroadcastPending|handlerBroadcastPending|pendingCallbackBroadcast)' -Label "$version coalesced handler broadcasts"
    Assert-Contains -Path $server -Text 'quarantineServerHandler' -Label "$version faulting server handler quarantine"
    Assert-Contains -Path $server -Text 'Double.isFinite(range)' -Label "$version finite interaction-range validation"
    Assert-Contains -Path $server -Text 'SLOT_CLICKED_OUTSIDE' -Label "$version strict negative click-slot validation"
    Assert-Contains -Path $server -Text 'payload.targetKind() < DesktopPackets.QUICK_TARGET_DEFAULT' -Label "$version quick-move target discriminator lower bound"
    Assert-Contains -Path $server -Text 'payload.targetKind() > DesktopPackets.QUICK_TARGET_HOTBAR' -Label "$version quick-move target discriminator upper bound"
    Assert-Contains -Path $server -Text 'payload.targetKind() != DesktopPackets.QUICK_TARGET_SESSION' -Label "$version explicit session target bypasses vanilla result routing"
    Assert-Matches -Path $server -Pattern '(?s)sendCarriedChange\([^)]*ItemStack stack\).{0,1400}(?:this\.)?player\.inventoryMenu\.getCarried\(\)' -Label "$version detached synchronizer preserves canonical player cursor"
    Assert-Contains -Path $server -Text 'mayInteract(player, pos)' -Label "$version linked-source protection check"
    Assert-NotContains -Path $server -Text 'SaltsInventoryRuntime' -Label "$version server decisions do not use client-global config"
    Assert-NotContains -Path $server -Text 'DesktopJeiTransferRequirement' -Label "$version server ignores client ingredient authority"
    Assert-NotContains -Path $server -Text 'ThreadLocalRandom' -Label "$version server authorization tokens are not predictable PRNG output"
    Assert-NotContains -Path $containerClient -Text 'ThreadLocalRandom' -Label "$version client handshake nonce uses secure randomness"
    Assert-Matches -Path $containerClient -Pattern 'MODE_(?:RESEND|REFRESH)_INTERVAL_TICKS' -Label "$version dropped mode updates eventually converge"
    Assert-Contains -Path $containerClient -Text 'MAX_DESKTOP_SESSIONS' -Label "$version client session aggregate cap"
    Assert-Contains -Path $stateStore -Text 'AtomicUtf8File' -Label "$version atomic state persistence"
    Assert-Contains -Path $stateStore -Text 'StableStateIdentity' -Label "$version stable private world identity"
    Assert-Matches -Path $stateStore -Pattern '(?:hasSafeSnbtStructure|isSafeLocalState)' -Label "$version bounded local SNBT parser input"
    Assert-Contains -Path $tomsPayloads -Text 'MAX_TOMS_COMPRESSED_BYTES' -Label "$version Tom's compressed payload cap"
    Assert-Contains -Path $tomsPayloads -Text 'MAX_TOMS_DECOMPRESSED_BYTES' -Label "$version Tom's decompressed payload cap"
    Assert-Contains -Path $tomsPayloads -Text 'MAX_TOMS_DEPTH' -Label "$version Tom's NBT depth cap"
    Assert-Contains -Path $tomsPayloads -Text 'MAX_TOMS_NODES' -Label "$version Tom's NBT node cap"
    Assert-Contains -Path $tomsPayloads -Text 'MAX_TOMS_STRING_BYTES' -Label "$version Tom's NBT string cap"
    Assert-Contains -Path $tomsPayloads -Text 'MAX_TOMS_PRIMITIVE_ARRAY_BYTES' -Label "$version Tom's NBT primitive-array cap"
    Assert-Contains -Path $tomsPayloads -Text 'StackOverflowError' -Label "$version Tom's recursive decoder fail-safe"
    Assert-Contains -Path $config -Text 'MAX_FORCED_MENU_IDS' -Label "$version forced-menu config count cap"
    Assert-Contains -Path $config -Text 'MAX_IDENTIFIER_LENGTH' -Label "$version forced-menu identifier length cap"
    Assert-Contains -Path $inventorySlot -Text 'isGameplay' -Label "$version dormant extra slots reject vanilla access"
    Assert-Contains -Path $inventorySlot -Text 'boolean isActive()' -Label "$version extra-slot activation gate"
    Assert-Contains -Path $inventorySlot -Text 'boolean mayPickup(Player player)' -Label "$version extra-slot pickup gate"
    Assert-Contains -Path $inventorySlot -Text 'boolean mayPlace(ItemStack stack)' -Label "$version extra-slot placement gate"
    Assert-Matches -Path $inventoryMixin -Pattern '(?s)isExpansionInventoryEmpty\([^)]*\).{0,220}if \(\s*cir\.getReturnValueZ\(\)\s*&&\s*!InventoryExpansion\.access' -Label "$version extra contents remain visible to lifecycle isEmpty"
    Assert-Contains -Path $deathMixin -Text 'method = "die"' -Label "$version transient menu inputs close before death drops"
    Assert-Contains -Path $serverPlayerMixin -Text 'method = "restoreFrom"' -Label "$version player replacement state transfer"
}

foreach ($version in $versions) {
    Write-Host "== $version return hotbar to inventory ==" -ForegroundColor Cyan

    $hotbarFabricRoot = Join-Path $RepoRoot "versions\$version\fabric\src\main\java\com\salts_inventory_update"
    $hotbarClient = Join-Path $hotbarFabricRoot 'client\InventoryDesktopScreen.java'
    $hotbarConfig = Join-Path $hotbarFabricRoot 'client\SaltsInventoryConfig.java'
    $hotbarConfigScreen = Join-Path $hotbarFabricRoot 'client\SaltsInventoryConfigScreen.java'
    $hotbarWindowedClient = Join-Path $hotbarFabricRoot 'client\WindowedInventoryClient.java'
    $hotbarMixinClass = if ($version -eq '26.2') { 'HudMixin.java' } else { 'GuiMixin.java' }
    $hotbarHudMixin = Join-Path $hotbarFabricRoot "mixin\client\$hotbarMixinClass"
    $hotbarRenderMethod = switch ($version) {
        '1.20.1' { 'renderHotbar' }
        '1.21.1' { 'renderItemHotbar' }
        '1.21.11' { 'renderItemHotbar' }
        default { 'extractItemHotbar' }
    }
    $hotbarLanguage = Join-Path $RepoRoot "versions\$version\common\src\main\resources\assets\salts_inventory_update\lang\en_us.json"

Assert-Contains -Path $hotbarConfig -Text 'public boolean returnHotbarToInventory = false;' -Label "$version return-hotbar config defaults off"
Assert-Contains -Path $hotbarConfig -Text 'this.returnHotbarToInventory = defaults.returnHotbarToInventory;' -Label "$version return-hotbar config resets to default"
Assert-Contains -Path $hotbarConfigScreen -Text '"return_hotbar_to_inventory"' -Label "$version return-hotbar config-screen toggle"
Assert-Contains -Path $hotbarConfigScreen -Text 'config.returnHotbarToInventory = value' -Label "$version return-hotbar config-screen update"
Assert-Contains -Path $hotbarLanguage -Text '"config.salts_inventory_update.return_hotbar_to_inventory"' -Label "$version return-hotbar translation"
Assert-Matches -Path $hotbarLanguage -Pattern '(?i)"config\.salts_inventory_update\.return_hotbar_to_inventory\.description": "[^"]*creative' -Label "$version return-hotbar description covers Creative"
Assert-Matches -Path $hotbarClient -Pattern '(?i)Enable Return Hotbar to Inventory[^\r\n]*Creative' -Label "$version in-game instructions cover Creative"

Assert-Contains -Path $hotbarHudMixin -Text "@Inject(method = `"$hotbarRenderMethod`", at = @At(`"HEAD`"), cancellable = true)" -Label "$version vanilla hotbar cancellable injection"
Assert-Contains -Path $hotbarHudMixin -Text 'WindowedInventoryClient.shouldHideHotbar()' -Label "$version vanilla hotbar cancellation predicate"
Assert-Contains -Path $hotbarWindowedClient -Text 'public static boolean shouldHideHotbar()' -Label "$version hotbar visibility predicate"
Assert-Matches -Path $hotbarWindowedClient -Pattern '(?s)public static boolean shouldHideHotbar\(\).{0,700}(?:returnHotbarToInventory.{0,400}screen\.hasWindows\(\)|screen\.hasWindows\(\).{0,400}returnHotbarToInventory)' -Label "$version hotbar hidden only for configured open windows"

Assert-Contains -Path $hotbarClient -Text 'private static final int INVENTORY_HOTBAR_GAP' -Label "$version inventory hotbar gap constant"
Assert-Contains -Path $hotbarClient -Text 'private static final int INVENTORY_HOTBAR_WIDTH' -Label "$version inventory hotbar width constant"
Assert-Contains -Path $hotbarClient -Text 'private static final int INVENTORY_HOTBAR_RESERVED_HEIGHT = INVENTORY_HOTBAR_GAP + SLOT_SIZE;' -Label "$version inventory hotbar reserved-height marker"
Assert-Contains -Path $hotbarClient -Text 'usesInventoryWindowHotbar()' -Label "$version inventory hotbar mode helper"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private void renderInventoryWindow\([^)]*\).{0,1600}renderInventoryHotbar\(' -Label "$version inventory window renders embedded hotbar"
Assert-Matches -Path $hotbarClient -Pattern '(?s)this\.kind == WindowKind\.INVENTORY.{0,2200}inventoryHotbarSlotAt\(' -Label "$version inventory window hits embedded hotbar"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private int minResizableWidth\(InventoryWindow window\).{0,1200}INVENTORY_HOTBAR_WIDTH' -Label "$version inventory minimum width reserves hotbar"
Assert-Contains -Path $hotbarClient -Text 'int reservedHeight = inventoryHotbarReservedHeight(window);' -Label "$version inventory grid layout reserves hotbar row"

Assert-Matches -Path $hotbarClient -Pattern '(?s)private void renderCharacterWindow\([^)]*\).{0,2400}(?:returnHotbarToInventory.{0,900}offhandSlot|offhandSlot.{0,900}returnHotbarToInventory)' -Label "$version character window conditionally renders offhand"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private @Nullable SlotHit characterOffhandSlotAt\([^)]*\).{0,1200}returnHotbarToInventory.{0,1200}offhandSlot' -Label "$version character offhand hit helper is conditional"
Assert-Contains -Path $hotbarClient -Text 'SlotHit offhandHit = screen.characterOffhandSlotAt(this, mouseX, mouseY);' -Label "$version character window hits offhand"
Assert-Contains -Path $hotbarClient -Text 'private static final int CHARACTER_MODEL_SLOT_GAP = CHARACTER_MODEL_X - (CHARACTER_ARMOR_X - 1 + SLOT_SIZE);' -Label "$version character offhand mirrors armor-model frame gap"
Assert-Contains -Path $hotbarClient -Text 'private static final int CHARACTER_OFFHAND_X = CHARACTER_MODEL_X + CHARACTER_MODEL_WIDTH + CHARACTER_MODEL_SLOT_GAP + 1;' -Label "$version character offhand uses mirrored frame gap"
Assert-Contains -Path $hotbarClient -Text 'private static final int CHARACTER_CRAFT_VERTICAL_OFFSET = 6;' -Label "$version character crafting group downward offset"
Assert-Contains -Path $hotbarClient -Text 'private static final int CHARACTER_CRAFT_Y = 22 + CHARACTER_CRAFT_VERTICAL_OFFSET;' -Label "$version character crafting grid applies shared offset"
Assert-Contains -Path $hotbarClient -Text 'private static final int CHARACTER_CRAFT_ARROW_Y = 31 + CHARACTER_CRAFT_VERTICAL_OFFSET;' -Label "$version character crafting arrow applies shared offset"
Assert-Contains -Path $hotbarClient -Text 'private static final int CHARACTER_CRAFT_RESULT_Y = 31 + CHARACTER_CRAFT_VERTICAL_OFFSET;' -Label "$version character crafting result and recipe button apply shared offset"

Assert-Matches -Path $hotbarClient -Pattern '(?s)private static int creativeWindowHeight\(\).{0,500}returnHotbarToInventory.{0,300}INVENTORY_HOTBAR_RESERVED_HEIGHT' -Label "$version creative window conditionally reserves hotbar footer"
Assert-Matches -Path $hotbarClient -Pattern '(?s)else if \(kind == WindowKind\.CREATIVE\).{0,500}creativeWindowWidth\(\).{0,200}creativeWindowHeight\(\)' -Label "$version new creative window uses hotbar-aware fixed size"
Assert-Contains -Path $hotbarClient -Text 'case CREATIVE -> DesktopWindowSize.of(creativeWindowWidth(), creativeWindowHeight());' -Label "$version creative default size uses hotbar-aware height"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private DesktopWindowSize defaultInventoryPlacementAnchorSize\(WindowKind inventoryKind\).{0,500}DesktopWindowSize\.of\(creativeWindowWidth\(\), creativeWindowHeight\(\)\)' -Label "$version creative placement anchor uses hotbar-aware height"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private void forceFixedWindowSize\(InventoryWindow window\).{0,400}WindowKind\.CREATIVE.{0,300}creativeWindowWidth\(\).{0,200}creativeWindowHeight\(\)' -Label "$version restored creative windows cannot retain stale height"
Assert-Matches -Path $hotbarClient -Pattern '(?s)public void refreshInventoryWindowLayout\(\).{0,700}WindowKind\.CREATIVE.{0,300}forceFixedWindowSize.{0,200}clampWindowIntoDesktop' -Label "$version live creative window follows config layout changes"
Assert-Matches -Path $hotbarConfigScreen -Pattern '(?s)boolean \w+ = SaltsInventoryConfig\.get\(\)\.returnHotbarToInventory;.{0,300}config\.returnHotbarToInventory = value.{0,300}refreshInventoryWindowLayout\(\w+\)' -Label "$version hotbar toggle preserves the previous footer state"
Assert-Matches -Path $hotbarClient -Pattern '(?s)public void refreshInventoryWindowLayout\(boolean \w+\).{0,500}int \w*[Dd]elta = \w+ - previousReservedHeight;.{0,1200}window\.height = clamp\(window\.height \+ \w*[Dd]elta, minHeight, maxHeight\)' -Label "$version live inventory toggle adds and removes only the footer height"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private void renderCreativeWindow\([^)]*\).{0,1000}renderCreativeInventoryTab\(.{0,400}renderCreativeCatalogTab\(.{0,300}renderCreativeHotbar\(' -Label "$version creative hotbar renders after every tab body"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private @Nullable SlotHit creativeHotbarSlotAt\([^)]*\).{0,1400}usesInventoryWindowHotbar\(\).{0,700}hotbarSlots\(\).{0,700}DesktopPackets\.PLAYER_MENU_SESSION' -Label "$version creative hotbar uses real player slots"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private boolean creativeMouseClicked\([^)]*\).{0,1800}creativeHotbarSlotAt\(.{0,400}handleSlotMouseClicked\(hotbarHit.{0,500}CreativeModeTab selectedTab' -Label "$version creative hotbar clicks work before tab-specific handling"
Assert-Matches -Path $hotbarClient -Pattern '(?s)this\.kind == WindowKind\.CREATIVE.{0,400}creativeHotbarSlotAt\(.{0,300}creativeInventorySlotAt\(' -Label "$version creative hover and drag paths prioritize the hotbar on every tab"
Assert-Matches -Path $hotbarClient -Pattern '(?s)this\.kind == WindowKind\.CREATIVE.{0,400}creativeInventorySlotAt\(.{0,200}else if \(this\.kind == WindowKind\.CHARACTER\)' -Label "$version non-character windows cannot hit character slots"
Assert-Contains -Path $hotbarClient -Text 'return window.x + (window.width - INVENTORY_HOTBAR_WIDTH) / 2;' -Label "$version creative hotbar is horizontally centered"
Assert-Contains -Path $hotbarClient -Text 'return window.y + window.height - CREATIVE_CONTENT_MARGIN - SLOT_SIZE;' -Label "$version creative hotbar stays at window bottom"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private void renderCreativeInventoryTab\([^)]*\).{0,1400}creativeInventoryGridLayout\(\).{0,700}window\.creativeScrollRow \* CREATIVE_GRID_COLUMNS.{0,700}CREATIVE_GRID_COLUMNS \* CREATIVE_GRID_ROWS' -Label "$version expanded creative inventory is constrained to the five-row viewport"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private @Nullable SlotHit creativeInventorySlotAt\([^)]*\).{0,1500}creativeInventoryGridLayout\(\).{0,700}window\.creativeScrollRow \* CREATIVE_GRID_COLUMNS.{0,700}CREATIVE_GRID_COLUMNS \* CREATIVE_GRID_ROWS' -Label "$version creative inventory hit testing follows the visible scroll viewport"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private void renderCreativeInventoryScrollbar\([^)]*CreativeGridLayout layout\).{0,1200}CREATIVE_SCROLLER_SPRITE.{0,500}CREATIVE_INVENTORY_SCROLLBAR_TRACK_HEIGHT' -Label "$version expanded creative inventory scrollbar is active"
Assert-Matches -Path $hotbarClient -Pattern '(?s)private @Nullable InventoryIncreaseButtonRect creativeIncreaseInventoryButtonRect\([^)]*\).{0,1200}creativeInventoryGridLayout\(\).{0,500}buttonIndex - window\.creativeScrollRow \* CREATIVE_GRID_COLUMNS' -Label "$version creative expansion button follows the inventory scroll viewport"

Assert-Matches -Path $hotbarClient -Pattern '(?s)(?:usesInventoryWindowHotbar\(\).{0,500}renderDesktopHotbarAffordances|renderDesktopHotbarAffordances.{0,500}usesInventoryWindowHotbar\(\))' -Label "$version desktop HUD hotbar affordances gated"
Assert-Matches -Path $hotbarClient -Pattern '(?s)(?:usesInventoryWindowHotbar\(\).{0,700}hotbarSlotAt|hotbarSlotAt.{0,700}usesInventoryWindowHotbar\(\))' -Label "$version desktop HUD hotbar hits gated"
Assert-Matches -Path $hotbarClient -Pattern '(?s)(?:usesInventoryWindowHotbar\(\).{0,700}offhandSlotAt|offhandSlotAt.{0,700}usesInventoryWindowHotbar\(\))' -Label "$version desktop HUD offhand hits gated"
}

$rootBuildScript = Join-Path $RepoRoot 'build.gradle.kts'
$settingsScript = Join-Path $RepoRoot 'settings.gradle.kts'
Assert-Contains -Path $rootBuildScript -Text 'version = "0.1.2"' -Label 'release version is 0.1.2'
Assert-Contains -Path $rootBuildScript -Text 'org.jspecify:jspecify:1.0.0' -Label 'JSpecify is an explicit compile-only dependency'
Assert-Contains -Path $rootBuildScript -Text '"**/compat/rei/SaltsReiClientPlugin.java"' -Label 'non-Fabric loaders exclude only the Fabric REI entrypoint'
Assert-NotContains -Path $rootBuildScript -Text '"**/compat/rei/**"' -Label 'non-Fabric loaders retain shared REI runtime support'
Assert-NotContains -Path $rootBuildScript -Text '"--mixin.config", "salts_inventory_update.mixins.json"' -Label 'development runs do not duplicate the loader-supplied mixin config'
Assert-Contains -Path $settingsScript -Text 'id("dev.prism.settings") version "0.5.17"' -Label 'Prism settings plugin version is reproducibly pinned'

foreach ($version in $versions) {
    $runtimeRei = Join-Path $RepoRoot "versions\$version\fabric\src\main\java\com\salts_inventory_update\compat\rei\RuntimeReiRecipeBrowserAccess.java"
    Assert-File -Path $runtimeRei -Label "$version shared REI runtime access retained" | Out-Null

    $nonFabricLoader = if ($version -eq '1.20.1') { 'forge' } else { 'neoforge' }
    $nonFabricRei = Join-Path $RepoRoot "versions\$version\$nonFabricLoader\src\main\java\com\salts_inventory_update\compat\rei\SaltsReiForgeClientPlugin.java"
    Assert-File -Path $nonFabricRei -Label "$version non-Fabric REI client plugin retained" | Out-Null
}

$packFormats = @{
    '1.20.1' = 15
    '1.21.1' = 34
    '1.21.11' = 75
    '26.1.2' = 84
    '26.2' = 88
}
$mixinLevels = @{
    '1.20.1' = 'JAVA_17'
    '1.21.1' = 'JAVA_21'
    '1.21.11' = 'JAVA_21'
    '26.1.2' = 'JAVA_25'
    '26.2' = 'JAVA_25'
}
$canonicalHotbarDescription = 'Places the hotbar below the player inventory and creative windows and the offhand slot in the character window. The normal HUD hotbar is hidden while any Salt window is open.'
foreach ($version in $versions) {
    $commonResources = Join-Path $RepoRoot "versions\$version\common\src\main\resources"
    Assert-Contains -Path (Join-Path $commonResources 'pack.mcmeta') -Text "`"pack_format`": $($packFormats[$version])" -Label "$version exact pack format"
    if ($version -in @('1.21.11', '26.1.2', '26.2')) {
        Assert-Contains -Path (Join-Path $commonResources 'pack.mcmeta') -Text "`"min_format`": $($packFormats[$version])" -Label "$version exact minimum pack format"
        Assert-Contains -Path (Join-Path $commonResources 'pack.mcmeta') -Text "`"max_format`": $($packFormats[$version])" -Label "$version exact maximum pack format"
    }
    Assert-Contains -Path (Join-Path $commonResources 'salts_inventory_update.mixins.json') -Text "`"compatibilityLevel`": `"$($mixinLevels[$version])`"" -Label "$version Mixin/Java compatibility"
    Assert-Contains -Path (Join-Path $commonResources 'assets\salts_inventory_update\lang\en_us.json') -Text $canonicalHotbarDescription -Label "$version canonical hotbar translation"

    $resourceArtwork = Get-ChildItem -LiteralPath $commonResources -Recurse -Filter '*.pdn' -File -ErrorAction SilentlyContinue
    if ($resourceArtwork) {
        Add-Failure "$version resource tree contains editor artwork: $($resourceArtwork.FullName -join ', ')"
    } else {
        Write-Host "PASS $version resource tree excludes editor artwork" -ForegroundColor Green
    }

    $mixinRoot = Join-Path $RepoRoot "versions\$version\fabric\src\main\java\com\salts_inventory_update\mixin"
    $mixinConfigPath = Join-Path $commonResources 'salts_inventory_update.mixins.json'
    if ((Assert-File -Path $mixinConfigPath -Label "$version mixin config") -and (Test-Path -LiteralPath $mixinRoot -PathType Container)) {
        $mixinConfig = Get-Content -LiteralPath $mixinConfigPath -Raw | ConvertFrom-Json
        $registeredMixins = @($mixinConfig.mixins) + @($mixinConfig.client) + @($mixinConfig.server)
        $declaredMixins = Get-ChildItem -LiteralPath $mixinRoot -Recurse -Filter '*.java' -File |
            Where-Object { (Get-Content -LiteralPath $_.FullName -Raw).Contains('@Mixin') } |
            ForEach-Object {
                $_.FullName.Substring($mixinRoot.Length + 1, $_.FullName.Length - $mixinRoot.Length - 6).Replace('\', '.')
            }
        $missingMixins = @($declaredMixins | Where-Object { $_ -notin $registeredMixins })
        $staleMixins = @($registeredMixins | Where-Object { $_ -notin $declaredMixins })
        $duplicateMixins = @($registeredMixins | Group-Object | Where-Object { $_.Count -ne 1 } | ForEach-Object { $_.Name })
        if ($missingMixins.Count -gt 0) {
            Add-Failure "$version mixin classes are not registered: $($missingMixins -join ', ')"
        } elseif ($staleMixins.Count -gt 0) {
            Add-Failure "$version mixin config has missing classes: $($staleMixins -join ', ')"
        } elseif ($duplicateMixins.Count -gt 0) {
            Add-Failure "$version mixin classes are registered more than once: $($duplicateMixins -join ', ')"
        } else {
            Write-Host "PASS $version every mixin class is registered exactly once" -ForegroundColor Green
        }
    }
}

Assert-File -Path (Join-Path $RepoRoot 'artwork\window_controls.pdn') -Label 'canonical window-controls source artwork' | Out-Null
$bundledNullable = Get-ChildItem -LiteralPath (Join-Path $RepoRoot 'versions') -Recurse -Filter 'Nullable.java' -File -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match 'org[\\/]jspecify[\\/]annotations' }
if ($bundledNullable) {
    Add-Failure "bundled JSpecify annotation sources remain: $($bundledNullable.FullName -join ', ')"
} else {
    Write-Host 'PASS no bundled JSpecify annotation sources' -ForegroundColor Green
}

$nonFabricRanges = @{
    '1.20.1' = @{ Loader = 'forge'; LanguageRange = '[47,48)'; ModRange = '[47.4.0,48)'; File = 'mods.toml' }
    '1.21.1' = @{ Loader = 'neoforge'; LanguageRange = '[4.0.34,5)'; ModRange = '[21.1.95,21.2)'; File = 'neoforge.mods.toml' }
    '1.21.11' = @{ Loader = 'neoforge'; LanguageRange = '[10.0.36,11)'; ModRange = '[21.11.42,21.12)'; File = 'neoforge.mods.toml' }
    '26.1.2' = @{ Loader = 'neoforge'; LanguageRange = '[11.0.13,12)'; ModRange = '[26.1.2.59-beta,26.1.3)'; File = 'neoforge.mods.toml' }
    '26.2' = @{ Loader = 'neoforge'; LanguageRange = '[11.0.13,12)'; ModRange = '[26.2.0.7-beta,26.3)'; File = 'neoforge.mods.toml' }
}
foreach ($version in $versions) {
    $range = $nonFabricRanges[$version]
    $metadata = Join-Path $RepoRoot "versions\$version\$($range.Loader)\src\main\resources\META-INF\$($range.File)"
    Assert-Contains -Path $metadata -Text "loaderVersion = `"$($range.LanguageRange)`"" -Label "$version bounded javafml language-loader range"
    Assert-Contains -Path $metadata -Text "versionRange = `"$($range.ModRange)`"" -Label "$version bounded Forge/NeoForge mod range"
    Assert-Contains -Path $metadata -Text 'versionRange = "[${minecraft_version}]"' -Label "$version exact non-Fabric Minecraft range"
}

foreach ($entry in $loaderMatrix) {
    $version = $entry.Version
    $loader = $entry.Loader
    Write-Host "== $version $loader loader ==" -ForegroundColor Cyan

    $loaderClass = Join-Path $RepoRoot "versions\$version\$loader\src\main\java\com\salts_inventory_update\$($entry.LoaderClass)"
    $metadata = Join-Path $RepoRoot "versions\$version\$loader\src\main\resources\$($entry.Metadata)"

    Assert-Contains -Path $loaderClass -Text 'DesktopPackets.registerPayloadTypes();' -Label "$version $loader payload registration"
    Assert-Contains -Path $loaderClass -Text 'DesktopContainerSessions.initialize();' -Label "$version $loader server session init"

    if ($entry.MetadataKind -eq 'fabric') {
        $clientClass = Join-Path $RepoRoot "versions\$version\$loader\src\main\java\com\salts_inventory_update\$($entry.ClientClass)"
        Assert-Contains -Path $clientClass -Text 'WindowedInventoryClient.initialize();' -Label "$version $loader client init"
        Assert-Contains -Path $metadata -Text '"entrypoints"' -Label "$version $loader entrypoint metadata"
        Assert-Contains -Path $metadata -Text '"fabricloader": ">=${fabric_loader_version}"' -Label "$version $loader loader requirement expansion"
        Assert-Contains -Path $metadata -Text '"java": ">=${java_version}"' -Label "$version $loader Java requirement expansion"
        Assert-Contains -Path $metadata -Text 'SaltsInventoryUpdateFabric' -Label "$version $loader main entrypoint metadata"
        Assert-Contains -Path $metadata -Text 'SaltsInventoryUpdateFabricClient' -Label "$version $loader client entrypoint metadata"
        Assert-Contains -Path $metadata -Text '"mixins"' -Label "$version $loader mixin metadata block"
        Assert-Contains -Path $metadata -Text '${mod_id}.mixins.json' -Label "$version $loader mixin metadata config"
    } else {
        Assert-Contains -Path $metadata -Text '[[mixins]]' -Label "$version $loader mixin metadata block"
        Assert-Contains -Path $metadata -Text 'config = "${mod_id}.mixins.json"' -Label "$version $loader mixin metadata config"
        if ($loader -eq 'forge') {
            $manifest = Join-Path $RepoRoot "versions\$version\$loader\src\main\resources\META-INF\MANIFEST.MF"
            Assert-Contains -Path $manifest -Text 'MixinConfigs: salts_inventory_update.mixins.json' -Label "$version $loader mixin manifest config"
        }
    }
}

if ($failures.Count -gt 0) {
    Write-Host "Source feature parity failed with $($failures.Count) issue(s)." -ForegroundColor Red
    exit 1
}

Write-Host 'Source feature parity passed.' -ForegroundColor Green
