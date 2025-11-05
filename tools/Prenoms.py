#!/usr/bin/env python3

import sys

'''
This script processes the Prenoms.txt
file and converts it to the Baby Name app format.
'''

input_path = "Prenoms.txt"
output_path = "converted_Prenoms.txt"

header = [
    "# Based on data produced by Mike Campbell and Boris New",
    "# See http://www.lexique.org/public/prenoms.php",
    "# License: GFDL-1.2-or-later"
]

def translateOrigin(origin):
    originMap = {
        '': 'Unknown',
        '?': 'Unknown',
        'african': 'Africa',
        'albanian': 'Albania',
        'ancient celtic': 'Ancient Celtic',
        #'ancient celtic (latinized)': 'ancient celtic (latinized)',
        'ancient egyptian': 'Ancient Egyptian',
        'ancient germanic': 'Ancient Germanic',
        #'ancient germanic (latinized)': 'ancient germanic (latinized)',
        'ancient greek': 'Ancient Greek',
        #'ancient greek (anglicized)': 'Ancient Greek (anglicized)',
        #'ancient greek (latinized)': 'Greece',
        'ancient roman': 'Ancient Rome',
        'ancient scandinavian': 'Ancient Scandinavia',
        'anglo-saxon': 'Anglo-Saxon',
        #'anglo-saxon (latinized)': 'anglo-saxon (latinized)',
        'anglo-saxon mythology': 'Anglo-Saxon Mythology',
        'arabic': 'Arabia/Persia',
        'armenian': 'Armenia',
        'astronomy': 'Astronomy',
        'basque': 'Basque',
        'biblical': 'biblical',
        #'biblical (original)': 'Bible',
        #'biblical (variant)': 'Bible',
        'breton': 'Bretagne',
        'bulgarian': 'Bulgaria',
        'catalan': 'Catalan',
        'celtic mythology': 'Celtic Mythology',
        #'celtic mythology (latinized)': 'Celtic Mythology (latinized)',
        'chinese': 'China',
        'cornish': 'Cornwall',
        'croatian': 'Croatia',
        'czech': 'Czech Republic',
        'danish': 'Denmark',
        'dutch': 'Netherlands',
        'egyptian mythology': 'Egyptian Mythology',
        #'egyptian mythology (hellenized)': 'egyptian mythology (hellenized)',
        'english': 'England',
        #'english (modern)': 'English',
        'esperanto': 'Esperanto',
        'estonian': 'Estonia',
        'far eastern mythology': 'Far Eastern Mythology',
        'finnish': 'Finland',
        'french': 'France',
        'frisian': 'East Frisian',
        'galician': 'Galicia',
        'german': 'Germany',
        'germanic mythology': 'Germanic Mythology',
        'greek': 'Greece',
        'greek mythology': 'Greek Mythology',
        #'greek mythology (anglicized)': 'Greek Mythology (anglicized)',
        #'greek mythology (latinized)': 'Greek Mythology',
        'hawaiian': 'Hawaii',
        'hindu mythology': 'Hindu Mythology',
        'history': 'History',
        'hungarian': 'Hungary',
        'icelandic': 'Iceland',
        'indian': 'India',
        'iranian': 'Iran',
        'irish': 'Ireland',
        'irish mythology': 'Irish Mythology',
        'italian': 'Italy',
        'japanese': 'Japan',
        'jewish': 'Israel',
        'judeo-christian legend': 'Judeo-Christian Legends',
        'khmer': 'Khmer',
        'korean': 'Korea',
        'late roman': 'Late Roman',
        'latvian': 'Latvia',
        'literature': 'Literature',
        'lithuanian': 'Lithuania',
        'macedonian': 'Macedonia',
        'manx': 'Isle of Man',
        'maori': 'Maori',
        'medieval english': 'England',
        'mormon': 'Mormon',
        'mythology': 'Mythology',
        'native american': 'Native American',
        'near eastern mythology': 'Near Eastern Mythology',
        #'near eastern mythology (hellenized)': 'Near Eastern Mythology',
        'new world mythology': 'New World Mythology',
        'norse mythology': 'Norse Mythology',
        'norwegian': 'Norway',
        'polish': 'Poland',
        'portuguese': 'Portugal',
        'provençal': 'Provençal',
        'roman mythology': 'Roman Mythology',
        'romanian': 'Romania',
        'russian': 'Russia',
        'scandinavian': 'Scandinavia',
        'scottish': 'Scotland',
        'serbian': 'Serbia',
        'slavic mythology': 'Slavic Mythology',
        'slovak': 'Slovak',
        'slovene': 'Slovene',
        'spanish': 'Spain',
        'swedish': 'Sweden',
        'thai': 'Thai',
        'theology': 'Theology',
        'turkish': 'Turkey',
        'ukrainian': 'Ukraine',
        'vietnamese': 'Vietnam',
        'welsh': 'Wales',
        'welsh mythology': 'Welsh Mythology'
    }

    if " (" in origin:
        origin = origin[0:origin.index(" (")]

    if origin.endswith(" mythology"):
        return [originMap[origin], "Mythology"]

    if origin.startswith("ancient "):
        return [originMap[origin], "Ancient"]

    return [originMap[origin]]

def parseOrigins(origins):
    ret = []
    for origin in origins.split(", "):
        ret.extend(translateOrigin(origin))

    # let's only keep Mythology/Ancient/.. names from this database
    return [x for x in ret if ("Mythology" in x or "Ancient" in x or "Literature" in x or "Astronomy" in x or "History" in x)]

def parseName(name):
    return name.replace(" (1)", "").replace(" (2)", "").replace(" (3)", "").replace("'", "").capitalize()

def translateGender(gender):
    if gender == "m":
        return "M"
    if gender == "f":
        return "F"
    if gender == "m,f" or gender == "f,m":
        return "?"
    print(gender)
    assert(0)

entries = {}

with open(input_path, encoding='iso-8859-1') as file:
    text = file.read()
    lines = text.splitlines()

    for num, line in enumerate(lines):
        if num == 0 or len(line) == 0 or line[0] == '#':
            continue

        toks = line.split("\t")
        assert(len(toks) == 4)
        name = parseName(toks[0].strip())
        assert(len(name) > 0)
        gender = translateGender(toks[1].strip())
        originNames = parseOrigins(toks[2].strip())
        frequency = toks[3].strip()

        if len(originNames) == 0:
            continue

        origins = []
        for originName in originNames:
            origins.append(f"{originName}:{gender}")

        if name in entries:
            entries[name].extend(origins)
        else:
            entries[name] = origins

'''
# Generate Kotlin Code

ori = set()
for name, origins in entries.items():
    for origin in origins:
        ori.add(origin.split(":")[0])

for o in ori:
    ident = o.lower().replace(" ", "_").replace("-", "_")
    print(f'<string name="origin_item_{ident}">{o}</string>')

for o in ori:
    ident = o.lower().replace(" ", "_")
    print(f'"{o}" -> R.string.origin_item_{ident}')
'''

for name in entries:
    # make origins distinct
    entries[name] = list(set(entries[name]))

def toLines(entries):
  lines = []
  for name, origins in entries.items():
    lines.append(name + ";" + ",".join(origins) + ";")
  return lines

with open(output_path, "w") as f:
  f.write("\n".join(header) + "\n")
  lines = toLines(entries)
  f.write("\n".join(lines))
  print(f"Wrote {output_path}")

