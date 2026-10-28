"""Regression for provided native recipe menus, mapped buttons and one-click toggling."""
import argparse
from run_processing import Processing,recipe_toggle


def visible(result):
    return next((w for w in result['client']['desktopWindows'] if w.get('recipeVisible')),None)


def run(loader,run_id):
    for copper in ([False,True] if loader=='forge' else [False]):
        r=Processing(loader,run_id,'recipe-books-'+('copper' if copper else 'cooking'))
        # Also capture the real cooking workflow against the corrected Core build.
        r.cooking(copper)
        r.command('close')
        r.fixture('miners_delight:copper_pot' if copper else 'farmersdelight:cooking_pot')
        result=r.command('inspect');b=result['client']['window']
        r.expect(visible(result) is None,'new window does not expose a stale native recipe panel')
        def toggle():return r.command('xy-click',**recipe_toggle(r.command('inspect')))
        result=toggle();book=visible(result)
        r.expect(book is not None,'first click opens the supplied recipe component')
        rb=book['recipeBounds']
        r.expect(rb['x']+rb['width']<=b['x'] or rb['x']>=b['x']+b['width']
                 or rb['y']+rb['height']<=b['y'] or rb['y']>=b['y']+b['height'],
                 'recipe panel avoids the machine and its in-window toggle')
        r.capture('recipe-book')
        result=toggle();r.expect(visible(result) is None,'second click closes the recipe panel')
        r.capture('book-closed')
        result=toggle();r.expect(visible(result) is not None,'third click reopens the recipe panel')
        r.capture('book-reopened')
        r.command('close');r.command('open');r.command('dismiss-help')
        result=r.command('inspect');b=result['client']['window']
        result=toggle();r.expect(visible(result) is not None,'first click also works after the window is closed and reopened')
        r.capture('reopened-window-book')
        print(loader,r.case,len(r.checks),'focused recipe checks passed',flush=True)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--loader',choices=['fabric','forge'],required=True);p.add_argument('--run',required=True)
    a=p.parse_args();run(a.loader,a.run)
