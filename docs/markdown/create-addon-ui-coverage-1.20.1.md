# Create addon UI coverage - Minecraft 1.20.1

The native Create rollout covers its retained 20 base/CBC/Rails menu types. It does **not** establish complete community-addon coverage. This follow-up checked publisher metadata and 369 original JARs across 270 projects without installing or executing them. 46 additional project entries have container-backed screen/menu candidates requiring review. This includes alternative branches and mounted third-party integrations; it is not a count of unique new inventory windows.

The full machine-readable inventory, selected versions, loaders, Create requirements, exact class names, publisher links and release hashes are saved under `output/create-addon-coverage-audit/`. The implementation queue is `implementation-queue.json`, with a spreadsheet-readable `coverage.csv`.

## First native-window review batch

| Addon | Declared screen/menu candidates | Release notes |
|---|---|---|
| [Create: Numismatics](https://modrinth.com/mod/numismatics) | TrustList, Bank, SubAccountList, BlazeBanker, AndesiteDepositor, BrassDepositor, SalepointConfig, SalepointPurchase, Vendor | Vendor, sale points, depositors and banking require native UI work. Trust/subaccount lists are configuration states; assess inventory relevance per view. |
| [Create: Storage [Neo/Forge]](https://modrinth.com/mod/create-storage-neo-forge) | Backpack, StorageBox, StorageBoxMounted, SimpleStorageBox, SimpleStorageBoxMounted | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create Stuff 'N Additions](https://modrinth.com/mod/create-stuff-additions) | BrassDroneGUI, VaultModuleGUI | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create Utilities](https://modrinth.com/mod/create-utilities) | VoidChest | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create: Central Kitchen](https://modrinth.com/mod/create-central-kitchen) | CookingGuide, MinersCookingGuide | Cooking Guide and Miner's Cooking Guide are editable ingredient-filter menus: addSlots creates six/four ghost ingredient cells plus player inventory. Do not exclude these solely because their names contain "Guide". |
| [Create: Diesel Generators](https://modrinth.com/mod/create-diesel-generators) | EntityFilter | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create: Tweaked Controllers](https://modrinth.com/mod/create-tweaked-controllers) | TweakedLinkedController | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create: The Factory Must Grow](https://modrinth.com/mod/create-tfmg) | EngineController | Engine Controller has six ghost frequency cells plus player inventory, confirmed by addSlots bytecode. Electrician's Wrench is a separate non-container control. |
| [Create Nuclear](https://modrinth.com/mod/createnuclear) | ReactorBluePrintItem, ReactorBluePrint, ReactorInput, ReactorRodInput | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create: Food](https://modrinth.com/mod/create-food) | ClothSackItem, ClothSack, RationBoxItem, RationBox | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create Big Cannons: Advanced Technologies](https://modrinth.com/mod/create-big-cannons-advanced-technologies) | HeavyAutocannonAmmoContainer | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create: Vintage](https://modrinth.com/mod/create-vintage-unofficial-port) | Lathe, RecipeCard | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create: Sound of Steam](https://modrinth.com/mod/create-sound-of-steam) | KeyboardRelay, RollPuncher, TrackerBar | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create: Additional Logistics](https://modrinth.com/mod/create-additional-logistics) | CashRegister | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create Factory Logistics](https://modrinth.com/mod/create_factory_logistics) | FactoryPanelSetFluid | Original UI capture, native-style design and focused gameplay acceptance still required. |
| [Create: Mobile Packages](https://modrinth.com/mod/create-mobile-packages) | MobilePackagerEdit, MobilePackager, PortableStockTicker | Includes mobile packager/edit, portable stock ticker and Bee Port paths. Publisher marks 1.20.1 Forge no longer supported, but its release remains available. |
| [Sophisticated Backpacks Create Integration](https://modrinth.com/mod/sophisticated-backpacks-create-integration) | MountedBackpackContainerMenu, MountedBackpackSettingsContainerMenu | Declares its own mounted backpack and settings menus/screens; do not assume a generic Sophisticated screen match covers them. |
| [Sophisticated Storage Create Integration](https://modrinth.com/mod/sophisticated-storage-create-integration) | MountedLimitedBarrelContainerMenu, MountedLimitedBarrelSettingsContainerMenu, MountedStorageContainerMenu, MountedStorageSettingsContainerMenu | Declares mounted storage/limited-barrel and settings menus/screens; existing owning-mod coverage must be verified. |

## Additional UI candidates

- [[DEPRECATED]Create Enchantment Industry - Fabric Legacy](https://modrinth.com/mod/create-enchantment-industry-fabric-legacy): EnchantingGuideScreen. Deprecated Fabric branch; keep separate from the current Enchantment Industry project.
- [Create: Misc and Things](https://modrinth.com/mod/create-misc-and-things): BrassSpeakerGUIScreen, InscriberGUIScreen. 
- [Create: LazyTick](https://modrinth.com/mod/createlazytick): NutKineticScreen, LazyTickScrollerScreen. Performance/configuration tool screens are menu-backed; visually assess whether any item filter needs a window, and keep pure settings outside inventory scope.
- [Create: Vintage Improvements](https://modrinth.com/mod/create-vintage-improvements): LatheScreen, RecipeCardScreen. Original older branch. The separate Create: Vintage project declares Create 6 support.
- [Create More: Package Couriers](https://modrinth.com/mod/create-more-package-couriers): PortableStockTickerScreen. 
- [Create: The Air War](https://modrinth.com/mod/create-the-air-war): DfgdfgScreen, FdgddScreen, UfyScreen. Published GUI classes use opaque generated names; in-game opening and translation lookup are required to identify the actual menus.
- [Create: Metallurgy](https://modrinth.com/mod/create-metallurgy): LadleFilterScreen. 
- [Create: Northstar - Redux](https://modrinth.com/mod/northstar-redux): AstronomyTableScreen, RocketStationScreen, SpaceAtlasScreen. Astronomy/rocket menus need visual scope review; Space Atlas and telescope/navigation controls must not be converted merely because of their Java superclass.
- [Create: Amazing Trading](https://modrinth.com/mod/create-amazing-trading): ShopScreen, ShredderScreen. 
- [Create: Winery](https://modrinth.com/mod/create-winery): WineCellarGUIScreen. 
- [Destroy](https://modrinth.com/mod/destroy): RedstoneProgrammerScreen, MixedExplosiveScreen. 
- [Create: Missions](https://modrinth.com/mod/missions): MechanicalExchangerScreen. 
- [Create Missiles](https://modrinth.com/mod/create-missiles): AssemblyPanelScreen, ControlPanelScreen, DroneScreen, NavigationPanelScreen. 
- [Create: Quality of Life](https://modrinth.com/mod/create-qol): ChooseLogisticNetworkScreen, ConfigureStatueScreen, StockManagerScreen. 
- [Create: Teleporters](https://modrinth.com/mod/create-teleporters): BlockTeleporterGuiScreen, CustomTeleporterGuiScreen, EntityTeleporterGuiScreen, ItemTeleporterGuiScreen. 
- [Create: Biotech - 机械动力：生物科技](https://modrinth.com/mod/create-biotech): ShulkerTeleporterScreen, SpiderAssemblyTableScreen, AllayPortScreen, AllayCourierScreen. Wireless stockkeeper and courier/port variants require checks for both new menus and shared Create behavior.
- [Create: Northstar](https://modrinth.com/mod/create-northstar): AstronomyTableScreen, RocketStationScreen, TelescopeScreen. Older original release; keep separate from Northstar Redux.
- [Create: Storage](https://modrinth.com/mod/fxnt-create-storage): BackPackScreen, StorageBoxScreen, SimpleStorageBoxScreen. Original Fabric 0.20 pins Create 0.5.1-f-build.1417; separate from the current Forge port and the Create 6 runtime.
- [Create Stock Bridge](https://modrinth.com/mod/create-stock-bridge): AEStockBridgeScreen. AEStockBridgeScreen extends AEBaseScreen; assess existing AE integration inheritance and native registration before adding another renderer.
- [Create: FluidLogistics](https://modrinth.com/mod/createfluidlogistic): HandPointerFilterScreen, CopperFrogportScreen, ResourceFactoryGaugeSetFilterScreen. 
- [Create Logistics](https://modrinth.com/mod/create-logistics): VaultWalletGUIScreen. 
- [Applied Create](https://modrinth.com/mod/applied-create): MEBlueprintCannonScreen, MEGearboxScreen. 
- [Create: Arms Race](https://modrinth.com/mod/create-arms-race): ResearchTableScreen. 
- [Create: Fantasizing Again](https://modrinth.com/mod/create-fantasizing-again): AndesiteCrateScreen, BrassCrateScreen, IronCrateScreen, SturdyCrateScreen. 
- [Create: Smart Crafter](https://modrinth.com/mod/create-smart-crafter): SmartCrafterScreen. 
- [Create: Mechanical Companion](https://modrinth.com/mod/create-mechanical-companion): WolfScreen. 
- [Create: Springs](https://modrinth.com/mod/create-springs): PortativeEngineScreen, SpringScreen. 
- [Create: Vault UI](https://modrinth.com/mod/vault-ui): VaultScreen. 

## Publisher-only follow-up

[Create: Protection Pixel](https://www.curseforge.com/minecraft/mc-mods/protection-pixel) documents an Armor Load Platform menu in its 1.20.1 Forge release. Its UI still needs a release-level/in-game review; it is additional to the 270 scanned projects.
Create Robotics has a public repository, but no applicable 1.20.1 release was established in this check. Keep it on a version-verification watchlist instead of claiming coverage.

## Shared screens and visual variants

The following have no separately declared menu requiring a new template in the scanned JARs, but need regression checks with their owning UI. They are not marked compatible merely because the base Create screen is covered.

- [Create: Cardboarded Conveynience](https://modrinth.com/mod/create-cardboarded-conveynience): Test shared package/container interactions; absence of a declared screen does not establish compatibility.
- [Clockwork](https://modrinth.com/mod/create-clockwork): Physics/contraption source-context regression; no dedicated screen declaration found.
- [Create Contraption Terminals](https://modrinth.com/mod/create-contraption-terminals): Uses Tom's Simple Storage terminals on moving contraptions; verify the owning mod integration and source context.
- [Create: Interactive](https://modrinth.com/mod/interactive): Contraption/container source-context regression; no dedicated screen declaration found.
- [Create: Vibrant Vaults](https://modrinth.com/mod/create-vibrant-vaults): Color/variant regression coverage for existing Create package-port and requester UIs; no new screen class is required to change their appearance.
- [Create: Connected](https://modrinth.com/mod/create-connected): Changes existing Create features, including Attribute Filter; test with the addon present.
- [Create: FastSchematicCannon](https://modrinth.com/mod/create-fast-schematic-cannon): Modifies a shared Schematicannon path; verify native-frame layout and widgets with the addon present.
- [Create: Compatible Storage](https://modrinth.com/mod/create-compatible-storage): Storage/contraption bridge; requires owning-container regression checks.
- [create-blueprint-tweaks](https://modrinth.com/mod/create-blueprint-tweaks): Modifies shared Blueprint behavior; verify the native frame with this addon present.
- [Create: Storage Drawers compat](https://modrinth.com/mod/storage-drawers-create-compat): Uses the owning drawer interface; verify mounted/source behavior rather than inventing another UI.
- [Create: Connected Fabric](https://modrinth.com/mod/create-connected-fabric): Fabric port of Connected; test shared Create screens on a matching release.
- [Create: Sophisticated Backpacks Compat](https://modrinth.com/mod/create-sophisticated-backpacks-compat): Separate bridge project; test placed/mounted backpack opening with the owning Sophisticated integration.

Sophisticated Backpacks/Storage Create Integration are in the main pending queue because they actually declare mounted menus/screens. Their settings variants must also be accounted for.

## Controls, books and version boundaries

Connected/Pulse Generator, Railways Navigator, Radars, Encased/Brass Shaft, Ender Transmission, Extra Gauges, Optical/Hologram Source, track/door/bogey selectors and other configuration-only screens remain native under the agreed inventory-only scope. The CSV records the exact classes. An Ore Vein Atlas or claim settings screen is not made inventory-related by extending a container superclass.

Central Kitchen is a useful exception to name-based filtering: its Cooking Guide and Miner's Cooking Guide menus create ingredient ghost cells plus player inventory. They are editable filter/configuration menus, not merely read-only recipe-book pages. Their visual design still needs an original in-game capture.

Original Fabric Create: Storage 0.20 pins Create 0.5.1-f-build.1417. The Forge Create: Storage port is a separate release line supporting Create 6. Likewise distinguish original Vintage Improvements from the newer Create: Vintage port, and original Northstar from Northstar Redux. Do not install all branches in one runtime.

## Verification limits

- This is a bounded discovery audit, not proof that every Create addon ever released was found. Download counts are a selection aid, not an official endorsement.
- Container-screen inheritance is a candidate signal, not proof every view is inventory-related. Read-only books/maps and pure configuration remain outside the agreed window scope.
- No new addon was launched or functionally tested in this audit. A pending native design does not imply SIU fallback is broken.
- Declared version ranges do not prove Create 6 binary compatibility, especially old releases with unbounded minimum constraints.
- A project with no declared custom screen may reuse or modify another mod's UI; this is not a compatibility pass.
- The installed game/modpack was not changed; no public API or runtime implementation changed.
