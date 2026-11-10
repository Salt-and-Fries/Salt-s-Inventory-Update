"""Synthetic block-use callbacks must describe a point on their reported face."""
import re

def correct_block_hits(source):
    # A center point with Direction.UP is inside the block. MI's native
    # MachineOverlay rejects it as impossible. Keep authored ray hits intact.
    return re.sub(
        r'new ((?:net\.minecraft\.world\.phys\.)?BlockHitResult)\('
        r'((?:net\.minecraft\.world\.phys\.)?Vec3\.atCenterOf\((?:origin\(\)|POS|pos)\)),'
        r'\s*((?:net\.minecraft\.core\.)?Direction\.UP),',
        r'new \1(\2.add(0,0.5,0),\3,',source)
