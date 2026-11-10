"""Invalid inside-block clicks must not mask genuine menu-opening behavior."""
import unittest
from native_input_geometry import correct_block_hits

class NativeHitGeometryTests(unittest.TestCase):
    def test_named_and_qualified_up_hits_land_on_the_top_face(self):
        for point in ['origin()','pos','POS']:
            for prefix,core in [('', ''),('net.minecraft.world.phys.','net.minecraft.core.')]:
                source=f'new {prefix}BlockHitResult({prefix}Vec3.atCenterOf({point}),{core}Direction.UP,{point},false)'
                result=correct_block_hits(source)
                self.assertEqual(source.replace(f'atCenterOf({point})',f'atCenterOf({point}).add(0,0.5,0)'),result)
    def test_authored_ray_hits_and_other_faces_are_not_rewritten(self):
        source='new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false); mc.gameMode.useItemOn(mc.player,hand,mc.hitResult);'
        self.assertEqual(source,correct_block_hits(source))
    def test_correction_is_idempotent(self):
        fixed='new BlockHitResult(Vec3.atCenterOf(origin()).add(0,0.5,0),Direction.UP,origin(),false)'
        self.assertEqual(fixed,correct_block_hits(fixed))

if __name__=='__main__':unittest.main()
