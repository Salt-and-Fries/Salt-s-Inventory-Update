"""Exercise native Sawmill search, selection and both configured canvas widths."""
import argparse
import json
import re
from pathlib import Path
from run_processing import Processing, items


def run(loader, run_id):
    r=Processing(loader,run_id,"sawmill-variants")
    config=Path(f"versions/1.20.1/{loader}/build/runtime-smoke/chest-close-{run_id}/config/sawmill-common."+("json" if loader=="fabric" else "toml"))
    original=config.read_text()
    try:
        for wide in [False,True]:
            r.case="sawmill-wide" if wide else "sawmill-narrow"
            r.command("close")
            if loader=="fabric":
                content=json.loads(original)
                content["general"].update(wider_gui=wide,search_bar_mode="ON")
                config.write_text(json.dumps(content,indent=2)+"\n")
            else:
                content=re.sub(r'wider_gui = \w+',f'wider_gui = {str(wide).lower()}',original)
                config.write_text(re.sub(r'search_bar_mode = "\w+"','search_bar_mode = "ON"',content))
            r.command("reload-sawmill-config")
            r.fixture("sawmill:sawmill")
            r.command("seed",items=items((0,"minecraft:oak_log",32)))
            result=r.capture("search-visible")
            controls=result["client"]["allWindows"][0]["nativeControls"]
            r.expect(any(c["visible"] and "EditBox" in c["type"] for c in controls),"native search field is visible")
            b=result["client"]["window"]
            source_x=16 if wide else 20
            r.command("xy-click",x=b["x"]+8+60-source_x,y=b["y"]+24+5)
            r.command("text",text="chest")
            r.capture("search-filtered")
            r.command("xy-click",x=b["x"]+8+60-source_x,y=b["y"]+24+33-13)
            result=r.capture("search-result")
            output=result["server"]["slots"][1]["item"]
            r.expect("chest" in output,"filtered recipe selection produces a chest")
            r.take_output(1,output)
        print(f"{loader}: {len(r.checks)} focused Sawmill checks passed",flush=True)
    finally:
        r.command("close")
        config.write_text(original)
        r.command("reload-sawmill-config")


if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--loader",required=True,choices=["fabric","forge"])
    p.add_argument("--run",required=True)
    a=p.parse_args();run(a.loader,a.run)
