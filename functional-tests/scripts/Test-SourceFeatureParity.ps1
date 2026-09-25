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

function Assert-NotMatches {
    param(
        [string] $Path,
        [string] $Pattern,
        [string] $Label
    )

    if (-not (Assert-File -Path $Path -Label $Label)) {
        return
    }

    $content = Get-Content -LiteralPath $Path -Raw
    if ([regex]::IsMatch($content, $Pattern)) {
        Add-Failure "$Label unexpectedly matched pattern '$Pattern' in $Path"
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
    $craftingMenuMixin = Join-Path $fabricRoot 'mixin\server\CraftingMenuMixin.java'
    $serverPlayerMixin = Join-Path $fabricRoot 'mixin\server\ServerPlayerInventoryExpansionMixin.java'
    $deathMixin = if ($version -eq '1.21.1') {
        Join-Path $fabricRoot 'mixin\server\ServerPlayerMixin.java'
    } else {
        $serverPlayerMixin
    }
    $tomsPayloads = Join-Path $fabricRoot 'compat\toms_storage\TomsStoragePayloads.java'
    $language = Join-Path $RepoRoot "versions\$version\common\src\main\resources\assets\salts_inventory_update\lang\en_us.json"
    $mixinConfig = Join-Path $RepoRoot "versions\$version\common\src\main\resources\salts_inventory_update.mixins.json"

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
    Assert-Contains -Path $server -Text 'public static void syncCraftingResult' -Label "$version detached crafting result custom sync"
    Assert-Contains -Path $craftingMenuMixin -Text 'DesktopContainerSessions.syncCraftingResult' -Label "$version crafting result recalculation bridge"
    Assert-Contains -Path $mixinConfig -Text 'server.CraftingMenuMixin' -Label "$version crafting result mixin registration"
    Assert-Matches -Path $server -Pattern '(?s)clickMenu\(payload\.debugId\(\), player, sessions, session\.menu,.{0,500}sessions\.broadcastAll\(player\)' -Label "$version container clicks finalize all shared menus"
    if ($version -ne '1.20.1') {
        Assert-Contains -Path $craftingMenuMixin -Text 'method = "finishPlacingRecipe"' -Label "$version recipe placement result bridge"
    }
    Assert-Matches -Path $server -Pattern '(?s)sendCarriedChange\([^)]*ItemStack stack\).{0,1400}(?:this\.)?player\.inventoryMenu\.getCarried\(\)' -Label "$version detached synchronizer preserves canonical player cursor"
    Assert-Contains -Path $server -Text 'mayInteract(player, pos)' -Label "$version linked-source protection check"
    Assert-NotContains -Path $server -Text 'SaltsInventoryRuntime' -Label "$version server decisions do not use client-global config"
    Assert-NotContains -Path $server -Text 'DesktopJeiTransferRequirement' -Label "$version server ignores client ingredient authority"
    Assert-NotContains -Path $server -Text 'ThreadLocalRandom' -Label "$version server authorization tokens are not predictable PRNG output"
    Assert-NotContains -Path $containerClient -Text 'ThreadLocalRandom' -Label "$version client handshake nonce uses secure randomness"
    Assert-Matches -Path $containerClient -Pattern 'MODE_(?:RESEND|REFRESH)_INTERVAL_TICKS' -Label "$version dropped mode updates eventually converge"
    if ($version -eq '1.20.1') {
        Assert-Matches -Path $server -Pattern '(?s)syncCarried\([^)]*\).{0,600}syncPlayerMenuState\(player\)' -Label "$version carried sync acknowledges player menu state"
    }
    if ($version -eq '1.21.1') {
        Assert-Matches -Path $containerClient -Pattern '(?s)DesktopCarriedPayload\.TYPE.{0,800}CONNECTION\.authorizes\(payload\.connectionNonce\(\), 0L, false\).{0,300}payload\.playerSessionToken\(\) != playerSessionToken.{0,500}(?:setSharedCarried|inventoryMenu\.setCarried)\(payload\.carried\(\)' -Label "$version carried payload authenticates its player session before applying authoritative cursor state"
    }
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
Assert-Contains -Path $rootBuildScript -Text '"**/compat/rei/SaltsReiClientPlugin.java"' -Label 'non-Fabric loaders exclude the Fabric REI entrypoint'
Assert-NotContains -Path $rootBuildScript -Text '"**/compat/rei/**"' -Label 'non-Fabric loaders retain shared REI runtime support'
Assert-Contains -Path $rootBuildScript -Text '"**/compat/emi/SaltsEmiClientPlugin.java"' -Label 'non-Fabric loaders exclude the Fabric EMI entrypoint'
Assert-Contains -Path $rootBuildScript -Text 'val includeEmiRuntime' -Label 'EMI development runtime is opt-in'
Assert-Contains -Path $rootBuildScript -Text '"1.21.1" to "1.1.24+1.21.1"' -Label 'EMI dependency is pinned for 1.21.1'
Assert-NotContains -Path $rootBuildScript -Text '"--mixin.config", "salts_inventory_update.mixins.json"' -Label 'development runs do not duplicate the loader-supplied mixin config'
Assert-Contains -Path $settingsScript -Text 'id("dev.prism.settings") version "0.5.17"' -Label 'Prism settings plugin version is reproducibly pinned'

$sophisticatedTargets = @(
    @{ Version = '1.20.1'; Loader = 'forge'; Metadata = 'mods.toml'; OptionalMarker = 'mandatory = false' },
    @{ Version = '1.21.1'; Loader = 'neoforge'; Metadata = 'neoforge.mods.toml'; OptionalMarker = 'type = "optional"' },
    @{ Version = '1.21.11'; Loader = 'neoforge'; Metadata = 'neoforge.mods.toml'; OptionalMarker = 'type = "optional"' },
    @{ Version = '26.1.2'; Loader = 'neoforge'; Metadata = 'neoforge.mods.toml'; OptionalMarker = 'type = "optional"' },
    @{ Version = '26.2'; Loader = 'neoforge'; Metadata = 'neoforge.mods.toml'; OptionalMarker = 'type = "optional"' }
)

foreach ($target in $sophisticatedTargets) {
    $version = $target.Version
    $loader = $target.Loader
    $label = "$version $loader"
    $nonFabricRoot = Join-Path $RepoRoot "versions\$version\$loader"
    $fabricRoot = Join-Path $RepoRoot "versions\$version\fabric"
    $mixinConfigPath = Join-Path $nonFabricRoot 'src\main\resources\salts_inventory_update.sophisticated.mixins.json'
    $metadataPath = Join-Path $nonFabricRoot "src\main\resources\META-INF\$($target.Metadata)"
    $fabricMetadataPath = Join-Path $fabricRoot 'src\main\resources\fabric.mod.json'
    $compatRoot = Join-Path $nonFabricRoot 'src\main\java\com\salts_inventory_update\compat\sophisticated'
    $mixinRoot = Join-Path $compatRoot 'mixin'
    $fabricCompatRoot = Join-Path $fabricRoot 'src\main\java\com\salts_inventory_update\compat\sophisticated'

    Assert-File -Path $mixinConfigPath -Label "$label Sophisticated mixin config" | Out-Null
    Assert-File -Path (Join-Path $compatRoot 'SophisticatedCompatBootstrap.java') -Label "$label guarded Sophisticated bootstrap" | Out-Null
    Assert-File -Path (Join-Path $compatRoot 'client\SophisticatedHostedScreenDefinition.java') -Label "$label Salt-native Sophisticated host" | Out-Null
    if ($version -eq '1.20.1') {
        $legacyOpenersMixin = Join-Path $mixinRoot 'SophisticatedStorageOpenersMixin.java'
        Assert-Matches -Path $legacyOpenersMixin -Pattern '(?s)@ModifyArg\(.{0,300}method = "getOpenCount".{0,700}Level;getEntities.{0,500}index = 2.{0,1000}nativeOwnership\.test\(player\).{0,500}hasOpenSessionMatching' -Label '1.20.1 Forge augments the legacy opener predicate instead of redirecting its invokedynamic method reference'
    }
    Assert-Contains -Path $metadataPath -Text 'config = "${mod_id}.sophisticated.mixins.json"' -Label "$label registers its Sophisticated mixin config"
    foreach ($modId in @('sophisticatedcore', 'sophisticatedbackpacks', 'sophisticatedstorage')) {
        Assert-Matches -Path $metadataPath -Pattern "(?s)modId = `"$modId`".{0,100}$([regex]::Escape($target.OptionalMarker))" -Label "$label keeps $modId optional"
    }
    Assert-NotContains -Path $fabricMetadataPath -Text 'sophisticated' -Label "$version Fabric metadata remains free of Sophisticated integration"
    if (Test-Path -LiteralPath $fabricCompatRoot) {
        Add-Failure "$version Fabric unexpectedly contains a Sophisticated compatibility source tree: $fabricCompatRoot"
    } else {
        Write-Host "PASS $version Fabric contains no Sophisticated compatibility source tree" -ForegroundColor Green
    }

    if ((Test-Path -LiteralPath $mixinConfigPath -PathType Leaf) -and (Test-Path -LiteralPath $mixinRoot -PathType Container)) {
        $targetMixinConfig = Get-Content -LiteralPath $mixinConfigPath -Raw | ConvertFrom-Json
        $registered = @($targetMixinConfig.mixins) + @($targetMixinConfig.client) + @($targetMixinConfig.server)
        $declared = Get-ChildItem -LiteralPath $mixinRoot -Recurse -Filter '*.java' -File |
            Where-Object { (Get-Content -LiteralPath $_.FullName -Raw).Contains('@Mixin') } |
            ForEach-Object {
                $_.FullName.Substring($mixinRoot.Length + 1, $_.FullName.Length - $mixinRoot.Length - 6).Replace('\', '.')
            }
        $missing = @($declared | Where-Object { $_ -notin $registered })
        $stale = @($registered | Where-Object { $_ -notin $declared })
        $duplicates = @($registered | Group-Object | Where-Object { $_.Count -ne 1 } | ForEach-Object { $_.Name })
        if ($missing.Count -gt 0) {
            Add-Failure "$label Sophisticated mixin classes are not registered: $($missing -join ', ')"
        } elseif ($stale.Count -gt 0) {
            Add-Failure "$label Sophisticated mixin config has missing classes: $($stale -join ', ')"
        } elseif ($duplicates.Count -gt 0) {
            Add-Failure "$label Sophisticated mixin classes are registered more than once: $($duplicates -join ', ')"
        } else {
            Write-Host "PASS $label every Sophisticated mixin class is registered exactly once" -ForegroundColor Green
        }
    }
}

$sophisticatedNeoForgeRoot = Join-Path $RepoRoot 'versions\26.2\neoforge'
$sophisticatedFabricRoot = Join-Path $RepoRoot 'versions\26.2\fabric'
$sophisticatedMixinConfig = Join-Path $sophisticatedNeoForgeRoot 'src\main\resources\salts_inventory_update.sophisticated.mixins.json'
$sophisticatedNeoForgeMetadata = Join-Path $sophisticatedNeoForgeRoot 'src\main\resources\META-INF\neoforge.mods.toml'
$sophisticatedFabricMetadata = Join-Path $sophisticatedFabricRoot 'src\main\resources\fabric.mod.json'
$sophisticatedHostedDefinition = Join-Path $sophisticatedNeoForgeRoot 'src\main\java\com\salts_inventory_update\compat\sophisticated\client\SophisticatedHostedScreenDefinition.java'
$sophisticatedMixinRoot = Join-Path $sophisticatedNeoForgeRoot 'src\main\java\com\salts_inventory_update\compat\sophisticated\mixin'
$sophisticatedCraftingTabMixin = Join-Path $sophisticatedMixinRoot 'CraftingUpgradeTabMixin.java'
$sophisticatedBlockConverterMixin = Join-Path $sophisticatedMixinRoot 'BlockConverterRecipeControlMixin.java'
$sophisticatedSearchBoxMixin = Join-Path $sophisticatedMixinRoot 'SearchBoxMixin.java'
$sophisticatedGuiHelperTabBackgroundMixin = Join-Path $sophisticatedMixinRoot 'GuiHelperTabBackgroundMixin.java'
$sophisticatedBackpackContextAccessor = Join-Path $sophisticatedMixinRoot 'BackpackContextItemAccessor.java'
$sophisticatedServerItemLockMixin = Join-Path $sophisticatedMixinRoot 'ServerGamePacketListenerItemLockMixin.java'
$sophisticatedStorageOpenersMixin = Join-Path $sophisticatedMixinRoot 'SophisticatedStorageOpenersMixin.java'
$sophisticatedMixinPlugin = Join-Path $sophisticatedMixinRoot 'SophisticatedMixinPlugin.java'
$sophisticatedBackpackItemLocks = Join-Path $sophisticatedNeoForgeRoot 'src\main\java\com\salts_inventory_update\compat\sophisticated\SophisticatedBackpackItemLocks.java'
$sophisticatedBackpacksCompat = Join-Path $sophisticatedNeoForgeRoot 'src\main\java\com\salts_inventory_update\compat\sophisticated\SophisticatedBackpacksCompat.java'
$desktopItemSourceLocks = Join-Path $sophisticatedFabricRoot 'src\main\java\com\salts_inventory_update\internal\desktop\DesktopItemSourceLocks.java'
$desktopContainerClient = Join-Path $sophisticatedFabricRoot 'src\main\java\com\salts_inventory_update\client\DesktopContainerClient.java'
$desktopContainerSession = Join-Path $sophisticatedFabricRoot 'src\main\java\com\salts_inventory_update\client\DesktopContainerSession.java'
$desktopInventoryScreen = Join-Path $sophisticatedFabricRoot 'src\main\java\com\salts_inventory_update\client\InventoryDesktopScreen.java'
$internalDesktopWindowDefinition = Join-Path $sophisticatedFabricRoot 'src\main\java\com\salts_inventory_update\client\InternalDesktopWindowDefinition.java'
$internalDesktopWindowRegion = Join-Path $sophisticatedFabricRoot 'src\main\java\com\salts_inventory_update\client\InternalDesktopWindowRegion.java'
$desktopMenuSlots = Join-Path $sophisticatedFabricRoot 'src\main\java\com\salts_inventory_update\internal\desktop\DesktopMenuSlots.java'
$desktopContainerSessions = Join-Path $sophisticatedFabricRoot 'src\main\java\com\salts_inventory_update\server\DesktopContainerSessions.java'
$legacyDesktopContainerSessions = Join-Path $RepoRoot 'versions\1.20.1\fabric\src\main\java\com\salts_inventory_update\server\DesktopContainerSessions.java'
$desktopPackets = Join-Path $sophisticatedFabricRoot 'src\main\java\com\salts_inventory_update\network\DesktopPackets.java'
$sophisticatedCoreCompat = Join-Path $sophisticatedNeoForgeRoot 'src\main\java\com\salts_inventory_update\compat\sophisticated\SophisticatedCoreCompat.java'
Assert-File -Path $sophisticatedMixinConfig -Label '26.2 NeoForge Sophisticated mixin config' | Out-Null
Assert-Contains -Path $sophisticatedNeoForgeMetadata -Text 'config = "${mod_id}.sophisticated.mixins.json"' -Label '26.2 NeoForge registers its Sophisticated mixin config'
Assert-NotContains -Path $sophisticatedFabricMetadata -Text 'sophisticated' -Label '26.2 Fabric metadata remains free of Sophisticated integration'
Assert-Matches -Path $rootBuildScript -Pattern '(?s)if \(minecraftVersion != null && name in setOf\("forge", "neoforge"\)\).{0,1800}compileOnly' -Label 'Sophisticated dependencies are scoped to every Forge and NeoForge target'
Assert-Matches -Path $rootBuildScript -Pattern '(?s)name == "forge" && minecraftVersion == "1\.20\.1".{0,1800}sophisticatedForgeRuntimeNamed.{0,900}attribute\(mappingsAttribute, namedMappings\).{0,1200}classpath \+= files\(sophisticatedForgeRuntimeNamed\)' -Label '1.20.1 Forge development runtime remaps Sophisticated production jars to named mappings'
Assert-Matches -Path $rootBuildScript -Pattern '(?s)runTask\.workingDir.{0,200}resolve\("mods"\).{0,500}file\.name\.startsWith\("jei-"\).{0,600}project\.delete\(staleJeiJars\).{0,500}managed by -PincludeJeiRuntime' -Label 'development runs remove stale JEI jars before applying the managed JEI runtime option'
Assert-Matches -Path $rootBuildScript -Pattern '(?s)neoForgeJeiVersions = mapOf\(.{0,500}"26\.2" to "30\.7\.0\.39".{0,500}"1\.21\.11" to "27\.13\.0\.43".{0,500}"1\.21\.1" to "19\.32\.0\.359"' -Label 'NeoForge JEI pins satisfy the selected Sophisticated Core versions'
Assert-Matches -Path $rootBuildScript -Pattern '(?s)val jeiVersion = if \(name == "neoforge"\).{0,250}neoForgeJeiVersions\[minecraftVersion\] \?: jeiVersions\[minecraftVersion\].{0,150}else \{\s*jeiVersions\[minecraftVersion\]' -Label 'Fabric retains its independent JEI version pins'
Assert-Matches -Path $legacyDesktopContainerSessions -Pattern '(?s)SessionSynchronizer synchronizer = new SessionSynchronizer\(player, session\).{0,400}DesktopSynchronizerOverride\.run\(\s*synchronizer,.{0,250}session\.menu\.setSynchronizer\(synchronizer\)' -Label '1.20.1 preserves the detached Salt synchronizer while Sophisticated initializes high-stack synchronization'
Assert-Contains -Path $legacyDesktopContainerSessions -Text 'SessionSynchronizer implements ContainerSynchronizer, DesktopSessionSynchronizer' -Label '1.20.1 detached synchronizer carries the preservation marker consumed by Sophisticated mixins'
Assert-Matches -Path $rootBuildScript -Pattern '(?s)minecraftVersion in setOf\("1\.20\.1", "1\.21\.1", "1\.21\.11", "26\.1\.2", "26\.2"\).{0,300}loader in setOf\("forge", "neoforge"\).{0,500}sophisticatedMixin !in entries' -Label 'release verification requires the Sophisticated mixin on every Forge and NeoForge target'
Assert-NotMatches -Path $sophisticatedHostedDefinition -Pattern '(?m)^\s*(?!//).*\.\s*extract(?:Background|Contents)\s*\(' -Label 'Sophisticated host does not invoke whole-screen rendering'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)translateDetachedTabChildren\(composite, dx, dy\).{0,1800}attachedChildren\.contains\(child\).{0,400}child\.setPosition\(new Position\(child\.getX\(\) \+ dx, child\.getY\(\) \+ dy\)\)' -Label 'Sophisticated closed-tab controls follow hosted window movement exactly once'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)tab instanceof SophisticatedHostedPositionAccess positionAccess.{0,300}positionAccess\.salts_inventory_update\$translateHostedPosition\(dx, dy\).{0,700}child instanceof SophisticatedHostedPositionAccess positionAccess.{0,300}positionAccess\.salts_inventory_update\$translateHostedPosition\(dx, dy\)' -Label 'Sophisticated cached tab geometry receives each nonzero hosted movement delta'
Assert-Matches -Path $sophisticatedCraftingTabMixin -Pattern '(?s)resultListPositionDimensions = Pair\.of\(.{0,300}topLeft\.x\(\) \+ dx, topLeft\.y\(\) \+ dy.{0,500}resultChoicePositions\.set\(index, new Position\(choice\.x\(\) \+ dx, choice\.y\(\) \+ dy\)\)' -Label 'Sophisticated crafting result-choice geometry follows its hosted tab'
Assert-Matches -Path $sophisticatedBlockConverterMixin -Pattern 'browseButton\.setPosition\(new Position\(browseButton\.getX\(\) \+ dx, browseButton\.getY\(\) \+ dy\)\)' -Label 'Sophisticated block-converter browse button follows its hosted control'
Assert-Matches -Path $sophisticatedSearchBoxMixin -Pattern '(?s)@Invoker\("isExpandedOrFocused"\).{0,300}salts_inventory_update\$isExpandedOrFocused\(\).{0,1200}@ModifyArg\(.{0,900}new Position\(this\.salts_inventory_update\$hostedX, nativePosition\.y\(\)\)' -Label 'Sophisticated hosted search uses native expansion state while expanding right from Salt content'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)WidgetBase\[\] controls = \{\s*components\.sortMode\(\),\s*components\.sort\(\),\s*components\.transferToStorage\(\),\s*components\.transferToInventory\(\).{0,700}widget\.setVisible\(!searchExpanded\).{0,300}setWidgetPosition\(state, widget, HIDDEN_SLOT, HIDDEN_SLOT\)' -Label 'Sophisticated standard toolbar is compact and hides non-search controls for the full native search expansion'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)gridY = standardToolbarY\(context\) \+ TOOLBAR_HEIGHT \+ TOOLBAR_GAP.{0,300}context\.contentHeight\(\) \+ STANDARD_HEADER_RAISE' -Label 'Sophisticated standard toolbar and grid consume Salt top padding'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)standardToolbarY\(DesktopWindowContext<\?, \?> context\).{0,200}context\.contentY\(\) - STANDARD_HEADER_RAISE.{0,300}standardContentHeight\(int rows\).{0,200}rows \* SLOT - STANDARD_HEADER_RAISE' -Label 'Sophisticated standard snap sizing preserves row counts after raising the body'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)visibleRows = defaultVisibleRows\(context, rows\).{0,300}standardContentWidth\(columns, rows > visibleRows, inventoryControlWidth\)' -Label 'Sophisticated standard windows initially expose every row that fits on the desktop'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)int storageSlotCount = context\.menu\(\) instanceof StorageContainerMenuBase<\?> storage\s*\? allStorageSlots\(context\.state\(\), storage\)\.size\(\).{0,900}int rows = Math\.min\(requestedRows, Math\.max\(MIN_VISIBLE_ROWS, totalRows\)\).{0,250}standardContentWidth\(columns, scrolling, inventoryControlWidth\)' -Label 'Sophisticated resize snapping uses the unfiltered inventory, removes unused rows, and reserves width only for an active scrollbar'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '\(scrolling \? SCROLLBAR_RESERVE : 0\)' -Label 'Sophisticated standard width has no permanent empty scrollbar gutter'
Assert-Contains -Path $sophisticatedHostedDefinition -Text 'private static final int STANDARD_MIN_CONTENT_WIDTH = 9 * SLOT;' -Label 'Sophisticated compact standard width is the exact nine-column hosted grid'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)legacyScrollbarGutterMigrationPending.{0,1100}obsoleteScrollbarGutter = unusedWidth == SCROLLBAR_RESERVE.{0,250}duplicatedNativeMargins = occupiedContentWidth == STANDARD_MIN_CONTENT_WIDTH.{0,400}context\.windowWidth\(\) - unusedWidth' -Label 'Sophisticated existing compact windows discard obsolete scrollbar and doubled-native-margin width only'
Assert-Contains -Path $sophisticatedHostedDefinition -Text 'return saltSize(columns * SLOT, rows * SLOT);' -Label 'Sophisticated regular settings defaults fit their exact grid'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)this\.kind == LayoutKind\.LIMITED_BARREL_SETTINGS.{0,180}return saltSize\(MIN_CONTENT_WIDTH, 92\)' -Label 'Sophisticated limited settings defaults retain specialized space'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)kind == LayoutKind\.LIMITED_BARREL_SETTINGS.{0,150}contentHeight = 92.{0,250}kind == LayoutKind\.SETTINGS.{0,450}contentWidth = Math\.max\(1, settings\.getSlotsOnLine\(\)\) \* SLOT;\s*contentHeight = rows \* SLOT' -Label 'Sophisticated fixed settings size removes native side margins and template footer without clipping limited barrels'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)salts_inventory_update\$setPosition\(\s*itemX - state\.screen\.getLeftPos\(\),\s*itemY - state\.screen\.getTopPos\(\).{0,700}new Rect2i\(itemX - 1, itemY - 1, SLOT, SLOT\)' -Label 'Sophisticated hosted slots use one canonical item origin and an 18-pixel frame offset by minus one'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern 'slotHighlight\(placed\.itemX, placed\.itemY\)' -Label 'Sophisticated slot highlights use the canonical item origin'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern 'DesktopSlotHit\.of\(slotId, placed\.itemX, placed\.itemY\)' -Label 'Sophisticated desktop slot hits expose the canonical item origin'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern 'slotBackground\(placed\.itemX, placed\.itemY\)' -Label 'Sophisticated Salt slot backgrounds use the canonical item origin'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)state\.screen\.getLeftPos\(\) \+ slot\.x \+ sideMoveDx,\s*state\.screen\.getTopPos\(\) \+ slot\.y \+ sideMoveDy,\s*SlotGroup\.PANEL' -Label 'Sophisticated panel slots retain the native item origin while following their hosted tab'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern 'nativeGridX = state\.screen\.getLeftPos\(\) \+ NATIVE_STORAGE_X \+ 1' -Label 'Sophisticated fallback slots translate from the native item origin rather than its background origin'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)PlacedSlot placed = interactiveSlotAt\(state, layout, event\.x\(\), event\.y\(\)\);\s*if \(placed != null\s*&& !this\.kind\.settings\(\)\s*&& !replacesSlot\(state\.access, placed\.slot\)\) \{.{0,150}return false;' -Label 'Sophisticated mapped panel slots fall through to authenticated Salt click and quick-craft handling'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)private static @Nullable PlacedSlot interactiveSlotAt\(.{0,500}components\.modal\(\) != null.{0,500}!tabs\.slotIsNotCoveredAt\(placed\.slot, mouseX, mouseY\)' -Label 'Sophisticated native overlays retain input ownership over hosted panel slots'
Assert-NotContains -Path $sophisticatedHostedDefinition -Text 'placed.group != SlotGroup.PANEL' -Label 'Sophisticated panel slots are not intercepted by native screen pointer capture'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)renderBehindFrame\(DesktopRenderContext<\?, \?> context\).{0,1000}upgradeRailFrame\(.{0,500}context\.windowNineSlice\(\s*SALT_WINDOW_TEXTURE' -Label 'Sophisticated upgrade rail reuses the Salt window texture below the main frame'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)rail\.translateY\(original\.itemY\).{0,7000}itemY = railY \+ 1 \+ index \* \(SLOT \+ UPGRADE_RAIL_SLOT_GAP\).{0,200}new RailRowTranslation\(original\.itemY, itemY - original\.itemY\)' -Label 'Sophisticated upgrade slots and enable switches share the spaced rail-row translation'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)upgradeRailFrame\(context, placed, components\.upgradeSwitches\(\)\).{0,200}result\.add\(leftRail\).{0,3000}frameRight = context\.windowX\(\) \+ UPGRADE_RAIL_FRAME_OVERLAP' -Label 'Sophisticated upgrade rail shell tucks beneath the main frame and participates in external-region clamping'
Assert-Contains -Path $sophisticatedHostedDefinition -Text 'context.windowX() - template.getWidth(),' -Label 'Sophisticated native template controls move to the left of the Salt window'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)settingsTemplateRailFrame\(.{0,1200}frameRight = context\.windowX\(\) \+ UPGRADE_RAIL_FRAME_OVERLAP' -Label 'Sophisticated settings template shell tucks beneath the main frame'
Assert-Contains -Path $sophisticatedHostedDefinition -Text 'visual.add(leftRailFrame);' -Label 'Sophisticated settings rail participates in hosted visual coverage'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)renderBehindFrame\(DesktopRenderContext<\?, \?> context\).{0,1800}for \(Rect2i rightTabFrame : rightTabFrames\(context, sideTabs\)\).{0,500}context\.windowNineSlice\(\s*SALT_WINDOW_TEXTURE.{0,700}withSaltTabBackgrounds\(\(\) ->\s*sideTabs\.extractRenderState' -Label 'Sophisticated right tabs render on individual Salt frames below the main frame'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)rightTabFrames\(.{0,1500}tabs\.children\(\).{0,500}isVisibleBesideOpenTab\(tab, openTab\).{0,500}openTab\.getTabRectangle\(\).{0,200}frames::add' -Label 'Sophisticated right tabs receive one visible frame each with the expanded tab painted last'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)rightTabFrame\(DesktopWindowContext<\?, \?> context, Rect2i tab\).{0,500}frameX = context\.windowX\(\) \+ context\.windowWidth\(\) - UPGRADE_RAIL_FRAME_OVERLAP.{0,300}frameRight = tab\.getX\(\) \+ tab\.getWidth\(\)' -Label 'Each Sophisticated right tab frame fits its native icon width and tucks beneath the main window'
Assert-Count -Path $sophisticatedHostedDefinition -Text 'rightTabFrames(context,' -Expected 3 -Label 'Sophisticated individual right tab frames participate in rendering, coverage, and clamping'
Assert-Matches -Path $sophisticatedGuiHelperTabBackgroundMixin -Pattern '(?s)@Mixin\(value = GuiHelper\.class, remap = false\).{0,600}@Inject\(method = "renderTabBackground".{0,500}useSaltTabBackgrounds\(\).{0,100}ci\.cancel\(\)' -Label 'Sophisticated native tab plates are replaced only during Salt-hosted tab rendering'
Assert-NotContains -Path $sophisticatedHostedDefinition -Text 'setupSizeWithClosedTabs' -Label 'Sophisticated setup sizing does not predict disabled settings tabs from menu containers'
Assert-NotContains -Path $sophisticatedHostedDefinition -Text 'closedSideTabCount' -Label 'Sophisticated tab height is derived only from initialized visible tab children'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)minSize\(DesktopWindowContext<T, State> context\).{0,500}sizeWithClosedTabs\(context, context\.state\(\), base\)' -Label 'Sophisticated resize minimum applies the closed side-tab height floor'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)sizeWithClosedTabs\(.{0,900}tabs\.children\(\).{0,500}tabTop = \(state\.kind == LayoutKind\.STANDARD \? standardToolbarY\(context\) : context\.contentY\(\)\)\s*- context\.windowY\(\).{0,300}\(tabCount - 1\) \* RIGHT_TAB_STRIDE \+ Tab\.DEFAULT_HEIGHT' -Label 'Sophisticated closed-tab height uses actual hosted content offsets in main and settings windows'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)desiredSizeWithClosedTabs\(.{0,800}adjusted\.height\(\) > context\.windowHeight\(\)' -Label 'Sophisticated undersized restored windows grow to their closed tab rail without following open panels'
Assert-Contains -Path $sophisticatedHostedDefinition -Text 'InternalDesktopWindowRegion.transientVisualAndInteractive(' -Label 'Sophisticated transient template fields remain interactive without constraining the window'
Assert-Contains -Path $sophisticatedHostedDefinition -Text 'state.nativePointerOwner = consumed ? templateTarget : null;' -Label 'Sophisticated detached template controls capture their pointer owner'
Assert-Contains -Path $sophisticatedHostedDefinition -Text 'pointerOwner.mouseDragged(event, dx, dy)' -Label 'Sophisticated detached template controls retain pointer ownership while dragging'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)updateTooltipState\(.{0,1000}!active \|\| layout == null \|\| !layout\.containsVisual\(mouseX, mouseY\).{0,150}dismissTemplateInputs\(state\)' -Label 'Sophisticated template fields release focus whenever their native tooltip pass is inactive'
Assert-Matches -Path $sophisticatedHostedDefinition -Pattern '(?s)dismissTemplateInputs\(State state\).{0,900}textBox\.setVisible\(false\).{0,150}textBox\.setFocused\(false\).{0,700}state\.screen\.setFocused\(null\)' -Label 'Sophisticated transient template inputs are hidden and unfocused together'
Assert-Contains -Path $internalDesktopWindowDefinition -Text 'default void updateTooltipState(' -Label '26.2 internal hosted definitions receive tooltip activity state'
Assert-Contains -Path $internalDesktopWindowRegion -Text 'boolean constrainsWindow' -Label '26.2 internal external regions distinguish stable and transient footprints'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)boolean tooltipEligible = this\.sharedCarried\.isEmpty\(\);.{0,300}apiUpdateTooltipState\(window, mouseX, mouseY, tooltipEligible && window == hoveredWindow\);.{0,300}extractWindowControlTooltip' -Label '26.2 desktop clears skipped hosted tooltip state before other tooltip early returns'
Assert-Count -Path $desktopInventoryScreen -Text '!region.constrainsWindow()' -Expected 2 -Label '26.2 transient regions do not affect placement or visible clamping bounds'
Assert-Contains -Path $desktopInventoryScreen -Text '&& region.constrainsWindow()' -Label '26.2 desktop clamps only stable hosted external regions'
Assert-Matches -Path $desktopContainerSession -Pattern '(?s)DesktopMenuSlots\.prepareSnapshot\(menu, items\);.{0,1500}menu\.initializeContents\(payload\.stateId\(\), items, payload\.carried\(\)\);.{0,500}DesktopMenuSlots\.size\(menu\).{0,700}snapshot mismatch after initialization' -Label '26.2 desktop sessions revalidate logical slot count after menu initialization'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)boolean apiInitialized = this\.initializeApiWindow\(window, apiDefinition, apiSetup\);.{0,1200}!apiInitialized.{0,900}window\.apiDefinition = null;.{0,500}window\.width = Math\.min\(defaultWindowWidth.{0,900}this\.sessions\.add\(session\);.{0,1200}this\.sessions\.remove\(transitionReplacedSession\);' -Label '26.2 hosted session transitions fall back to the reconstructed incoming menu before committing old-window removal'
Assert-Contains -Path $desktopPackets -Text 'clientboundPlay().register(DesktopMutationAckPayload.TYPE, DesktopMutationAckPayload.CODEC);' -Label '26.2 registers the clientbound mutation acknowledgement'
Assert-Matches -Path $desktopPackets -Pattern '(?s)record DesktopMutationAckPayload\(.{0,300}long connectionNonce,.{0,200}long playerSessionToken,.{0,200}long mutationId,.{0,200}boolean sequenceAccepted' -Label '26.2 mutation acknowledgements carry connection, player-token, and sequence authentication'
Assert-Count -Path $desktopPackets -Text 'long mutationId' -Expected 15 -Label '26.2 mutation id is carried by all fourteen mutators and their acknowledgement'
$serializedMutationPayloads = @(
    'InventorySlotPurchasePayload',
    'DesktopClickPayload',
    'DesktopDragSlotsPayload',
    'DesktopPickupAllPayload',
    'DesktopQuickMoveAllPayload',
    'DesktopBundleSelectPayload',
    'DesktopQuickMovePayload',
    'DesktopButtonPayload',
    'DesktopPlaceRecipePayload',
    'DesktopJeiTransferPayload',
    'DesktopRenamePayload',
    'DesktopCustomPayload',
    'DesktopCarriedPayload'
)
foreach ($mutationPayload in $serializedMutationPayloads) {
    $escapedPayload = [regex]::Escape($mutationPayload)
    Assert-Matches -Path $desktopPackets -Pattern "(?s)record $escapedPayload\(.{0,160}long mutationId,.{0,160}long playerSessionToken" -Label "26.2 $mutationPayload binds its mutation id to the player-session epoch"
}
Assert-Matches -Path $desktopContainerClient -Pattern '(?s)private static boolean queueMutation\(.{0,1500}inFlightMutation != null.{0,1800}queued\.factory\(\)\.create\(mutationId\).{0,1500}nextMutationId = mutationId' -Label '26.2 client serializes shared-cursor mutations with dispatch-time packet construction'
Assert-Matches -Path $desktopContainerClient -Pattern '(?s)queueMutation\("click".{0,500}SessionAuth auth = sessionAuth\(sessionId\);.{0,250}ItemStack carried = currentClientCarried\(\);.{0,500}auth\.stateId\(\)' -Label '26.2 queued clicks resolve current authentication, state, and carried stack at dispatch'
Assert-Count -Path $desktopContainerClient -Text 'queueMutation("' -Expected 14 -Label '26.2 routes all item and menu mutations through one global client lane'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private QuickMoveTarget quickMoveTarget\(.{0,900}this\.sessions\.contains\(focusedWindow\.session\).{0,250}!focusedWindow\.minimized.{0,150}!focusedWindow\.ghosted.{0,150}!focusedWindow\.persistentHidden' -Label '26.2 cross-window quick moves target only registered visible focused sessions'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private void applyDragDistribution\((?:(?!\r?\n    private).)*List<DesktopContainerClient\.SlotTarget> targets = slots\.stream\(\)(?:(?!\r?\n    private).)*DesktopContainerClient\.dragSlots\(drag\.quickCraftType\(\), targets\)' -Label '26.2 non-legacy mixed-window drags use one authenticated cross-session batch'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private @Nullable DragDistribution activeDragPreview\((?:(?!\r?\n    private).)*this\.dragDistribution\.size\(\) == 0' -Label '26.2 drag preview is visible from the first accepted slot'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)public void texturelessSlot\((?:(?!\r?\n        @Override).)*DragSlotPreview dragPreview = InventoryDesktopScreen\.this\.dragSlotPreview\(slot\)(?:(?!\r?\n        @Override).)*if \(dragPreview != null\)(?:(?!\r?\n        @Override).)*renderItemStack\(' -Label 'Sophisticated textureless slots render live drag previews before acknowledgement'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)PendingPickupAll delayedPickupAll = this\.pendingPickupAll;.{0,1000}this\.pendingPickupAll = delayedPickupAll;\s*this\.dispatchPendingPickupAll\("carried-sync"\);' -Label '26.2 double-click collection waits for the first click carried synchronization'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private List<Integer> visiblePickupAllSessions\((?:(?!\r?\n    private).)*sourceSessions\.add\(window\.session\.sessionId\(\)\)(?:(?!\r?\n    private).)*sourceSessions\.add\(DesktopPackets\.PLAYER_MENU_SESSION\)' -Label '26.2 pickup-all includes visible detached containers and the player inventory'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private boolean handleBundleScroll\(.{0,700}DesktopItemSourceLocks\.isSlotLocked.{0,500}return true;.{0,300}BundleItem\.getNumberOfItemsToShow' -Label '26.2 locked Backpack bundles consume wheel input without changing selection'
Assert-Matches -Path $desktopContainerClient -Pattern '(?s)public static boolean selectBundleItem\(.{0,900}!canDispatchOptimisticMutationNow\(\).{0,500}boolean queued = queueMutation\("bundle-select".{0,1000}return immediateMutationDispatched\("bundle-select", queued\);' -Label '26.2 bundle selection reports only an immediately dispatched optimistic mutation'
Assert-Matches -Path $desktopContainerClient -Pattern '(?s)public static boolean canAcceptMutationIntent\(\).{0,300}!mutationQueueBlocked.{0,300}MUTATION_QUEUE\.size\(\) < MAX_QUEUED_MUTATIONS' -Label '26.2 optimistic menu actions share the bounded mutation-intent admission gate'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private void selectBundleItem\(.{0,900}if \(!sent\).{0,700}else \{.{0,300}BundleItem\.toggleSelectedItem' -Label '26.2 optimistic bundle selection occurs only after its mutation intent is accepted'
Assert-Matches -Path $desktopContainerClient -Pattern '(?s)acceptMutationAck\(DesktopMutationAckPayload payload\).{0,500}CAP_MULTI_MENU_GESTURES.{0,300}payload\.playerSessionToken\(\) != playerSessionToken.{0,1200}payload\.mutationId\(\) != inFlight\.mutationId\(\).{0,900}dispatchNextMutation\(\)' -Label '26.2 client validates mutation acknowledgement nonce, token, id, and ordering'
Assert-Matches -Path $desktopContainerClient -Pattern '(?s)MUTATION_TIMEOUT_NANOS.{0,40000}markTimedOut\(\);.{0,200}discardPendingMutations\("ack-timeout"\);.{0,200}mutationQueueBlocked = true' -Label '26.2 mutation timeout fails closed while allowing a late acknowledgement to recover'
Assert-Count -Path $desktopContainerSessions -Text 'context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId()' -Expected 14 -Label '26.2 wraps all fourteen item and menu mutation receivers in the serialized server lane'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void handleMutation\(.{0,2200}authorizesMutationConnection\(connectionNonce, playerSessionToken\).{0,700}acceptMutation\(mutationId\).{0,3000}finally \{.{0,300}finishMutation' -Label '26.2 server authenticates the player epoch and monotonically orders every queued mutation'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void finishMutation\(.{0,700}new DesktopMutationAckPayload\(.{0,300}sequenceAccepted' -Label '26.2 server acknowledges every authenticated sequence after handler-owned synchronization'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)sequence rejected player=.{0,700}allowResync\(player, operation \+ "-sequence"\).{0,200}resyncGesture\(player, sessions\)' -Label '26.2 rejected sequences use the strict resync limiter before a full-session snapshot'
Assert-NotMatches -Path $desktopContainerSessions -Pattern '(?s)private static void finishMutation\(.{0,500}broadcastGesture\(' -Label '26.2 mutation acknowledgements do not add an unconditional full-session broadcast'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void selectBundleItem\(.{0,1800}runGestureMutation\(.{0,700}slot\.setChanged\(\);' -Label '26.2 successful bundle selection persists its selected item before releasing its mutation lane'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void selectBundleItem\((?:(?!\r?\n    private static).)*broadcastGesture\(' -Label '26.2 bundle selection broadcasts its authoritative visual state before acknowledgement'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)replacement\.playerSessionToken = nextToken\(\);.{0,250}replacement\.lastMutationId = 0L;.{0,500}new DesktopPlayerSessionPayload' -Label '26.2 player-token rotation starts a matching mutation sequence epoch'
Assert-Contains -Path $desktopMenuSlots -Text 'default boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu, Slot slot)' -Label '26.2 menu adapters can reserve special slots for native quick moves'
Assert-Matches -Path $sophisticatedCoreCompat -Pattern '(?s)allowsTargetedQuickMoveSource\(.{0,500}slot instanceof StorageUpgradeSlot.{0,200}getSlotUpgradeContainer\(slot\)\.isEmpty\(\)' -Label 'Sophisticated upgrade and upgrade-panel slots cannot use cross-menu quick-move routing'
Assert-Matches -Path $sophisticatedCoreCompat -Pattern '(?s)SETTINGS_MENU_GESTURE_POLICY.{0,1800}allowsTargetedQuickMoveSource\(.{0,200}return false;' -Label 'Sophisticated settings slots cannot use cross-menu quick-move routing'
Assert-Matches -Path $sophisticatedCoreCompat -Pattern '(?s)SETTINGS_MENU_GESTURE_POLICY.{0,1600}allowsPickupAllAnchor\(.{0,250}return false;' -Label 'Sophisticated settings slots cannot anchor desktop-wide pickup-all'
Assert-Count -Path $desktopContainerSessions -Text 'allowsQuickMoveRouting(' -Expected 4 -Label '26.2 server enforces special-source routing for single and batch quick moves'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void quickMove\(.{0,5000}!allowsQuickMoveRouting\(source\.menu, source\.slot, payload\.targetKind\(\)\).{0,500}resyncGesture\(player, sessions\);' -Label '26.2 server rejects disallowed targets for special quick-move sources'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void quickMoveAll\(.{0,5000}for \(AuthorizedSlot source : sources\).{0,1800}!allowsQuickMoveRouting\(source\.authorization\(\)\.menu\(\), slot, payload\.targetKind\(\)\).{0,500}resyncGesture\(player, sessions\);' -Label '26.2 server validates every special source before a batch quick move'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static boolean allowsQuickMoveRouting\(.{0,500}DesktopMenuSlots\.allowsTargetedQuickMoveSource\(menu, slot\).{0,300}targetKind == DesktopPackets\.QUICK_TARGET_DEFAULT.{0,200}DesktopMenuSlots\.useNativeQuickMove\(menu\)' -Label '26.2 default-only native quick moves cannot bypass special-source policy'

Assert-Count -Path $desktopContainerSessions -Text 'proportionalBatchCost(payload.' -Expected 4 -Label '26.2 variable-size gesture packets are precharged proportionally'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void handleMutation\(.{0,1800}sequence == MutationSequence\.NEXT.{0,300}preAuthorizationCost.{0,300}allowOperation\(.{0,300}mutation\.run\(\)' -Label '26.2 proportional gesture cost is charged only after mutation ordering and before payload authorization'
Assert-NotMatches -Path $desktopContainerSessions -Pattern '(?s)private static void dragSlots\((?:(?!\r?\n    private static).)*allowOperation\(player, "drag-slots", 1\.0D\)' -Label '26.2 drag does not double-charge after its proportional precharge'
Assert-NotMatches -Path $desktopContainerSessions -Pattern '(?s)private static void quickMoveAll\((?:(?!\r?\n    private static).)*allowOperation\(player, "quick-move-all", 1\.0D\)' -Label '26.2 quick-move-all does not double-charge after its proportional precharge'
Assert-NotMatches -Path $desktopContainerSessions -Pattern '(?s)private static void pickupAll\((?:(?!\r?\n    private static).)*allowOperation\(player, "pickup-all", 1\.0D\)' -Label '26.2 pickup-all does not double-charge after its encoded-session precharge'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void pickupAll\(.{0,2500}MAX_PICKUP_ALL_LOGICAL_SLOTS - totalLogicalSlots.{0,900}pickup-all-logical-scan.{0,200}Math\.min\(64\.0D, supplementalScanCost\)' -Label '26.2 pickup-all bounds and proportionally charges its authorized logical-slot scan'

Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void dragSlots\((?:(?!\r?\n    private static).)*new DragTargetPlan\((?:(?!\r?\n    private static).)*verifyDragPhysicalTargets\(plans\)(?:(?!\r?\n    private static).)*setByPlayer\(plan\.after\(\)\.copy\(\), plan\.before\(\)\.copy\(\)\)(?:(?!\r?\n    private static).)*setSharedCarried\(player, sessions, carriedAfter\)(?:(?!\r?\n    private static).)*rollbackDrag\(' -Label '26.2 mixed drag snapshots and verifies every physical target before committing its cursor'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void rollbackDrag\(.{0,1800}for \(int index = plans\.size\(\) - 1; index >= 0; index--\).{0,1500}restoreSlot\(.{0,1800}observedTargetGain.{0,1800}setSharedCarried\(player, sessions, reconciledCarried\)' -Label '26.2 failed mixed drags restore targets in reverse and reconcile the cursor to any unreverted credit'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void pickupAll\((?:(?!\r?\n    private static).)*new PickupTargetPlan\((?:(?!\r?\n    private static).)*safeTake\((?:(?!\r?\n    private static).)*verifyPickupSources\(plans\)(?:(?!\r?\n    private static).)*rollbackPickupAll\(' -Label '26.2 pickup-all snapshots and verifies every physical source transactionally'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void rollbackPickupAll\((?:(?!\r?\n    private static).)*for \(int index = touched\.size\(\) - 1; index >= 0; index--\)(?:(?!\r?\n    private static).)*matchesPhysicalSlot\((?:(?!\r?\n    private static).)*observedSourceLoss\((?:(?!\r?\n    private static).)*setSharedCarried\(player, sessions, reconciledCarried\)' -Label '26.2 failed pickup-all restores unique sources and preserves only proven cursor credit'
Assert-Count -Path $desktopContainerSessions -Text 'new QuickMoveWorkBudget(MAX_QUICK_MOVE_TRANSACTION_WORK)' -Expected 3 -Label '26.2 single, batch, and sort moves allocate hard work budgets'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void quickMoveAll\((?:(?!\r?\n    private static).)*QuickMoveWorkBudget quickMoveWork = new QuickMoveWorkBudget\(MAX_QUICK_MOVE_TRANSACTION_WORK\)(?:(?!\r?\n    private static).)*for \(QuickMoveSourceSnapshot source : sourceSnapshots\)(?:(?!\r?\n    private static).)*moveSlotStack\((?:(?!\r?\n    private static).)*quickMoveWork' -Label '26.2 batch quick move shares one bounded validation budget across every source'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static boolean moveSlotStack\((?:(?!\r?\n    private static).)*workBudget\.consume\((?:(?!\r?\n    private static).)*try \{(?:(?!\r?\n    private static).)*insertIntoMatchingSlots\(' -Label '26.2 quick move reserves worst-case validation work before slot callbacks'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static int insertQuickMoveTarget\(.{0,1400}ItemStack attempt = sourceBefore\.copy\(\).{0,900}returnedRemainder\[0\] = target\.safeInsert\(attempt\)' -Label '26.2 generic quick moves use the target callback returned remainder'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static int insertQuickMoveTarget\(.{0,3200}ItemStack\.matches\(expectedTarget, target\.getItem\(\)\).{0,700}ItemStack\.matches\(sourceBefore, source\.slot\.getItem\(\)\)' -Label '26.2 generic quick moves verify their exact target gain and unchanged source'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static int insertQuickMoveTarget\(.{0,4200}setByPlayer\(sourceAfter\.copy\(\), sourceBefore\.copy\(\)\).{0,800}moving\.setCount\(sourceAfter\.getCount\(\)\)' -Label '26.2 generic quick moves advance their shared remainder only after verified source consumption'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void rollbackQuickMoveTransaction\(.{0,2600}restoreSlot\(player, sessions, plan\.menu\(\), plan\.slot\(\), plan\.before\(\).{0,900}aggregateQuickMoveCredit.{0,1200}sourceRollback = ItemStack\.EMPTY.{0,1500}matchesPhysicalSlot\(.{0,700}restoreSlot\(player, sessions, source\.menu, source\.slot, sourceRollback' -Label '26.2 failed generic quick moves never replenish a source behind unverified or rebound target credit'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void clickMenu\((?:(?!\r?\n    private static).)*catch \(RuntimeException exception\).{0,200}throw exception;' -Label '26.2 click callback failures escape to full mutation recovery'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void button\((?:(?!\r?\n    private static).)*catch \(RuntimeException exception\).{0,200}throw exception;' -Label '26.2 button callback failures escape to full mutation recovery'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void placeRecipe\((?:(?!\r?\n    private static).)*catch \(RuntimeException exception\).{0,200}throw exception;' -Label '26.2 recipe callback failures escape to full mutation recovery'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void transferJeiRecipe\((?:(?!\r?\n    private static).)*target\.menu\(\) instanceof AbstractCraftingMenu.{0,2600}catch \(RuntimeException exception\).{0,200}throw exception;' -Label '26.2 JEI crafting callback failures escape to full mutation recovery'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void customPayload\((?:(?!\r?\n    private static).)*catch \(RuntimeException exception\).{0,200}throw exception;' -Label '26.2 custom callback failures escape to full mutation recovery'

Assert-Matches -Path $desktopItemSourceLocks -Pattern '(?s)resolver = RESOLVERS\.get\(menuType\(menu\)\);.{0,200}if \(resolver == null\) \{\s*return NOOP_LEASE;' -Label 'item-source locking remains an opt-in no-op without a loader resolver'
Assert-Matches -Path $desktopItemSourceLocks -Pattern '(?s)new SourceKey\(source\.inventorySlot, source\.identity\).{0,500}new ActiveLock\(source\.fingerprint\.copy\(\), source\.identityMatcher, 1\).{0,300}active\.references\+\+.{0,400}return new ActiveLease\(player, key\)' -Label 'item-source locks are identity-keyed and reference counted across sessions'
Assert-Matches -Path $desktopItemSourceLocks -Pattern '(?s)isCurrentSource\(Player player, SourceKey key, ActiveLock active\).{0,500}active\.matches\(current\).{0,1400}if \(--active\.references <= 0\).{0,200}playerLocks\.remove\(key\)' -Label 'item-source validity uses its stable matcher and releases only after the last lease'
Assert-Matches -Path $desktopItemSourceLocks -Pattern '(?s)hasLockedSourceInRange\((?:(?!\r?\n    public static).)*inventorySlot >= fromInclusive(?:(?!\r?\n    public static).)*inventorySlot < toExclusive(?:(?!\r?\n    public static).)*isCurrentSource\(' -Label 'item-source range checks accept only current identity-matched locks'
Assert-Matches -Path $sophisticatedBackpackItemLocks -Pattern '(?s)ContextType\.ITEM_BACKPACK.{0,500}saltsInventoryUpdate\$getHandlerName\(\).{0,500}switch \(handlerName\).{0,700}MAIN_INVENTORY.{0,300}OFFHAND_INVENTORY.{0,300}ARMOR_INVENTORY.{0,1000}getPlayerInventoryHandler\(handlerName\).{0,800}getStackInSlot\(player, itemContext\.saltsInventoryUpdate\$getIdentifier\(\), handlerSlot\).{0,800}getContentsUuid\(\).{0,600}matchesContentsUuid\(stack, contentsUuid\)' -Label 'Sophisticated source resolver locks the exact top-level held or worn item Backpack'
Assert-Matches -Path $sophisticatedBackpackItemLocks -Pattern '(?s)matchesContentsUuid\(ItemStack stack, UUID contentsUuid\).{0,300}BackpackWrapper\.fromStack\(stack\)\.getContentsUuid\(\)\.filter\(contentsUuid::equals\)' -Label 'Sophisticated source identity survives mutable Backpack component updates via contents UUID'
Assert-Contains -Path $sophisticatedBackpacksCompat -Text 'SophisticatedBackpackItemLocks.register(backpack, settings);' -Label '26.2 NeoForge registers item Backpack locks for main and settings menus'
Assert-Matches -Path $sophisticatedCoreCompat -Pattern '(?s)TransferItemsPayload\.TYPE\.id\(\)(?:(?!\r?\n        register).)*!payload\.transferToInventory\(\)(?:(?!\r?\n        register).)*DesktopItemSourceLocks\.hasLockedSourceInRange\(context\.player\(\), 9, Inventory\.INVENTORY_SIZE\)(?:(?!\r?\n        register).)*context\.broadcastChanges\(\);\s*return;' -Label 'Sophisticated bulk transfer-to-storage cannot move an open item Backpack into itself'
Assert-Matches -Path $sophisticatedCoreCompat -Pattern '(?s)TransferFullSlotPayload\.TYPE\.id\(\).{0,300}runForValidUnlockedSlot\(context, payload\.slotId\(\)' -Label 'Sophisticated full-slot transfers use the locked-source-aware handler'
Assert-Matches -Path $sophisticatedCoreCompat -Pattern '(?s)private static void runForValidUnlockedSlot\((?:(?!\r?\n    private static).)*DesktopItemSourceLocks\.isSlotLocked\(context\.player\(\), DesktopMenuSlots\.slot\(context\.menu\(\), slotId\)\)' -Label 'Sophisticated full-slot transfer rejects the exact locked Backpack source'
Assert-Matches -Path $sophisticatedMixinPlugin -Pattern '(?s)mixinClassName\.endsWith\("\.BackpackContextItemAccessor"\).{0,200}mixinClassName\.endsWith\("\.ServerGamePacketListenerItemLockMixin"\).{0,200}return this\.backpacksEnabled;' -Label 'item-lock mixins remain guarded by Sophisticated Backpacks presence'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)hasOpenSessionMatching\(Player player, Predicate<AbstractContainerMenu> menuPredicate\).{0,800}menuPredicate\.test\(session\.menu\)' -Label 'detached menu ownership can be checked without exposing session state'
Assert-Matches -Path $sophisticatedStorageOpenersMixin -Pattern '(?s)counter instanceof SophisticatedOpenersCounter.{0,500}hasOpenSessionMatching.{0,900}player\.containerMenu = desktopMenu;.{0,500}counter\.isOwnContainer\(player\).{0,300}finally \{\s*player\.containerMenu = previousMenu;' -Label 'Sophisticated physical storage opener checks use official ownership semantics for detached menus'
Assert-Matches -Path $sophisticatedMixinPlugin -Pattern '(?s)mixinClassName\.endsWith\("\.SophisticatedStorageOpenersMixin"\).{0,150}return this\.storageEnabled;' -Label 'storage opener mixin remains guarded by Sophisticated Storage presence'
Assert-Matches -Path $desktopContainerSession -Pattern '(?s)this\.sourceLock = DesktopItemSourceLocks\.acquire\(player, menu\).{0,2500}public void close\(\).{0,300}this\.sourceLock\.close\(\)' -Label 'client detached sessions hold and release item-source leases'
Assert-Matches -Path $desktopContainerClient -Pattern '(?s)DesktopContainerSession session = null;\s*boolean sessionHandedOff = false;.{0,1200}openOrAddSession\(context\.client\(\), session, payload\.visible\(\)\);\s*sessionHandedOff = true;.{0,500}if \(!sessionHandedOff && session != null\) \{\s*session\.close\(\);' -Label 'failed client reconstruction releases its provisional item-source lease'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)public void removeSession\(int sessionId\).{0,500}session\.close\(\)' -Label 'client session removal releases its item-source lease'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)this\.sessions\.remove\(transitionReplacedSession\);\s*transitionReplacedSession\.close\(\)' -Label 'client atomic session transitions release the replaced item-source lease'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private void clearForOwnerChange\(String reason\).{0,1200}for \(DesktopContainerSession session : this\.sessions\) \{\s*session\.close\(\);\s*\}\s*this\.sessions\.clear\(\)' -Label 'client disconnect and owner teardown release every item-source lease'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)canDragDistributeTo\(SlotHit hit\).{0,500}DesktopItemSourceLocks\.isSlotLocked' -Label 'client drag distribution rejects the locked Backpack source'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private void slotClicked\(SlotHit hit, int button, ContainerInput input\).{0,500}DesktopItemSourceLocks\.shouldRejectClick' -Label 'client Salt clicks reject the locked Backpack source'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private void quickMoveSlot\(SlotHit hit\).{0,500}DesktopItemSourceLocks\.isSlotLocked' -Label 'client quick moves reject the locked Backpack source'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)private void handleWorldClick\(MouseButtonEvent event\).{0,500}DesktopItemSourceLocks\.isSelectedSlotLocked.{0,1200}private void handleGameplayClick\(MouseButtonEvent event\).{0,500}DesktopItemSourceLocks\.isSelectedSlotLocked' -Label 'client cursor and crosshair world input cannot use the selected locked Backpack source'
Assert-Matches -Path $desktopInventoryScreen -Pattern '(?s)handleSlotKeyPressed\(SlotHit hit, KeyEvent event\).{0,600}boolean lockedHit = .{0,200}DesktopItemSourceLocks\.isSlotLocked.{0,1600}DesktopItemSourceLocks\.isSelectedSlotLocked.{0,1600}DesktopItemSourceLocks\.isInventorySlotLocked' -Label 'client drop, swap, pick, and hotbar keys cannot mutate the locked Backpack source'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)session\.sourceLock = DesktopItemSourceLocks\.acquire\(player, session\.menu\).{0,6000}private void close\(ServerPlayer player, int sessionId, boolean notifyClient\).{0,1200}finally \{\s*session\.releaseSourceLock\(\);' -Label 'server detached sessions hold item-source leases through close and transition cleanup'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void quickMove\(.{0,4500}DesktopItemSourceLocks\.isSlotLocked\(player, source\.slot\)' -Label 'authenticated Salt quick moves reject locked Backpack sources'
Assert-Matches -Path $desktopContainerSessions -Pattern '(?s)private static void quickMove\(.{0,11000}targets\.removeIf\(slot -> DesktopItemSourceLocks\.isSlotLocked\(player, slot\)\)' -Label 'authenticated Salt quick moves reject locked Backpack destinations'
Assert-Contains -Path $desktopContainerSessions -Text 'DesktopItemSourceLocks.shouldRejectClick(player, menu, slotIndex, button, input, effectiveCarried)' -Label 'authenticated Salt clicks reject the locked Backpack source'
Assert-Count -Path $desktopContainerSessions -Text 'DesktopItemSourceLocks.anyLockedSourceMatches(' -Expected 2 -Label 'vanilla and JEI recipe transfers both exclude the locked Backpack source'
Assert-Matches -Path $sophisticatedServerItemLockMixin -Pattern '(?s)@Inject\(method = "handleContainerClick".{0,900}DesktopItemSourceLocks\.shouldRejectClick.{0,800}@Inject\(method = "handlePlayerAction".{0,1000}@Inject\(method = "handleUseItem".{0,700}@Inject\(method = "handleUseItemOn".{0,700}@Inject\(method = "handleInteract".{0,700}@Inject\(method = "handleSetCreativeModeSlot".{0,1200}@Inject\(method = "tryPickItem"' -Label 'NeoForge server rejects vanilla click, drop, swap, use, creative, and pick packets for the locked Backpack source'
Assert-Contains -Path $sophisticatedBackpackContextAccessor -Text '@Accessor("handlerName")' -Label 'Sophisticated item context exposes its top-level handler identity'

$fabricSourceLockRegistrations = @(Get-ChildItem -LiteralPath (Join-Path $sophisticatedFabricRoot 'src\main\java') -Recurse -Filter '*.java' -File |
    Where-Object { (Get-Content -LiteralPath $_.FullName -Raw).Contains('DesktopItemSourceLocks.register(') })
if ($fabricSourceLockRegistrations.Count -ne 0) {
    Add-Failure "26.2 Fabric unexpectedly registers item-source locking in: $($fabricSourceLockRegistrations.FullName -join ', ')"
} else {
    Write-Host 'PASS 26.2 Fabric registers no item-source locking behavior' -ForegroundColor Green
}

if ((Test-Path -LiteralPath $sophisticatedMixinConfig -PathType Leaf) -and (Test-Path -LiteralPath $sophisticatedMixinRoot -PathType Container)) {
    $mixinConfig = Get-Content -LiteralPath $sophisticatedMixinConfig -Raw | ConvertFrom-Json
    $registeredMixins = @($mixinConfig.mixins) + @($mixinConfig.client) + @($mixinConfig.server)
    $declaredMixins = Get-ChildItem -LiteralPath $sophisticatedMixinRoot -Recurse -Filter '*.java' -File |
        Where-Object { (Get-Content -LiteralPath $_.FullName -Raw).Contains('@Mixin') } |
        ForEach-Object {
            $_.FullName.Substring($sophisticatedMixinRoot.Length + 1, $_.FullName.Length - $sophisticatedMixinRoot.Length - 6).Replace('\', '.')
        }
    $missingMixins = @($declaredMixins | Where-Object { $_ -notin $registeredMixins })
    $staleMixins = @($registeredMixins | Where-Object { $_ -notin $declaredMixins })
    $duplicateMixins = @($registeredMixins | Group-Object | Where-Object { $_.Count -ne 1 } | ForEach-Object { $_.Name })
    if ($missingMixins.Count -gt 0) {
        Add-Failure "26.2 NeoForge Sophisticated mixin classes are not registered: $($missingMixins -join ', ')"
    } elseif ($staleMixins.Count -gt 0) {
        Add-Failure "26.2 NeoForge Sophisticated mixin config has missing classes: $($staleMixins -join ', ')"
    } elseif ($duplicateMixins.Count -gt 0) {
        Add-Failure "26.2 NeoForge Sophisticated mixin classes are registered more than once: $($duplicateMixins -join ', ')"
    } else {
        Write-Host 'PASS 26.2 NeoForge every Sophisticated mixin class is registered exactly once' -ForegroundColor Green
    }
}

foreach ($version in $versions) {
    $runtimeRei = Join-Path $RepoRoot "versions\$version\fabric\src\main\java\com\salts_inventory_update\compat\rei\RuntimeReiRecipeBrowserAccess.java"
    Assert-File -Path $runtimeRei -Label "$version shared REI runtime access retained" | Out-Null

    $nonFabricLoader = if ($version -eq '1.20.1') { 'forge' } else { 'neoforge' }
    $nonFabricRei = Join-Path $RepoRoot "versions\$version\$nonFabricLoader\src\main\java\com\salts_inventory_update\compat\rei\SaltsReiForgeClientPlugin.java"
    Assert-File -Path $nonFabricRei -Label "$version non-Fabric REI client plugin retained" | Out-Null
}

$versionDifferences = Join-Path $RepoRoot 'version_differences.md'
$emiRuntime = Join-Path $RepoRoot 'versions\1.21.1\fabric\src\main\java\com\salts_inventory_update\compat\emi\RuntimeEmiRecipeBrowserAccess.java'
$emiBootstrap = Join-Path $RepoRoot 'versions\1.21.1\fabric\src\main\java\com\salts_inventory_update\compat\emi\EmiRecipeBrowserBootstrap.java'
$emiFabricPlugin = Join-Path $RepoRoot 'versions\1.21.1\fabric\src\main\java\com\salts_inventory_update\compat\emi\SaltsEmiClientPlugin.java'
$emiNeoForgePlugin = Join-Path $RepoRoot 'versions\1.21.1\neoforge\src\main\java\com\salts_inventory_update\compat\emi\SaltsEmiForgeClientPlugin.java'
$emiFabricMetadata = Join-Path $RepoRoot 'versions\1.21.1\fabric\src\main\resources\fabric.mod.json'
$emiClientStartup = Join-Path $RepoRoot 'versions\1.21.1\fabric\src\main\java\com\salts_inventory_update\client\WindowedInventoryClient.java'
Assert-Contains -Path $versionDifferences -Text 'Minecraft 1.21.1 and earlier versions support EMI integration.' -Label 'EMI version boundary is documented'
Assert-Contains -Path $versionDifferences -Text 'available on every Forge and NeoForge target that Salt builds' -Label 'Sophisticated loader/version boundary is documented'
Assert-Contains -Path $versionDifferences -Text 'single Salt frame' -Label 'Sophisticated single-frame composition is documented'
Assert-Contains -Path $versionDifferences -Text 'Every Fabric target continues to use the complete native Sophisticated screens' -Label 'Fabric Sophisticated behavior is documented as unchanged'
Assert-Contains -Path $versionDifferences -Text 'Decoration Table is intentionally excluded' -Label 'Sophisticated Decoration Table exclusion is documented'
Assert-Contains -Path $versionDifferences -Text 'compact Sophisticated toolbar sits at the left above the raised storage grid' -Label 'Sophisticated compact raised header is documented'
Assert-Contains -Path $versionDifferences -Text 'temporarily hides the sort and transfer controls' -Label 'Sophisticated search-aware toolbar behavior is documented'
Assert-Contains -Path $versionDifferences -Text 'initial size that exposes every storage row that fits' -Label 'Sophisticated all-row preferred default is documented'
Assert-Contains -Path $versionDifferences -Text 'scrollbar gutter only while scrolling is actually required' -Label 'Sophisticated conditional scrollbar reserve is documented'
Assert-Contains -Path $versionDifferences -Text 'snaps the frame back to the useful storage-row count' -Label 'Sophisticated useful-height resize snap is documented'
Assert-Contains -Path $versionDifferences -Text 'spaced, Salt-framed rail attached to the left side' -Label 'Sophisticated attached upgrade rail is documented'
Assert-Contains -Path $versionDifferences -Text 'exact hosted slot grid instead of retaining Sophisticated''s native outer margins' -Label 'Sophisticated compact width without duplicated native margins is documented'
Assert-Contains -Path $versionDifferences -Text 'save, load, and export controls instead occupy the same tucked-under Salt-framed left tab design' -Label 'Sophisticated settings template rail is documented'
Assert-Contains -Path $versionDifferences -Text 'Every visible right-side native tab replaces its original tab plate with an individual Salt window frame' -Label 'Sophisticated individual right tab frames are documented'
Assert-Contains -Path $versionDifferences -Text 'base window grows only enough to meet the bottom of that closed tab stack' -Label 'Sophisticated closed right tab minimum height is documented'
Assert-Contains -Path $versionDifferences -Text 'Limited Barrels and their settings retain enough fixed body space' -Label 'Sophisticated limited settings specialized sizing is documented'
Assert-Contains -Path $versionDifferences -Text 'one canonical item origin' -Label 'Sophisticated main and popup slot alignment is documented'
Assert-Contains -Path $versionDifferences -Text 'same authenticated detached-container session as the main storage grid' -Label 'Sophisticated popup slot drag routing is documented'
Assert-Contains -Path $versionDifferences -Text 'that exact source Backpack is locked for the lifetime of its Salt session' -Label 'Sophisticated item Backpack source lock is documented'
Assert-Contains -Path $versionDifferences -Text 'exclusive to Forge and NeoForge and does not change Fabric behavior' -Label 'Sophisticated item-source lock loader boundary is documented'
Assert-Contains -Path $versionDifferences -Text 'physical Sophisticated Storage block remains visibly open' -Label 'Sophisticated detached storage opener lifetime is documented'
Assert-Contains -Path $emiRuntime -Text 'RecipeBrowserSource.EMI' -Label '1.21.1 shared EMI runtime access retained'
Assert-Contains -Path $emiRuntime -Text 'EmiCraftingRecipe' -Label '1.21.1 EMI crafting transfer support retained'
Assert-Contains -Path $emiRuntime -Text 'EMI_FAVORITES_CLASS' -Label '1.21.1 EMI favorites support retained'
Assert-Contains -Path $emiRuntime -Text 'cachedIndexEntriesByType' -Label '1.21.1 EMI index rendering cache retained'
Assert-Contains -Path $emiRuntime -Text 'recipeWidgetCache' -Label '1.21.1 EMI recipe widget cache retained'
Assert-Contains -Path $emiRuntime -Text 'EMI_INGREDIENT_RECIPE_MAX_HEIGHT' -Label '1.21.1 EMI tag recipes use compact paging'
Assert-Contains -Path $emiRuntime -Text 'usesCompactRecipeSpacing' -Label '1.21.1 EMI tag recipes remain top-packed in tall windows'
Assert-Contains -Path $emiRuntime -Text 'renderHoveredSlot' -Label '1.21.1 EMI hover highlight renders beneath slot contents'
Assert-Contains -Path $emiRuntime -Text 'renderRecipeTooltip' -Label '1.21.1 EMI recipe widget tooltips retained'
Assert-Contains -Path $emiRuntime -Text 'JEMI_TAG_RECIPE_HEIGHT = 42' -Label '1.21.1 EMI compacts JEI tag recipes imported through JEMI'
Assert-Contains -Path $emiRuntime -Text 'compactJemiTagWidgets' -Label '1.21.1 EMI replaces oversized JEMI tag widget bounds'
Assert-Contains -Path $emiBootstrap -Text 'RecipeBrowserBridge.install(RecipeBrowserSource.EMI, access)' -Label '1.21.1 EMI startup bootstrap retained'
Assert-Contains -Path $emiClientStartup -Text 'EmiRecipeBrowserBootstrap.initialize();' -Label '1.21.1 client initializes EMI integration'
Assert-Contains -Path $emiFabricPlugin -Text 'implements EmiPlugin' -Label '1.21.1 Fabric EMI client plugin retained'
Assert-Contains -Path $emiNeoForgePlugin -Text '@EmiEntrypoint' -Label '1.21.1 NeoForge EMI client plugin retained'
Assert-Contains -Path $emiFabricMetadata -Text '"emi": [' -Label '1.21.1 Fabric EMI entrypoint metadata retained'
foreach ($version in @('1.21.11', '26.1.2', '26.2')) {
    $newerFabricMetadata = Join-Path $RepoRoot "versions\$version\fabric\src\main\resources\fabric.mod.json"
    Assert-NotContains -Path $newerFabricMetadata -Text '"emi": [' -Label "$version has no unsupported EMI entrypoint"
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
    '1.21.1' = @{ Loader = 'neoforge'; LanguageRange = '[4.0.34,5)'; ModRange = '[21.1.229,21.2)'; File = 'neoforge.mods.toml' }
    '1.21.11' = @{ Loader = 'neoforge'; LanguageRange = '[10,11)'; ModRange = '[21.11.42,21.12)'; File = 'neoforge.mods.toml' }
    '26.1.2' = @{ Loader = 'neoforge'; LanguageRange = '[11,12)'; ModRange = '[26.1.2.59-beta,26.1.3)'; File = 'neoforge.mods.toml' }
    '26.2' = @{ Loader = 'neoforge'; LanguageRange = '[11,12)'; ModRange = '[26.2.0.53-beta,26.3)'; File = 'neoforge.mods.toml' }
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

foreach ($version in $versions) {
    $gestureScreen = Join-Path $RepoRoot "versions\$version\fabric\src\main\java\com\salts_inventory_update\client\InventoryDesktopScreen.java"
    Assert-Contains -Path $gestureScreen -Text 'CREATIVE_TAB_PAGE_BUTTON_WIDTH = 11;' -Label "$version creative atlas uses complete 11-pixel button halves"
    Assert-Contains -Path $gestureScreen -Text 'enabled && hovered ? 2 * CREATIVE_TAB_PAGE_BUTTON_WIDTH : 0' -Label "$version creative hover atlas stride matches button width"
    Assert-Contains -Path $gestureScreen -Text 'next.x(), next.y(), CREATIVE_TAB_PAGE_BUTTON_WIDTH, 12' -Label "$version creative disabled next atlas stride matches button width"
    $lifecycleRoot = Join-Path $RepoRoot "versions\$version\fabric\src\main\java\com\salts_inventory_update\mixin"
    $openerMixin = Join-Path $lifecycleRoot 'server\ChestOpenersCounterMixin.java'
    foreach ($block in @('ChestBlockEntity$1', 'BarrelBlockEntity$1', 'EnderChestBlockEntity$1')) {
        Assert-Contains -Path $openerMixin -Text $block -Label "$version detached ownership covers $block"
    }
    $enderMixin = Join-Path $lifecycleRoot 'server\ChestMenuEnderChestMixin.java'
    Assert-Contains -Path $enderMixin -Text 'DesktopContainerSessions.shouldCapture(player)' -Label "$version vanilla/disabled ender lifecycle remains native"
    Assert-Contains -Path $enderMixin -Text 'this.salts_inventory_update$enderChest.stopOpen(player)' -Label "$version closes the bound physical ender chest"
    Assert-Contains -Path $enderMixin -Text 'this.salts_inventory_update$enderChest.stillValid(player)' -Label "$version validates the bound physical ender chest"
    $lifecycleConfig = Join-Path $RepoRoot "versions\$version\common\src\main\resources\salts_inventory_update.mixins.json"
    Assert-Contains -Path $lifecycleConfig -Text 'server.ChestMenuEnderChestMixin' -Label "$version registers ender lifecycle on both loaders"
    Assert-Contains -Path $lifecycleConfig -Text 'accessor.PlayerEnderChestContainerAccessor' -Label "$version registers ender physical source accessor"
    Assert-NotMatches -Path $gestureScreen -Pattern 'handleSlotMouseClicked\(slotHit, event, doubleClick\)' -Label "$version ordinary slot clicks preserve previous-slot double-click identity"
    $gestureServer = Join-Path $RepoRoot "versions\$version\fabric\src\main\java\com\salts_inventory_update\server\DesktopContainerSessions.java"
    Assert-Matches -Path $gestureServer -Pattern '(?s)private static void clickMenu\(.{0,1000}DesktopMenuSlots\.slot\(menu, slotIndex\) == null' -Label "$version server clicks accept validated logical upgrade slots"
    Assert-Matches -Path $gestureServer -Pattern '(?s)private static ItemStack serverSlotStack\(.{0,250}DesktopMenuSlots\.slot\(menu, slotIndex\)' -Label "$version server slot snapshots include upgrade slots"
    Assert-Matches -Path $gestureScreen -Pattern '(?s)if \(vanillaDoubleClick && event\.button\(\) == GLFW\.GLFW_MOUSE_BUTTON_LEFT && !isCraftingResultSlot\(hit\)\) \{.{0,1800}this\.dispatchPickupAll\(hit\);' -Label "$version double-click dispatches authenticated cross-window collection"
}

# Sort-to-open-containers must remain available on every supported version/loader.
$sortAtlasReference = Join-Path $RepoRoot 'versions\26.2\common\src\main\resources\assets\salts_inventory_update\textures\gui\window_controls.png'
foreach ($version in $versions) {
    $sortRoot = Join-Path $RepoRoot "versions\$version\fabric\src\main\java\com\salts_inventory_update"
    $sortScreen = Join-Path $sortRoot 'client\InventoryDesktopScreen.java'
    $sortClient = Join-Path $sortRoot 'client\DesktopContainerClient.java'
    $sortServer = Join-Path $sortRoot 'server\DesktopContainerSessions.java'
    $sortPackets = Join-Path $sortRoot 'network\DesktopPackets.java'
    $sortSlots = Join-Path $sortRoot 'internal\desktop\DesktopSortSlots.java'
    Assert-Contains -Path $sortScreen -Text 'SORT_CONTROL_TEXTURE_COLUMN = 10;' -Label "$version normal sort atlas column"
    Assert-Contains -Path $sortScreen -Text 'FORCE_SORT_CONTROL_TEXTURE_COLUMN = 11;' -Label "$version Shift sort atlas column"
    Assert-Contains -Path $sortScreen -Text 'CONTROL_TEXTURE_WIDTH = CONTROL_SIZE * 12;' -Label "$version expanded control atlas width"
    Assert-Contains -Path $sortScreen -Text 'this.isShiftHeld() ? FORCE_SORT_CONTROL_TEXTURE_COLUMN : SORT_CONTROL_TEXTURE_COLUMN' -Label "$version live Shift sort icon"
    Assert-Contains -Path $sortScreen -Text 'window.minimized || window.ghosted || window.persistentHidden' -Label "$version sort excludes unavailable windows"
    Assert-Contains -Path $sortClient -Text 'canSortWindows()' -Label "$version sort capability gate"
    Assert-Contains -Path $sortPackets -Text 'CAP_SORT_WINDOWS = 1L << 5;' -Label "$version sort capability bit"
    Assert-Contains -Path $sortPackets -Text 'record DesktopSortWindowsPayload(' -Label "$version sort request payload"
    Assert-Contains -Path $sortServer -Text 'DesktopSortWindowsPayload.TYPE' -Label "$version sort server registration"
    Assert-Contains -Path $sortServer -Text 'executeSortTransfers(' -Label "$version sort routing"
    Assert-Contains -Path $sortServer -Text 'sourceSlot.onQuickCraft(remainder, sourceBefore)' -Label "$version sort output accounting"
    Assert-Contains -Path $sortServer -Text 'sourceSlot.onTake(player, sourceBefore.copyWithCount(extractedCount))' -Label "$version sort extracted-stack callback"
    Assert-Contains -Path $sortServer -Text 'contents().stream().allMatch(ItemStack::isEmpty)' -Label "$version sort empty fallback uses initial contents"
    Assert-Contains -Path $sortSlots -Text 'menu instanceof AbstractFurnaceMenu) return index == 2;' -Label "$version furnace sort output only"
    Assert-Contains -Path $sortSlots -Text 'menu instanceof BrewingStandMenu) return index >= 0 && index < 3;' -Label "$version brewing sort bottles only"
    $sortLang = Join-Path $RepoRoot "versions\$version\common\src\main\resources\assets\salts_inventory_update\lang\en_us.json"
    Assert-Contains -Path $sortLang -Text '"Sort into open containers"' -Label "$version normal sort tooltip"
    Assert-Contains -Path $sortLang -Text '"Force sort into open containers"' -Label "$version Shift sort tooltip"
    $sortAtlas = Join-Path $RepoRoot "versions\$version\common\src\main\resources\assets\salts_inventory_update\textures\gui\window_controls.png"
    if (Assert-File -Path $sortAtlas -Label "$version sort atlas") {
        if ((Get-FileHash -LiteralPath $sortAtlas).Hash -ne (Get-FileHash -LiteralPath $sortAtlasReference).Hash) {
            Add-Failure "$version sort atlas differs from the 26.2 reference"
        } else {
            Write-Host "PASS $version sort atlas matches 26.2" -ForegroundColor Green
        }
    }
    $sortLoader = if ($version -eq '1.20.1') { 'forge' } else { 'neoforge' }
    $sortCompat = Join-Path $RepoRoot "versions\$version\$sortLoader\src\main\java\com\salts_inventory_update\compat\sophisticated\SophisticatedCoreCompat.java"
    Assert-Contains -Path $sortCompat -Text 'DesktopSortSlots.registerStorage(menuType,' -Label "$version $sortLoader Sophisticated sort storage slots"
}

if ($failures.Count -gt 0) {
    Write-Host "Source feature parity failed with $($failures.Count) issue(s)." -ForegroundColor Red
    exit 1
}

Write-Host 'Source feature parity passed.' -ForegroundColor Green
