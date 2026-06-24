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
    'DesktopReadyPayload',
    'DesktopClickPayload',
    'DesktopQuickMovePayload',
    'DesktopButtonPayload',
    'DesktopPlaceRecipePayload',
    'DesktopRenamePayload',
    'DesktopCustomPayload',
    'DesktopCloseSessionPayload',
    'DesktopSessionPinPayload',
    'DesktopSessionVisibilityPayload',
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

    Assert-Contains -Path $packets -Text 'PIN_MODE_GHOST_PINNED' -Label "$version packet pin modes"
    Assert-Contains -Path $packets -Text 'QUICK_TARGET_HOTBAR' -Label "$version quick move target"
    Assert-Contains -Path $windowedClient -Text 'GLFW_KEY_C' -Label "$version character keybind"
    Assert-Contains -Path $windowedClient -Text '"salts_inventory"' -Label "$version client command root"
    Assert-Contains -Path $windowedClient -Text '"config"' -Label "$version config command"
    Assert-Contains -Path $windowedClient -Text 'FunctionalTestHarness' -Label "$version functional hook"
    Assert-Contains -Path $config -Text 'enableMod' -Label "$version config enableMod"
    Assert-Contains -Path $config -Text 'expandableInventory' -Label "$version config expandableInventory"
    Assert-Contains -Path $config -Text 'enableGhostPins' -Label "$version config ghost pins"
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
            $buildScript = Join-Path $RepoRoot "build.gradle.kts"
            Assert-Contains -Path $buildScript -Text '"--mixin.config", "salts_inventory_update.mixins.json"' -Label "$version $loader dev run mixin launch arg"
        }
    }
}

if ($failures.Count -gt 0) {
    Write-Host "Source feature parity failed with $($failures.Count) issue(s)." -ForegroundColor Red
    exit 1
}

Write-Host 'Source feature parity passed.' -ForegroundColor Green
