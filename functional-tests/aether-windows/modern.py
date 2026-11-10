"""Focused checks of the ported Aether interfaces, with the modern equipment backend."""
from pathlib import Path
import argparse
from character import CharacterCheck


def main():
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--loader',required=True,choices=['fabric','neoforge']);p.add_argument('phase',choices=['character','machines','lore']);a=p.parse_args()
    c=CharacterCheck(a.output)
    if a.phase=='character':
        # Aether now embeds Accessories on both loaders, rather than Curios.
        c.main('fabric')
        if a.loader=='neoforge':
            c.cloud();c.backpack_mode()
    else:getattr(c,a.phase)()


if __name__=='__main__':main()
