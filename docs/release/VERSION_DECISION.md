# Candidate version decision

The selected candidate uses the repository's consistent versions:

| Module | Candidate version |
| --- | --- |
| Core | 0.1.6 |
| Compatibility | 0.1.3 |
| Controlify companion | 0.1.1 |

These names identify the candidate under qualification, not a publication made
by this task. Exact SHA-256 values, source state and accepted test subjects must
be recorded in the final manifest before approval. Earlier development artifacts
with the same filenames are superseded by that manifest; filename equality is
not evidence of byte equality or acceptance.

Keep the three modules from the same accepted candidate. Their metadata must
require the corresponding Core version and the exact Minecraft/loader target.
Existing exported API signatures are retained, with additive contracts, API
version 6 and common wire protocol 5. Integration capability advertisement and
handshake checks remain authoritative for available features.

The read-only [public Core version audit](../../output/release-readiness/public-core-version-audit.json)
queried Modrinth's project version endpoint on November 6, 2026. Its latest
published Core version was 0.1.5; Core 0.1.6 was absent from that response. The
[public module search](../../output/release-readiness/public-module-search.json)
located the Core project and did not locate separate companion projects. A
search omission does not prove that no such project or release exists.
Recheck destination slots immediately before any separately authorized
publication, including GitHub or other destinations. These observations do not
reserve version numbers or certify other publication sites.

If publishing to an existing public version slot would replace previously
published files, choose fresh module versions and regenerate the artifact,
dependency and acceptance records before publishing. This qualification task
does not overwrite a release, create a tag, push commits or publish files.
