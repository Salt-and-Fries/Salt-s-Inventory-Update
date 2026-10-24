"""Seed and verify an isolated local Core-only dedicated server using its vanilla RCON interface."""
import argparse,json,re,socket,struct,time
from pathlib import Path

class Rcon:
    def __enter__(self):
        self.sock=socket.create_connection(('127.0.0.1',getattr(self,'port',25588)),timeout=10)
        self.send(1,3,getattr(self,'password','spatial-local-qa-only'))
        while True:
            ident,kind,_=self.receive()
            if ident==-1:raise RuntimeError('Local QA RCON authentication failed')
            if kind==2:break
        self.sock.settimeout(getattr(self,'command_timeout',10))
        return self
    def __exit__(self,*args):self.sock.close()
    def exact(self,n):
        data=b''
        while len(data)<n:
            part=self.sock.recv(n-len(data))
            if not part:raise EOFError('RCON disconnected')
            data+=part
        return data
    def send(self,ident,kind,text):
        body=struct.pack('<ii',ident,kind)+text.encode()+b'\0\0'
        self.sock.sendall(struct.pack('<i',len(body))+body)
    def receive(self):
        body=self.exact(struct.unpack('<i',self.exact(4))[0]);ident,kind=struct.unpack('<ii',body[:8])
        return ident,kind,body[8:-2].decode()
    def command(self,text):
        self.send(2,2,text)
        return self.receive()[2]

def wait(path,other):
    until=time.monotonic()+180
    while not path.exists():
        if other.exists():raise RuntimeError(other.read_text())
        if time.monotonic()>until:raise TimeoutError(str(path))
        time.sleep(.25)
    return json.loads(path.read_text())

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,required=True);parser.add_argument('--stop',action='store_true');args=parser.parse_args()
    if args.stop:
        with Rcon() as r:print(r.command('stop'))
    else:
        folder=args.output;folder.mkdir(parents=True,exist_ok=True)
        with Rcon() as r:
            r.command('whitelist off')
            r.command('fill -5 64 -5 5 64 5 minecraft:stone')
        connected=wait(folder/'connected.json',folder/'client-failure.json');name=connected['username']
        assert re.fullmatch(r'[A-Za-z0-9_]{1,16}',name)
        with Rcon() as r:
            r.command('fill -5 64 -5 5 64 5 minecraft:stone')
            r.command(f'tp {name} 0.5 65 0.5 0 0');r.command(f'clear {name}')
            r.command(f'item replace entity {name} inventory.0 with minecraft:dirt 32')
        client=wait(folder/'client-results.json',folder/'client-failure.json')
        with Rcon() as r:inventory=r.command(f'data get entity {name} Inventory')
        destination=re.search(r'\{[^{}]*Slot:\s*10b[^{}]*\}',inventory)
        assert destination and 'minecraft:dirt' in destination[0] and re.search(r'count:\s*32\b',destination[0]),inventory
        assert not re.search(r'Slot:\s*9b\b',inventory),inventory
        assert inventory.count('minecraft:dirt')==1,inventory
        (folder/'server-results.json').write_text(json.dumps({'passed':True,'spatial':connected['spatial'],'inventory':inventory,'client_checks':client['passed']},indent=2)+'\n')
        (folder/'server-verified').touch()
        print('Dedicated server confirmed one 32-item stack moved from inventory slot 9 to 10:',folder)
