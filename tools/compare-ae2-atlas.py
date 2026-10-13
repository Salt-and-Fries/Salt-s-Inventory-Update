"""Read-only reference comparison of native atlas geometry, controls and pixels.

Pixel equality is reported, never inferred from geometry. World backgrounds,
hover state, live numeric data and changing inventory contents may differ.
"""
import argparse
import csv
import json
from pathlib import Path
from PIL import Image


def entries(folder, match_labels=False):
    rows = list(csv.DictReader((folder/'manifest.csv').open()))
    key = 'label' if match_labels else 'file'
    result = {row[key]: row for row in rows}
    if len(result) != len(rows):
        raise ValueError(f'Duplicate atlas {key}: {folder}')
    return result


def rectangle(folder, row, prefix):
    image = Image.open(folder/row['file']).convert('RGB')
    scale = image.width / int(row['guiWidth'])
    x, y, width, height = [int(row[prefix+key]) for key in ['X','Y','Width','Height']]
    return image.crop(tuple(round(value*scale) for value in (x,y,x+width,y+height)))


def pixel_comparison(expected, actual):
    size_matches = expected.size == actual.size
    equal = sum(a == b for a,b in zip(expected.get_flattened_data(),actual.get_flattened_data())) if size_matches else 0
    pixels = expected.width * expected.height
    return size_matches, 100*equal/pixels, size_matches and equal==pixels


def controls(folder, row):
    values = json.loads((folder/row['file'].replace('.png','.controls.json')).read_text())
    for value in values:
        value['x'] -= int(row['x']); value['y'] -= int(row['y'])
    return values


def compare(reference, actual, match_labels=False):
    expected, received = entries(reference,match_labels), entries(actual,match_labels)
    results = []
    for name in sorted(expected.keys() & received.keys()):
        wanted, current = expected[name], received[name]
        old_geometry = json.loads((reference/wanted['file'].replace('.png','.geometry.json')).read_text())
        new_geometry = json.loads((actual/current['file'].replace('.png','.geometry.json')).read_text())
        different_geometry = {key:{'reference':old_geometry.get(key),'actual':new_geometry.get(key)}
                              for key in old_geometry.keys() | new_geometry.keys() if old_geometry.get(key) != new_geometry.get(key)}
        old_casefold = {key.casefold(): value for key, value in old_geometry.items()}
        new_casefold = {key.casefold(): value for key, value in new_geometry.items()}
        casefold_geometry_matches = (len(old_casefold) == len(old_geometry)
                                     and len(new_casefold) == len(new_geometry)
                                     and old_casefold == new_casefold)
        size_matches, identity, identical = pixel_comparison(rectangle(reference,wanted,'body'),rectangle(actual,current,'body'))
        visual_size_matches, visual_identity, visual_identical = pixel_comparison(
            rectangle(reference,wanted,'visual'),rectangle(actual,current,'visual'))
        results.append({'file':wanted['file'],'actualFile':current['file'],'geometryMatches':not different_geometry,'geometryDifferences':different_geometry,
                        'geometryMatchesWithCaseInsensitiveKeys':casefold_geometry_matches,
                        'controlsMatch':controls(reference,wanted)==controls(actual,current),
                        'bodySizeMatches':size_matches,'bodyPixelIdentityPercent':identity,
                        'bodyPixelsIdentical':identical,
                        'visualSizeMatches':visual_size_matches,'visualPixelIdentityPercent':visual_identity,
                        'visualPixelsIdentical':visual_identical})
    return {'referencePresentations':len(expected),'capturedPresentations':len(received),
            'matching': 'label' if match_labels else 'file',
            'missingReferencePresentations':sorted(expected[key]['file'] for key in expected.keys()-received.keys()),
            'additionalProbes':sorted(received[key]['file'] for key in received.keys()-expected.keys()),
            'exactGeometryMatches':sum(row['geometryMatches'] for row in results),
            'caseInsensitiveGeometryMatches':sum(row['geometryMatchesWithCaseInsensitiveKeys'] for row in results),
            'exactControlMatches':sum(row['controlsMatch'] for row in results),
            'exactBodyPixelMatches':sum(row['bodyPixelsIdentical'] for row in results),
            'exactVisualPixelMatches':sum(row['visualPixelsIdentical'] for row in results),
            'fullPixelIdentityProven':len(results)==len(expected) and all(row['bodyPixelsIdentical'] and row['visualPixelsIdentical'] and row['controlsMatch'] and row['geometryMatches'] for row in results),
            'results':results}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--reference',type=Path,required=True)
    parser.add_argument('--actual',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--match-labels',action='store_true',help='Match focused captures to the full reference by unique presentation label; retain full reference coverage requirements.')
    args = parser.parse_args()
    if args.output.resolve().is_relative_to(args.reference.resolve()): raise ValueError('Reference directory is read-only')
    result = compare(args.reference,args.actual,args.match_labels)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({key:value for key,value in result.items() if key!='results'},indent=2))


if __name__ == '__main__': main()
