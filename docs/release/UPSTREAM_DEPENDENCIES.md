# Pinned upstream dependencies

This guide records declarations from the exact publisher jars in [the reviewed support table](compatibility-pins.md), including its six intentionally native entries. It does not add upstream support or establish gameplay qualification.

Install the upstream mods and their matching libraries separately from SIU. The [complete dependency table](upstream-dependencies.csv) preserves each publisher hash, owning mod ID, version constraint, environment/side, ordering and original required/optional fields. Version alternatives remain JSON arrays. Blank fields preserve an omitted declaration.

These declarations are not a list of separate downloads: a publisher may bundle libraries, and a mod manager can supply transitive dependencies. This inventory does not resolve the complete dependency graph, validate every range against installed libraries, or override publisher constraints. Optional addons remain optional for SIU. A `none-declared` row records an empty descriptor, not proof that a mod is dependency-free.

Fabric `depends` and Forge `mandatory=true` declare requirements. Fabric recommendations, suggestions and conflicts retain their distinct original categories. NeoForge uses its dependency `type`, defaulting to `required` when omitted; legacy or extra flags are preserved in separate columns. See the [Fabric descriptor reference](https://docs.fabricmc.net/develop/loader/fabric-mod-json) and [NeoForge 1.21.1 descriptor reference](https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/).

## Declared addon chains

These are required base mods that also have a pin in this inventory on the same target. Library requirements and exact ranges/sides remain in the CSV. This table does not certify that every listed optional combination works together.

| Addon / dependent mod | Required recorded base mod IDs | Exact target / publisher release |
| --- | --- | --- |
| [Let's Do] Bakery - Farm&Charm Compat | `farm_and_charm` | 1.20.1 fabric / 2.0.6; 1.20.1 forge / 2.0.6; 1.21.1 fabric / 2.1.11; 1.21.1 neoforge / 2.1.11 |
| [Let's Do] Candlelight - Farm&Charm compat | `farm_and_charm` | 1.20.1 fabric / 2.0.5; 1.20.1 forge / 2.0.5; 1.21.1 fabric / 2.1.15; 1.21.1 neoforge / 2.1.15 |
| [Let's Do] NetherVinery | `vinery` | 1.20.1 fabric / 1.2.19; 1.20.1 forge / 1.2.19 |
| Ad-Astra: Giselle Addon | `ad_astra` | 1.20.1 fabric / 6.19; 1.20.1 forge / 6.20; 1.21.1 fabric / 8.2; 1.21.1 neoforge / 8.1 |
| AE2 Wireless Terminals | `ae2` | 1.20.1 fabric / 15.2.1-fabric; 1.21.1 neoforge / 19.5.1; 26.1.2 neoforge / 26.1.2-beta; 26.2 neoforge / 26.2.0-beta; 26.3 neoforge / 26.3.1-beta |
| AE2 Wireless Terminals | `ae2`, `curios` | 1.20.1 forge / 15.3.3-forge |
| Aether Addon: Protect Your Moa | `aether` | 1.20.1 forge / 1.20.1-1.0.0-neoforge; 1.21.1 fabric / 1.21.1-1.0.0-fabric; 1.21.1 neoforge / 1.21.1-1.0.0-neoforge |
| Alloyed | `create` | 1.20.1 fabric / 2.1.5+1.20.1-fabric; 1.20.1 forge / 2.1.4+1.20.1-forge |
| Applied Botanics | `ae2`, `botania` | 1.20.1 fabric / 1.5.2-fabric; 1.20.1 forge / 1.5.2-forge; 1.21.1 neoforge / 1.6.0-alpha.3 |
| Botania | `curios` | 1.20.1 forge / 1.20.1-456-forge; 1.21.1 neoforge / 457-20261029.170837-24 |
| Botania | `trinkets` | 1.20.1 fabric / 1.20.1-456-fabric |
| Crabber's Delight | `farmersdelight` | 1.20.1 forge / 1.2.3; 1.21.1 neoforge / 1.3.1 |
| Create Big Cannons | `create` | 1.20.1 fabric / 5.11.4; 1.20.1 forge / 5.11.4; 1.21.1 neoforge / 5.11.7 |
| Create: Steam 'n' Rails | `create` | 1.20.1 fabric / 1.7.3+fabric-mc1.20.1; 1.20.1 forge / 1.7.3+forge-mc1.20.1 |
| Deep Aether | `aether` | 1.20.1 forge / 1.20.1-1.1.7; 1.21.1 neoforge / 1.21.1-1.1.5.1 |
| Expanded Delight | `farmersdelight` | 1.20.1 fabric / 0.3.2-fabric; 1.21.1 neoforge / 0.1.4-neoforge |
| Extended AE | `ae2` | 1.20.1 fabric / 1.20-1.0.2-fabric; 1.20.1 forge / 1.20-1.4.18-forge; 1.21.1 neoforge / 1.21-2.2.35-neoforge; 26.1.2 neoforge / 26.1-1.0.3-neoforge |
| L_Ender's Cataclysm | `curios` | 1.21.1 neoforge / 3.33 |
| ME Requester | `ae2` | 1.20.1 fabric / 1.20.1-1.1.4+fabric; 1.20.1 forge / 1.20.1-1.2.1+forge; 1.21.1 neoforge / 1.21.1-1.5.0+neoforge; 26.1.2 neoforge / 26.1.2-1.5.1+neoforge |
| MEGA Cells | `ae2` | 1.20.1 fabric / fabric-2.4.6; 1.20.1 forge / forge-2.4.6; 1.21.1 neoforge / 4.11.0 |
| Miner's Delight | `farmersdelight` | 1.20.1 forge / 1.20.1-1.4.5-backport; 1.21.1 neoforge / 1.4.5 |
| Sophisticated Backpacks | `sophisticatedcore` | 1.20.1 forge / 1.20.1-3.26.18.2237; 1.21.1 neoforge / 1.21.1-3.26.18.2235; 1.21.11 neoforge / 1.21.11-3.26.3.2168; 26.1.2 neoforge / 26.1.2-3.26.18.2238; 26.2 neoforge / 26.2-3.26.17.2234; 26.3 neoforge / 26.3-3.26.18.2236 |
| Sophisticated Backpacks (Unoffical Fabric port) | `sophisticatedcore` | 1.20.1 fabric / 1.20.1-3.23.4.5.110; 1.21.1 fabric / 1.21.1-3.23.4.3.106 |
| Sophisticated Storage | `sophisticatedcore` | 1.20.1 forge / 1.20.1-1.5.11.2210; 1.21.1 neoforge / 1.21.1-1.6.11.2211; 1.21.11 neoforge / 1.21.11-1.5.102.2142; 26.1.2 neoforge / 26.1.2-1.5.126.2209; 26.2 neoforge / 26.2-1.5.126.2207; 26.3 neoforge / 26.3-1.5.127.2208 |
| Sophisticated Storage (Unofficial Fabric port) | `sophisticatedcore` | 1.20.1 fabric / 1.20.1-1.3.5.11.142; 1.21.1 fabric / 1.21.1-1.3.7.9.139 |
| Steam 'n' Rails Neoforge | `create` | 1.21.1 neoforge / 0.3.0-beta.2+neoforge-mc1.21.1 |
| The Aether | `accessories` | 1.20.1 fabric / 1.20.1-1.5.2-beta.9-fabric; 1.21.1 fabric / 1.21.1-1.5.11-fabric |
| The Aether | `curios` | 1.20.1 forge / 1.20.1-1.5.2-neoforge |
| Things | `trinkets` | 1.20.1 fabric / 0.3.3+1.20 |

Inspected 561 exact mod/target pins and 2519 declared dependency edges. This is metadata documentation, separate from the current release gameplay campaign.
