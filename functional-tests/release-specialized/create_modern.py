"""The pinned modern component encoding, with the same real native UI actions."""
from create_features import CurrentCreate
from create_codec_1211 import component_contains,fixture_command,schematic_stack


class ModernCreate(CurrentCreate):
    def call(self,kind,**args):
        if kind=='reviewed-session-seed'and args.get('snbt'):
            assert args['item']=='create:schematic'and args['slot']==0 and args['count']==1
            assert self.window()['menuId']=='create:schematicannon'and args['menu']==self.window()['menu']
            native=schematic_stack(args['snbt'])
            self.results.setdefault('native_fixture_codec_adaptations',[]).append(dict(
                original=args['snbt'],native=native,scope='Typed native schematic fixture in the already-open cannon; tested native controls remain unchanged.'))
            self.save();args={k:v for k,v in args.items()if k!='snbt'};args['native_stack']=native
        if kind=='command':
            original=args['command'];adapted=fixture_command(original)
            if adapted!=original:
                self.results.setdefault('native_fixture_codec_adaptations',[]).append(dict(
                    original=original,native=adapted,scope='Disposable native data fixture only; no tested input is replayed or callback invoked.'))
                self.save();args={**args,'command':adapted}
        return super().call(kind,**args)
    def assert_contains(self,actual,expected):
        result=component_contains(actual,expected)
        if result is None:return super().assert_contains(actual,expected)
        assert result,('Native 1.21.1 component expectation failed',expected,actual)
