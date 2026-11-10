"""Verify native fuel conversion before strict chunk-loader item round trips."""
from pathlib import Path
import hashlib
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()


def prime_chunk_loader(row,initial,act,expect,snapshot,total,cursor):
    row.setdefault('test_helper_sha256',{})[__name__]=SOURCE_SHA256
    item='minecraft:amethyst_shard';data=snapshot(item)
    menu=next(m for m in data['menus'] if m['type']=='net.additionz.block.screen.ChunkLoaderScreenHandler')
    sid=menu['session'];burn=data['nativeHost']['additionzBurn']
    expect('native chunk-loader fixture starts without upgrades or forced chunks',
           burn['remaining']==0 and burn['capacity']==0 and not burn['chunks'] and not burn['active'],native_burn=burn)
    before=total(data,item)
    act('regression-seed-player',items=[{'slot':9,'item':item,'count':1}],ticks=4)
    act('regression-player-click',slot=9,ticks=4)
    act('regression-slot-click',session=sid,slot=0,ticks=4)
    act('wait',ticks=20);data=snapshot(item);burn=data['nativeHost']['additionzBurn']
    expect('native chunk loader converts exactly one fuel into its real idle reserve',
           total(data,item)==before and cursor(data,item,0) and burn['remaining']>0
           and burn['capacity']==0 and not burn['chunks'] and not burn['active'],native_burn=burn)
    remaining=burn['remaining'];act('wait',ticks=20);burn=snapshot(item)['nativeHost']['additionzBurn']
    expect('idle chunk-loader reserve remains stable without enabled chunks',burn['remaining']==remaining and not burn['active'],native_burn=burn)
    row['native_recipe_checked']='One amethyst fuel converted by native ticks into a stable idle reserve, with zero upgrades/loaded chunks; subsequent exact-data transfers use native input cells. Chunk-loading topology is not certified.'
    return sid,0,item,7
