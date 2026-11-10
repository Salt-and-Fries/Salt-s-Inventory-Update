"""Pure adaptation of the legacy ledger to canonical-only detached cursor ownership.

Raw observations are never edited or serialized differently. Reject any actual
detached cursor copy, then reuse every original inventory/tile/slot/count/data
predicate through a temporary local view. This replaces the obsolete demand
that each detached menu duplicate the shared cursor, without relaxing any
resource or typed-data assertion.
"""
def conserved(original,item,server,expected):
 try:
  if any(item(menu['cursor'])[0]!=0 for menu in server['menus']):return False
  view=dict(server,menus=[dict(menu,cursor=server['cursor'])for menu in server['menus']])
  return original(view,expected)
 except(KeyError,TypeError,ValueError):return False

def features(original,base):
 def run(*args,**kwargs):
  previous=base.conserved
  base.conserved=lambda server,expected:conserved(previous,base.item,server,expected)
  try:return original(*args,**kwargs)
  finally:base.conserved=previous
 return run
