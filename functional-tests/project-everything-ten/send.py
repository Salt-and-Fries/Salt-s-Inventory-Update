"""Send one focused command to the excluded in-world observation fixture."""
from pathlib import Path
import argparse
import json
import time


def read(path):
    try:
        return json.loads(path.read_text())
    except (FileNotFoundError, json.JSONDecodeError):
        return {}


def send(output, kind, **arguments):
    sequence = max(read(output / "reply.json").get("sequence", -1),
                   read(output / "command.json").get("sequence", -1)) + 1
    request = {"sequence": sequence, "type": kind, "ticks": 8, **arguments}
    temporary = output / "command.tmp"
    temporary.write_text(json.dumps(request))
    # Windows can briefly deny replacement while the client reads the previous
    # command. Preserve atomic publication and retry that transient sharing race.
    for attempt in range(20):
        try:
            temporary.replace(output / "command.json")
            break
        except PermissionError:
            if attempt==19:raise
            time.sleep(.05)
    deadline = time.monotonic() + 50
    while time.monotonic() < deadline:
        response = read(output / "reply.json")
        if response.get("sequence") == sequence:
            if response.get("error") or response.get("probeError"):
                raise RuntimeError(response)
            return response
        time.sleep(.1)
    raise TimeoutError(request)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    parser.add_argument("type")
    parser.add_argument("arguments", nargs="?", default="{}")
    args = parser.parse_args()
    result = send(args.output, args.type, **json.loads(args.arguments))
    print(json.dumps(result, indent=2))
