"""Keep ordinary scripted clicks distinct from an intentional pickup-all."""
from pathlib import Path
import hashlib,time
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()


class SingleClickCadence:
    def __init__(self,dispatch,clock=time.monotonic,sleep=time.sleep):
        self.dispatch=dispatch;self.clock=clock;self.sleep=sleep
        self.previous=None;self.completed=0

    def send(self,command_type,**kw):
        kind=command_type
        key=None
        if kind in {'regression-slot-click','regression-player-click'}:
            key=(kw.get('session',0) if kind=='regression-slot-click' else 0,
                 kw['slot'],kw.get('button',0))
            # SIU's native double-click window is 250 ms. Start a second
            # ordinary left click at least 300 ms after the first completed;
            # this is conservative even if a server tick catches up quickly.
            if key==self.previous and key[2]==0:
                remaining=.300-(self.clock()-self.completed)
                if remaining>0:self.sleep(remaining)
        result=self.dispatch(kind,**kw)
        if key is not None:self.previous=key;self.completed=self.clock()
        return result
