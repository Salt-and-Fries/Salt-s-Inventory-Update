"""Author only version-local desktop documents; ordinary mod artwork stays native."""
from pathlib import Path
import json,zipfile
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'compatibility/ae2/versions/1.20.1/common/src/main/resources/assets/ae2/screens/desktop'
PUB=ROOT/'build/extended-ae-window-design/publishers'
HEIGHTS={'ex_pattern_provider':243,'ex_interface':251,'oversize_interface':251,'wireless_connector':251,'wireless_hub':251,'ingredient_buffer':192,'ex_drive':199,'ex_molecular_assembler':197,'ex_inscriber':187,'caner':170,'ex_io_port':166,'circuit_cutter':170,'ex_export_bus':251,'ex_import_bus':251,'active_formation_plane':251,'mod_export_bus':142,'mod_storage_bus':142,'precise_export_bus':251,'precise_storage_bus':256,'tag_export_bus':207,'tag_storage_bus':207,'threshold_export_bus':251,'threshold_level_emitter':184,'renamer':58,'pattern_modifier':192,'ex_emc_interface':251,'ex_emc_import_bus':251,'ex_emc_export_bus':251,'assembler_matrix':199}
TITLES={'ex_pattern_provider':'Pattern Provider','ex_interface':'Extended Interface','oversize_interface':'Oversize Interface','wireless_connector':'Wireless Connector','wireless_hub':'Wireless Hub','ingredient_buffer':'Ingredient Buffer','ex_drive':'Extended Drive','ex_molecular_assembler':'Molecular Assembler','ex_inscriber':'Extended Inscriber','caner':'ME Caner','ex_io_port':'Extended IO Port','circuit_cutter':'Circuit Slicer','ex_export_bus':'Extended Export Bus','ex_import_bus':'Extended Import Bus','active_formation_plane':'Active Formation Plane','mod_export_bus':'Mod Export Bus','mod_storage_bus':'Mod Storage Bus','precise_export_bus':'Precise Export Bus','precise_storage_bus':'Precise Storage Bus','tag_export_bus':'Tag Export Bus','tag_storage_bus':'Tag Storage Bus','threshold_export_bus':'Threshold Export Bus','threshold_level_emitter':'Threshold Emitter','renamer':'Rename','pattern_modifier':'Pattern Modifier','ex_emc_interface':'EMC Interface','ex_emc_import_bus':'EMC Import Bus','ex_emc_export_bus':'EMC Export Bus','assembler_matrix':'Assembler Matrix'}
def main():
 with zipfile.ZipFile(PUB/'ExtendedAE-1.20-1.4.18-forge.jar')as z:
  for name,h in HEIGHTS.items():
   doc={'includes':['../'+name+'.json'],'desktop':{'width':195 if name=='assembler_matrix'else 176,'height':h-(0 if name=='renamer'else 87)+(16 if name=='pattern_modifier'else 0),'resize':'fixed','showTitle':True,'preserveAeLayout':True,'inventoryCut':0 if name=='renamer'else 87},'text':{'dialog_title':{'text':{'translate':'gui.siu.extendedae.'+name},'position':{'left':8,'top':-4}}}}
   if name=='pattern_modifier':
    original=json.loads(z.read('assets/ae2/screens/pattern_modifier.json'));doc['slots']={}
    for semantic,p in original['slots'].items():
     if semantic not in ['PLAYER_INVENTORY','PLAYER_HOTBAR']:
      p=dict(p)
      if'top'in p:p['top']+=16
      doc['slots'][semantic]=p
   (DEST/(name+'.json')).write_text(json.dumps(doc,indent=2)+'\n')
 for name in ['ex_pattern_access_terminal','u_ex_pat']:
  doc={'includes':['../'+name+'.json'],'desktop':{'width':209,'height':129,'resize':'fixed','showTitle':True,'preserveAeLayout':True,'inventoryCut':91},'text':{'dialog_title':{'text':{'translate':'gui.siu.extendedae.pattern_access'},'position':{'left':21,'top':8}}}}
  (DEST/(name+'.json')).write_text(json.dumps(doc,indent=2)+'\n')
 doc={'includes':['../ex_crafting_terminal.json'],'desktop':{'width':195,'height':158,'resize':'grid','showTitle':True,'preserveAeLayout':True,'inventoryCut':91}}
 (DEST/'ex_crafting_terminal.json').write_text(json.dumps(doc,indent=2)+'\n')
 doc={'includes':['../set_precise_bus_amount.json'],'desktop':{'width':176,'height':107,'resize':'fixed','showTitle':True,'preserveAeLayout':True,'inventoryCut':0}}
 (DEST/'set_precise_bus_amount.json').write_text(json.dumps(doc,indent=2)+'\n')
 lang=ROOT/'compatibility/ae2/versions/1.20.1/common/src/main/resources/assets/salts_inventory_compat/lang/en_us.json';data=json.loads(lang.read_text())if lang.exists()else{}
 data.update({'gui.siu.extendedae.'+k:v for k,v in TITLES.items()});data['gui.siu.extendedae.pattern_access']='Pattern Access'
 for name in ['multiply','replace','clone']:data['gui.siu.extendedae.mode.'+name]=name.title()+' mode'
 lang.parent.mkdir(parents=True,exist_ok=True);lang.write_text(json.dumps(data,indent=2)+'\n');print('Wrote',len(HEIGHTS)+4,'version-local native desktop profiles')
if __name__=='__main__':main()
