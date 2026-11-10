"""Retain existing Waystones server snapshots without adding transport calls."""
from pathlib import Path
import hashlib, json

SCHEMA='waystone-existing-server-observations-v1'


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


class ExistingStateArchive:
    def __init__(self,profile,original):
        self.profile=Path(profile).resolve()
        self.original=original
        self.out=self.profile/'waystone-existing-observations'
        self.out.mkdir()
        self.records=[]

    def __call__(self,profile,kind,*args,**kwargs):
        assert Path(profile).resolve()==self.profile,'Transport must stay in this original run profile'
        result=self.original(profile,kind,*args,**kwargs)
        if kind=='regression-state':
            source=self.profile/'regression-state.json'
            raw=source.read_bytes()
            assert isinstance(json.loads(raw),dict)
            number=len(self.records)+1
            destination=self.out/(str(number).zfill(5)+'-server-state.json')
            with destination.open('xb')as stream:stream.write(raw)
            self.records.append(dict(ordinal=number,file=destination.name,sha256=sha(destination),
                command=kind,args=list(args),kwargs=kwargs))
            self.save()
        return result

    def save(self):
        (self.out/'index.json').write_text(json.dumps(dict(schema=SCHEMA,states=self.records),indent=2,allow_nan=False)+'\n')

    def finish(self):
        assert self.records,'Original workflow must have requested native server states'
        assert json.loads((self.out/'index.json').read_text())==dict(schema=SCHEMA,states=self.records)
        for record in self.records:assert sha(self.out/record['file'])==record['sha256']
        return len(self.records)
