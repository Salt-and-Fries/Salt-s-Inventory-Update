"""Account for specialized atlas entries without calling entry proof full coverage."""
def classify(gap,cases):
    target=gap['minecraft']+'/'+gap['loader']
    matches=[c for c in cases if c.get('target')==target
             and c.get('subjects_match_current_jars') is True
             and gap['screen'] in c.get('native_screens',[])
             and set(gap['publisher_sha256']).intersection(c.get('publisher_sha256',[]))]
    passed=[c for c in matches if c.get('status')=='passed']
    entered=[c for c in matches if c.get('status')=='entry-and-policy-only']
    chosen=(passed or entered or matches or [{}])[0]
    return {'target':target,'ui':gap['ui'],'screen':gap['screen'],
            'status':'recorded-scenario-passed' if passed else 'entry-only' if entered else 'attempted-not-passed' if matches else 'not-entered-on-current-subjects',
            'case':chosen.get('id',''),'coverage':chosen.get('coverage',chosen.get('reason','')),
            'report':chosen.get('report','')}
