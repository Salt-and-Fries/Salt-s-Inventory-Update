"""Run the scoped archive-corruption and full-item-data oracle checks."""
from pathlib import Path
import argparse,datetime,hashlib,io,json,sys,unittest

ROOT=Path(__file__).resolve().parents[1]


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True);a=p.parse_args()
    assert a.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in a.id)
    output=ROOT/'output/release-readiness'/('helper-validator-tests-'+a.id+'.json');assert not output.exists()
    sources=[ROOT/p for p in ['tools/audit-release-metadata.py','tools/audit-release-upstream-dependencies.py','functional-tests/release-archive/test_upstream_dependency_declarations.py','tools/check-release-helper-validators.py',
        'tools/focused_release_evidence_preserved_qa.py',
        'tools/focused_release_evidence_toms_layout.py',
        'tools/focused_release_evidence_controller_frames.py',
        'functional-tests/release-archive/test_controller_frame_observation.py',
        'tools/focused_release_evidence_ae2_drive_gutter.py',
        'tools/focused_release_evidence_ae2_modern_drive.py',
        'tools/focused_release_evidence_runtime_scope.py',
        'functional-tests/release-archive/test_startup_runtime_scope.py',
        'functional-tests/release-archive/test_ae2_drive_gutter.py',
        'functional-tests/release-archive/test_atlas_observer_versions.py',
        'functional-tests/release-archive/test_final_rei_transition.py',
        'tools/reconcile-final-render-rei-delta.py',
        'tools/reconcile-rei-workspace-policy-api.py',
        'functional-tests/release-specialized/run_rei_workspace_policy.py',
        'functional-tests/release-specialized/rei_workspace_policy_acceptance.py',
        'functional-tests/release-specialized/test_rei_workspace_policy_acceptance.py',
        'functional-tests/release-archive/test_toms_native_layout.py',
        'tools/collect-release-qualification.py',
        'functional-tests/release-archive/test_preserved_measured_qa.py',
        'functional-tests/release-archive/test_hud_authoritative_observation.py',
        'functional-tests/src/main/java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java',
        'functional-tests/release-archive/test_metadata.py','functional-tests/release-archive/test_travelers_cursor_transition.py',
        'functional-tests/release-specialized/test_film_data.py',
        'functional-tests/release-specialized/test_chipped_ledger.py','functional-tests/release-specialized/film_rename.py',
        'functional-tests/release-specialized/chipped_features.py','functional-tests/release-specialized/create_codec_1211.py',
        'functional-tests/release-specialized/create_modern.py','functional-tests/release-specialized/test_create_codec_1211.py',
        'functional-tests/release-specialized/test_minecells_contract.py','functional-tests/release-specialized/minecells_features.py',
        'functional-tests/release-specialized/test_pouch_data.py','functional-tests/release-specialized/pouch_features.py',
        'functional-tests/release-specialized/test_mowzie_data.py','functional-tests/release-specialized/mowzie_features.py',
        'functional-tests/release-specialized/test_wands_data.py','functional-tests/release-specialized/wands_features.py',
        'functional-tests/release-specialized/test_backlog_discovery.py','functional-tests/release-specialized/test_backlog_latest_attempt.py',
        'tools/build-release-coverage-backlog.py',
        'functional-tests/release-specialized/test_analyzer_data.py','functional-tests/release-specialized/analyzer_features.py',
        'functional-tests/release-specialized/test_ministrosity_data.py','functional-tests/release-specialized/ministrosity_features.py',
        'functional-tests/release-specialized/test_reforge_data.py','functional-tests/release-specialized/reforge_features.py',
        'functional-tests/release-specialized/test_smitherz_data.py','functional-tests/release-specialized/smitherz_features.py',
        'functional-tests/release-archive/test_candidate_identity.py','tools/release_candidate_identity.py',
        'tools/activate-release-candidate.py','functional-tests/release-specialized/native_item_serialization.py',
        *['functional-tests/release-specialized/'+name+'_observer.py'for name in
          ['wands','guard','sensor','ministrosity','moa','reforge','smitherz','nifty']],
        *['functional-tests/release-specialized/'+name for name in
          ['test_ssn_filters_data.py','ssn_filters_features.py','ssn_filters_observer.py','run_ssn_filters.py','prepare_ssn_filters.py',
           'test_ssn_process_data.py','ssn_process_features.py','ssn_process_observer.py','run_ssn_process.py',
           'test_gallery_data.py','gallery_features.py','gallery_observer.py','run_gallery.py','prepare_gallery.py',
           'test_red_merchant_data.py','red_merchant_features.py','red_merchant_observer.py','run_red_merchant.py','prepare_red_merchant.py']],
        'functional-tests/release-archive/test_focused_evidence.py','tools/focused_release_evidence.py',
        'tools/focused_release_evidence_retention_policy.py','functional-tests/release-archive/test_publisher_retention_policy.py',
        'tools/focused_release_evidence_publisher_retention.py','functional-tests/release-archive/test_publisher_retained_evidence.py',
        'tools/reconcile-publisher-retention.py',
        'tools/seal-focused-release-evidence.py','tools/native_publisher_index.py',
        'functional-tests/release-specialized/test_native_publisher_index.py',
        *['functional-tests/release-specialized/'+name for name in
          ['run_techreborn_status.py','prepare_techreborn_status.py','techreborn_status_observer.py','techreborn_status_features.py']],
        'tools/native_selector_evidence.py','tools/reconcile-native-selector-release-evidence.py',
        'functional-tests/release-archive/test_native_selector_evidence.py',
        'tools/reconcile-toms-release-evidence.py','functional-tests/release-archive/test_toms_workflow_evidence.py',
        'tools/reconcile-waystone-release-evidence.py','functional-tests/release-archive/test_waystone_workflow_evidence.py',
        'tools/sensor_feature_evidence.py','tools/reconcile-sensor-release-evidence.py','functional-tests/release-archive/test_sensor_feature_evidence.py',
        'tools/focused_release_evidence_guard.py','tools/reconcile-guard-release-evidence.py','functional-tests/release-archive/test_guard_feature_evidence.py',
        'tools/focused_release_evidence_nifty.py','tools/reconcile-nifty-release-evidence.py','functional-tests/release-archive/test_nifty_feature_evidence.py',
        'tools/focused_release_evidence_analyzer.py','tools/reconcile-analyzer-release-evidence.py','functional-tests/release-archive/test_analyzer_feature_evidence.py',
        'tools/focused_release_evidence_pet_cargo.py','tools/reconcile-pet-cargo-release-evidence.py','functional-tests/release-archive/test_pet_cargo_evidence.py',
        'tools/reconcile-techreborn-status-release-evidence.py','functional-tests/release-archive/test_techreborn_status_evidence.py',
        'functional-tests/release-archive/test_techreborn_fixture_selection.py','functional-tests/release-specialized/run_techreborn_status_latest.py',
        'tools/reconcile-mi-energy-release-evidence.py','functional-tests/release-archive/test_mi_energy_workflow_evidence.py',
        'tools/reconcile-mi-storage-release-evidence.py','functional-tests/release-archive/test_mi_storage_workflow_evidence.py',
        'tools/reconcile-mi-infrastructure-release-evidence.py','functional-tests/release-archive/test_mi_infrastructure_evidence.py',
        'tools/reconcile-mi-multiblock-release-evidence.py','functional-tests/release-archive/test_mi_multiblock_evidence.py',
        'tools/reconcile-ae2-settings-release-evidence.py','functional-tests/release-archive/test_ae2_settings_evidence.py',
        'tools/reconcile-ae2-quantum-release-evidence.py','functional-tests/release-archive/test_ae2_quantum_evidence.py',
        'tools/reconcile-ae2-network-release-evidence.py','functional-tests/release-archive/test_ae2_network_evidence.py',
        'tools/reconcile-ae2-stock-release-evidence.py','functional-tests/release-archive/test_ae2_stock_evidence.py',
        'tools/reconcile-ae2-processing-release-evidence.py','functional-tests/release-archive/test_ae2_processing_evidence.py',
        'tools/reconcile-ae2-pattern-release-evidence.py','functional-tests/release-archive/test_ae2_pattern_evidence.py',
        'tools/reconcile-ae2-crafting-release-evidence.py','functional-tests/release-archive/test_ae2_crafting_evidence.py',
        'tools/reconcile-ae2-cpu-release-evidence.py','functional-tests/release-archive/test_ae2_cpu_evidence.py',
        'tools/focused_release_evidence_ae2_variants.py','functional-tests/release-archive/test_ae2_variant_evidence.py',
        *['functional-tests/release-specialized/'+v for v in ['ae2_legacy_publisher_variant.py','run_ae2_legacy_variant.py','run_ae2_cpu_controls_legacy_variant.py','test_ae2_legacy_publisher_variant.py']],
        'tools/reconcile-core-diagnostic-delta.py','functional-tests/release-archive/test_core_diagnostic_delta.py',
        'tools/reconcile-core-final-delta.py','functional-tests/release-archive/test_core_final_delta.py',
        *['functional-tests/release-specialized/'+v for v in ['enchanting_book_12111.py','run_core_enchanting_book.py','test_enchanting_book_12111.py']],
        'tools/focused_release_evidence_core_log_transition.py','tools/reconcile-core-log-evidence-transition.py','functional-tests/release-archive/test_core_log_transition.py',
        'tools/focused_release_evidence_core_log_consumer.py','functional-tests/release-archive/test_core_log_consumer.py',
        'tools/focused_release_evidence_publisher_final_core.py','functional-tests/release-archive/test_publisher_final_core.py',
        *['functional-tests/release-specialized/'+v for v in ['core_render_diagnostic.py','run_core_render_diagnostic.py','test_core_render_diagnostic.py']],
        *['functional-tests/release-specialized/'+v for v in
          ['ae2_cpu_controls_source.py','run_ae2_cpu_controls.py','test_ae2_cpu_controls_source.py']],
        *['functional-tests/release-specialized/'+v for v in
          ['ae2_cpu_status_features.py','ae2_cpu_status_observer.py','run_ae2_cpu_status.py','test_ae2_cpu_status_data.py']],
        'functional-tests/release-specialized/mi_storage_controls.py','functional-tests/release-specialized/test_mi_storage_controls.py',
        'functional-tests/release-archive/test_native_neo_provenance.py',
        *['functional-tests/release-specialized/'+name for name in
          ['native_neo_provenance.py','native_neo.py','mi_features.py','run_mi.py',
           'test_native_observation_schema.py','native_observation_schema.py','resume_release_publishers.py',
           'test_named_core_provenance.py','toms_all_observer.py','run_toms_all.py',
           'run_accessories_equipment.py','run_accessories_equipment_older.py','prepare_accessories_equipment.py',
           'accessories_equipment_observer.py','accessories_equipment_features.py','test_accessories_equipment_data.py',
           'run_native_only.py','prepare_native_only.py','native_only_features.py','native_only_observer.py','test_native_only_contracts.py',
           'run_mi_energy.py','prepare_mi_energy.py','mi_energy_features.py','mi_energy_observer.py','test_mi_energy_data.py',
           'run_mi_storage.py','prepare_mi_storage.py','mi_storage_features.py','mi_storage_observer.py','test_mi_storage_data.py',
           'run_mi_infrastructure.py','prepare_mi_infrastructure.py','mi_infrastructure_features.py','mi_infrastructure_observer.py','test_mi_infrastructure_data.py',
           'run_mi_multiblock.py','prepare_mi_multiblock.py','mi_multiblock_features.py','mi_multiblock_observer.py','test_mi_multiblock_data.py']],
        'tools/full_suite_provenance.py','tools/run-full-validation.py','functional-tests/release-archive/test_full_suite_provenance.py',
        *['functional-tests/release-specialized/'+v for v in
          ['ae2_child_observer.py','ae2_settings_features.py','prepare_ae2_settings.py','run_ae2_settings.py','test_ae2_settings_data.py',
           'ae2_quantum_observer.py','ae2_quantum_features.py','prepare_ae2_quantum.py','run_ae2_quantum.py','test_ae2_quantum_data.py',
           'ae2_network_observer.py','ae2_network_features.py','prepare_ae2_network.py','run_ae2_network.py','test_ae2_network_data.py',
           'ae2_pattern_observer.py','ae2_pattern_features.py','prepare_ae2_pattern.py','run_ae2_pattern.py','test_ae2_pattern_data.py',
           'ae2_stock_observer.py','ae2_stock_features.py','prepare_ae2_stock.py','run_ae2_stock.py','test_ae2_stock_data.py',
           'ae2_processing_observer.py','ae2_processing_features.py','prepare_ae2_processing.py','run_ae2_processing.py','test_ae2_processing_data.py',
           'ae2_crafting_observer.py','ae2_crafting_features.py','prepare_ae2_crafting.py','run_ae2_crafting.py','test_ae2_crafting_data.py',
           'requester_observer.py','requester_features.py','prepare_requester.py','run_requester.py','test_requester_data.py',
           'mega_workbench_observer.py','mega_workbench_features.py','run_mega_workbench.py','test_mega_workbench_data.py',
           'ae2_preferences_observer.py','ae2_preferences_features.py','prepare_ae2_preferences.py','run_ae2_preferences.py','test_ae2_preferences_data.py',
           'blue_arcs_observer.py','blue_arcs_features.py','prepare_blue_arcs.py','run_blue_arcs.py','test_blue_arcs_data.py',
           'requester_wireless_observer.py','requester_wireless_features.py','run_requester_wireless.py','test_requester_wireless_data.py',
           'mega_bulk_observer.py','mega_bulk_features.py','run_mega_bulk.py','test_mega_bulk_data.py',
           'lifecycle_observer.py','lifecycle_features.py','run_lifecycle.py','prepare_lifecycle.py','test_lifecycle_data.py',
           'waystone_modifier_entry.py','run_waystone_modifiers.py','test_waystone_modifier_entry.py',
           'waystone_observation_archive.py','test_waystone_observation_archive.py',
           'powah_legacy_observer.py','powah_legacy_features.py','run_powah_legacy.py','prepare_powah_legacy.py','test_powah_legacy_data.py',
           'powah_modern_observer.py','powah_modern_features.py','run_powah_modern.py','test_powah_modern_data.py',
           'toms_observed_features.py','run_toms_observed.py','test_toms_observed_features.py',
           'warp_plate_entry_fixture.py','test_warp_plate_entry_fixture.py']]]]
    hashes={str(p.relative_to(ROOT)):sha(p)for p in sources}
    integrity=json.loads((ROOT/'output/release-readiness/candidate-integrity.json').read_text())
    assert integrity['status']=='passed'and len(integrity['artifacts'])==36
    assert all(sha(ROOT/v['file'])==v['sha256']for v in integrity['artifacts'])
    suite=unittest.TestSuite()
    for folder in ['functional-tests/release-archive','functional-tests/release-specialized']:
        path=ROOT/folder;sys.path.insert(0,str(path))
        suite.addTests(unittest.TestLoader().discover(str(path),pattern='test_*.py',top_level_dir=str(path)))
    log=io.StringIO();result=unittest.TextTestRunner(stream=log,verbosity=2).run(suite)
    log_path=output.with_suffix('.txt');log_path.write_text(log.getvalue())
    assert {str(p.relative_to(ROOT)):sha(p)for p in sources}==hashes
    assert all(sha(ROOT/v['file'])==v['sha256']for v in integrity['artifacts'])
    record=dict(status='passed'if result.wasSuccessful()else'failed',tests=result.testsRun,
        failures=len(result.failures),errors=len(result.errors),skipped=len(result.skipped),
        generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),inputs=hashes,
        log=dict(file=str(log_path.relative_to(ROOT)),sha256=sha(log_path)),artifacts=integrity['artifacts'],
        scope='Offline validator mutation/data-oracle tests only. No live Minecraft menu or full gameplay acceptance is established by this run.')
    output.write_text(json.dumps(record,indent=2)+'\n');print(log.getvalue())
    assert result.wasSuccessful()and not result.skipped and result.testsRun>=31


if __name__=='__main__':main()
