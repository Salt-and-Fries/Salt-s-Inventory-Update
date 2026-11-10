"""Clear only the disposable per-case work area before rebuilding its fixture."""
import hashlib
from pathlib import Path
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()

def commands(origin):
    x,y,z=origin
    return [f'fill {x-10} {y} {z-10} {x+10} {y+8} {z+10} minecraft:air',
            f'fill {x-10} {y-1} {z-10} {x+10} {y-1} {z+10} minecraft:stone']

def approach(origin):
    x,y,z=origin
    return f'tp @a {x-1.5:g} {y+1:g} {z+0.5:g} -90 46'


def water_walkway(origin):
    """A dry standing surface outside a trap's native 3x3 water pool."""
    x,y,z=origin
    return [f'fill {x-1} {y} {z+2} {x+1} {y} {z+4} minecraft:stone',
            f'tp @a {x+.5:g} {y+1:g} {z+3.5:g} 180 35']
