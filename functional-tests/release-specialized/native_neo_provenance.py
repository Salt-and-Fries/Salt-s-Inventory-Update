"""Make only the generated private native observer's evidence paths immutable."""
from pathlib import PurePosixPath


def private_init(text,run):
    assert run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in run)
    folder=PurePosixPath('build/release-readiness')/run/'native-observer'
    changes={
        "def generated=target.layout.buildDirectory.dir('native-reference-metadata').get().asFile":
            "def generated=root.file('"+str(folder/'generated-metadata')+"')",
        "destinationDirectory=target.layout.buildDirectory.dir('native-reference-artifact')":
            "destinationDirectory=root.layout.projectDirectory.dir('"+str(folder/'artifact')+"')",
    }
    for old,new in changes.items():
        assert text.count(old)==1,'Native observer evidence path contract changed'
        text=text.replace(old,new,1)
    anchor="new File(output,'staged-artifacts.json').text="
    assert text.count(anchor)==1,'Native observer staging contract changed'
    # The original task has already copied this actual built jar into mods.
    # Add its original path/hash to the ledger, not an invented later binary.
    text=text.replace(anchor,"inputs.add([file:qaJar.get().archiveFile.get().asFile,filename:'native-ui-reference.jar'])\n            "+anchor,1)
    return text
