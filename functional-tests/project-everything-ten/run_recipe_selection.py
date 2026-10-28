"""Use the original recipe-book search and buttons to fill actual machine inputs."""
import argparse
from run_processing import Processing,items,recipe_toggle
from run_recipe_books import visible


def run(loader,run_id,copper=False):
    r=Processing(loader,run_id,'recipe-selection-'+('copper' if copper else 'cooking'))
    r.case='copper-pot' if copper else 'cooking-pot'
    r.fixture('miners_delight:copper_pot' if copper else 'farmersdelight:cooking_pot')
    r.command('command',command='recipe give @a *')
    seed=items((9,'minecraft:carrot',8),(10,'minecraft:potato',8),(11,'minecraft:beetroot',8),
               (12,'farmersdelight:cabbage_leaf',8),(13,'miners_delight:copper_cup',8)) if copper else items((9,'farmersdelight:rice',8),(10,'minecraft:bowl',8))
    r.command('seed-player',items=seed)
    result=r.command('inspect');b=result['client']['window']
    result=r.command('xy-click',**recipe_toggle(result));rb=visible(result)['recipeBounds']
    r.command('xy-click',x=rb['x']+30+75,y=rb['y']+18)
    r.command('text',text='vegetable soup' if copper else 'rice')
    r.capture('recipe-search')
    # In these pinned releases the rice search has two unavailable mixed-rice
    # recipes before cooked rice. Copper's exact soup search has one result.
    column=0 if copper else 2
    result=r.command('xy-click',x=rb['x']+30+24+25*column,y=rb['y']+43)
    r.expect(any(s['count']>0 for s in result['server']['slots'][:4 if copper else 6]),
             'native recipe-book selection places the available ingredients')
    r.capture('recipe-selected')
    print(loader,r.case,'native recipe selection passed',flush=True)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--loader',choices=['fabric','forge'],required=True);p.add_argument('--run',required=True)
    p.add_argument('--copper',action='store_true');a=p.parse_args();run(a.loader,a.run,a.copper)
