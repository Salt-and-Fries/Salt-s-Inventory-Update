"""Readable local compatibility matrix, kept distinct from exhaustive verification."""
from pathlib import Path
import html,json
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'output/all-target-rollout'
rows=json.loads((OUT/'availability-and-registration.json').read_text())
complete=all(r.get('inventory_ui_coverage') in ['complete','appendix'] for r in rows)
def esc(v):return html.escape(str(v),quote=True)
body=[]
for r in rows:
 count=r.get('paired_states_captured',0)
 loader_label={'fabric':'Fabric','forge':'Forge','neoforge':'NeoForge'}[r['loader']]
 cells=[r['project'],r['minecraft'],loader_label,r['upstream_version'],
        r.get('source_registration_route','Present' if r['source_registration'] else 'Needs review'),r['container_screen_candidates'],
        (f'{count} pictured states; inventory coverage complete' if complete and count else f'{count} pictured states') if count else r.get('ui_scope','Pending')]
 body.append('<tr data-mc="'+esc(r['minecraft'])+'" data-loader="'+esc(r['loader'])+'">'+
             ''.join('<td>'+('<a href="'+esc(r['release_url'])+'">'+esc(v)+'</a>' if i==3 else esc(v))+'</td>' for i,v in enumerate(cells))+'</tr>')
versions=list(dict.fromkeys(r['minecraft'] for r in rows))
page='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SIU - version and loader compatibility matrix</title>
<style>body{font:15px/1.5 system-ui,sans-serif;margin:32px;color:#183d40;background:#f6f9f9}main{max-width:1500px;margin:auto}h1{line-height:1.2}p{max-width:1050px}label{display:inline-block;margin:0 22px 20px 0}input,select{font:inherit;padding:8px;border:1px solid #b5cbcd;border-radius:4px}table{border-collapse:collapse;width:100%;background:white}th,td{text-align:left;padding:10px 12px;border-bottom:1px solid #dbe5e5}th{background:#183d40;color:white;position:sticky;top:0}tr:nth-child(even){background:#eef4f4}a{color:#146e73}.notice{border-left:4px solid #d49b32;padding:12px 18px;background:#fff7e6}#count{font-weight:600}</style>
<main><h1>SIU compatibility by version and loader</h1>
<p class="notice"><strong>Work in progress.</strong> Publisher availability, source registration and pictured UI states are separate checks. This matrix does not claim that all interfaces or features have passed runtime testing. The accompanying PDF records actual original/SIU screenshots.</p>
<p>The candidate column counts concrete container-screen classes found in the publisher binaries. It is not an exhaustive UI count: shared vanilla screens, dynamically themed interfaces and non-inventory configuration screens need individual review. A zero is not a claim that the mod has no UI.</p>
<p><a href="availability-and-registration.csv">Download the detailed CSV</a> | <a href="../pdf/siu-all-target-ui-verification.pdf">Open the working screenshot atlas</a></p>
<label>Mod <input id="search" type="search" placeholder="Find a mod"></label>
<label>Minecraft <select id="mc"><option value="">All versions</option>'''+''.join('<option>'+esc(v)+'</option>' for v in versions)+'''</select></label>
<label>Loader <select id="loader"><option value="">All loaders</option><option value="fabric">Fabric</option><option value="forge">Forge</option><option value="neoforge">NeoForge</option></select></label><p id="count"></p>
<table><thead><tr><th>Mod</th><th>Minecraft</th><th>Loader</th><th>Publisher release</th><th>Source registration</th><th>Screen candidates</th><th>Screenshot coverage</th></tr></thead><tbody>'''+''.join(body)+'''</tbody></table>
<script>const fields=['search','mc','loader'].map(id=>document.getElementById(id));function update(){const[q,mc,loader]=fields.map(f=>f.value.toLowerCase());let visible=0;document.querySelectorAll('tbody tr').forEach(row=>{const show=(!mc||row.dataset.mc===mc)&&(!loader||row.dataset.loader===loader)&&row.cells[0].textContent.toLowerCase().includes(q);row.hidden=!show;if(show)visible++});document.getElementById('count').textContent=visible+' available mod/version/loader combinations shown';}fields.forEach(f=>f.addEventListener('input',update));update();</script></main></html>'''
if complete:
 page=page.replace('<strong>Work in progress.</strong> Publisher availability, source registration and pictured UI states are separate checks. This matrix does not claim that all interfaces or features have passed runtime testing. The accompanying PDF records actual original/SIU screenshots.','<strong>Inventory UI comparison coverage complete.</strong> Every inventoried in-scope screen has an exact-publisher original/SIU comparison, with excluded interfaces accounted for in the appendix. Focused entry, layout and affected-control checks are recorded separately; the complete functional/gameplay suite was not run.')
 page=page.replace('Open the working screenshot atlas','Open the screenshot confirmation PDF')
(OUT/'compatibility-matrix.html').write_text(page,encoding='utf-8')
print('Wrote local matrix:',len(rows),'available combinations')
