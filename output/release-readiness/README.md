# Release qualification records

The files committed here are compact summaries of the approved Core 0.1.6,
Compatibility 0.1.3 and Controlify 0.1.1 candidate: 36 jars across 12 targets.
The independent review approved the 69 original requirements within their
documented scopes; its separate review gate completed the 70-item goal.

- [Final verification](../../docs/release/FINAL_VERIFICATION.md)
- [Release manifest](release-manifest-final.json)
- [Independent review](independent-review-final-01/review.md)
- [Original requirements and completion evidence](requirements-final.json)
- [Coverage table](coverage-backlog.csv)

The complete local qualification archive includes roughly 16.5 GB of generated
reports, preserved runtimes, native observations and private QA/source bundles.
Those files remain on the qualification machine under `output/` and `build/`;
they are excluded from Git. Some detailed evidence links in these summaries
therefore require that local archive and are not available in a fresh clone.
Historical-evidence reader tests also require their named original archives.

Hashes, workspace paths and Git metadata in the records identify the original
reviewed subjects. The review occurred before the subsequent source commit;
its immutable reviewed manifest is retained alongside the approval. Committing
the source does not relabel those older test executions or publish mod releases.
Built jars remain in the ignored `dist/` directory for a separately authorized
release upload. No raw upstream mod jars, saved worlds or private runtime
archives are included in this source commit.
