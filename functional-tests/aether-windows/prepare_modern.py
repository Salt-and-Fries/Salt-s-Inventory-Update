"""Original publisher mods and production SIU builds, with an excluded observer."""
from pathlib import Path
import importlib.util,json,sys
from prepare import enhance
ROOT=Path(__file__).resolve().parents[2]


def source(text):
    return enhance(text).replace('.save(new net.minecraft.nbt.CompoundTag())','.saveOptional(player().registryAccess())').replace('.saveWithFullMetadata()','.saveWithFullMetadata(player().registryAccess())')


def main():
    loader=sys.argv.pop(1)
    spec=importlib.util.spec_from_file_location('aether_modern',ROOT/'tools/prepare-rollout-production-modern.py')
    prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep);prep.MC='1.21.1'
    original=prep.qa_source
    if loader=='fabric':
        def enhanced(side):
            path,text=original(side);return path,source(text)
        prep.qa_source=enhanced;prep.main()
    else:
        folder=ROOT/'build/aether-ports/observer/com/siu_reference';folder.mkdir(parents=True,exist_ok=True)
        _,text=original('siu');(folder/'NativeUiReference.java').write_text(source(text).replace('net.minecraftforge.fml.ModList','net.neoforged.fml.ModList'))
        # The named NeoForge observer needs no intermediary-name aliases.
        (folder/'RuntimeNames.java').write_text((Path(__file__).with_name('RuntimeNames.java')).read_text())
        init=(Path(__file__).with_name('native.init.gradle')).read_text()
        (ROOT/'build/aether-ports/native.init.gradle').write_text(init)
        print('Prepared named NeoForge observer and isolated launch task.')


if __name__=='__main__':main()
