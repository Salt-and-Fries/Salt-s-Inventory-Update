"""Complete native terminal saved state in historical block-world fixtures.

Modern KeyTypeSelection.readFromNBT disables types absent from enabledKeyTypes.
The old atlas's id-only part compound therefore is not a default terminal. No
menu state or repository contents are edited by this normalization.
"""
import hashlib
from pathlib import Path
import re

SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
PART=re.compile(r'(\b(?:down|up|north|south|east|west)\s*:\s*\{\s*id\s*:\s*"ae2:(?:terminal|crafting_terminal|pattern_encoding_terminal)")(\s*\})')

def normalize(command,minecraft):
    if minecraft=='1.20.1':return command
    if not re.search(r'(?:^|\brun\s+)setblock\s+\S+\s+\S+\s+\S+\s+ae2:cable_bus\b',command):return command
    return PART.sub(lambda m:m[1]+',enabledKeyTypes:["ae2:i","ae2:f"]'+m[2],command)
