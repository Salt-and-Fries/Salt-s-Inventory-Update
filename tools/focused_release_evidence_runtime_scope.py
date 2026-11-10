"""Describe the startup runner's real scope without granting new acceptance."""

PROJECT_STARTUP = 'functional-tests/scripts/Run-FunctionalMatrix.ps1'


def phase_binding(case, entry, artifact_identity):
    """A distribution hash guard is distinct from loaded packaged code."""
    result = dict(artifact_identity)
    if case['script'] != PROJECT_STARTUP:
        return result
    assert case['key'] == 'all:startup' and case['name'] == 'startup'
    assert case['minecraft'] is None and case['loader'] is None
    assert case['kinds'] == ['core'] and '-Runtime' in case['args']
    assert type(entry['exit_code']) is int and entry['exit_code'] == 0
    assert entry['runtime_evidence'] == {
        'scope': 'QA-source archive and outer process log; inner scope remains in the named runner evidence'}
    assert entry.get('qa_source_archive') and entry.get('log')
    result['distribution_identity_kind'] = result['execution_kind']
    result['execution_kind'] = 'project-runtime-initialization'
    result['runtime_scope'] = (
        'Original twelve runClient project runtimes and initialization summaries. '
        'The source archive and outer log are preserved. These runs do not '
        'establish that the packaged distribution jars were the loaded code sources.')
    result['artifact_binding_scope'] = (
        'Recorded distribution identities only; packaged Core gameplay and '
        'dedicated-server qualification retain their separate native evidence.')
    result['qa_source_archive'] = entry['qa_source_archive']
    result['original_process_log'] = entry['log']
    return result
