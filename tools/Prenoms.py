#!/usr/bin/env python3

import sys

'''
This script processes the Prenoms.txt
file and converts it to the Baby Name app format.
'''

input_path = "Prenoms.txt"
output_path = "converted_Prenoms.txt"

# translate special char sequences to UTF-8
def translateName(name):
    for key, value in translations.items():
        if key in name:
            name = name.replace(key, chr(value))
    return name

header = [
    "# Based on data produced by Mike Campbell and Boris New",
    "# See http://www.lexique.org/public/prenoms.php",
    "# License: GFDL-1.2-or-later"
]

def translateOrigin(origin):
    originMap = {
        'english (modern)': 'English', 'finnish': 'Finland', 'english': 'English', 'spanish': 'Spain', 'biblical': 'Bible',
        'irish': 'Ireland', 'arabic': 'Arabia/Persia', 'jewish': 'Israel', 'hungarian': 'Hungary', 'french': 'France',
        'danish': 'Denmark', 'african': 'Africa', 'indian': 'India', 'german': 'Germany', 'biblical (variant)': 'Bible',
        'ancient greek (latinized)': 'Greece', 'greek mythology (latinized)': 'Greek mythology', 'italian': 'Italy',
        'swedish': 'Sweden', '': 'other', 'ancient germanic': 'Ancient Germanic', 'portuguese': 'Portugal', 'polish': 'Poland',
        'russian': 'Russia', 'romanian': 'Romania', 'dutch': 'Netherlands', 'turkish': 'Turkey', 'astronomy': 'Astronomy',
        'history': 'History', 'theology': 'Theology', 'greek mythology': 'Greek mythology', 'esperanto': 'Esperanto',
        'anglo-saxon': 'Anglo-Saxon', 'ancient roman': 'Ancient Roman', 'roman mythology': 'roman mythology', 'welsh': 'Wales',
        'welsh mythology': 'welsh mythology', 'ancient greek (anglicized)': 'Ancient Greek (anglicized)', 'greek': 'Greece',
        'ancient greek': 'ancient greek', 'czech': 'Czech Republic', 'slovene': 'Slovene', 'armenian': 'Armenia',
        'scandinavian': 'Scandinavia', 'latvian': 'Latvia', 'celtic mythology': 'celtic mythology', 'chinese': 'China',
        'near eastern mythology': 'near eastern mythology', 'native american': 'native american', 'japanese': 'Japan',
        'scottish': 'Scotland', 'irish mythology': 'rish mythology', 'basque': 'Basque', 'hawaiian': 'Hawaii', 'norwegian': 'Norway',
        'bulgarian': 'Bulgaria', 'macedonian': 'Macedonian', 'croatian': 'Croatia', 'serbian': 'Serbia', 'albanian': 'Albania',
        'ancient scandinavian': 'Ancient Scandinavian', 'iranian': 'Iran', 'norse mythology': 'norse mythology', 'slovak': 'slovak',
        'late roman': 'late roman', 'far eastern mythology': 'far eastern mythology', 'egyptian mythology': 'egyptian mythology',
        'ancient egyptian': 'Ancient Egyptian', 'egyptian mythology (hellenized)': 'egyptian mythology (hellenized)',
        'provençal': 'Provençal', 'catalan': 'Catalan', 'near eastern mythology (hellenized)': 'near eastern mythology (hellenized)',
        'literature': 'Literature', 'maori': 'Maori', 'ukrainian': 'Ukraine', 'icelandic': 'Iceland', 'breton': 'Breton',
        'lithuanian': 'Lithuanian', 'hindu mythology': 'hindu mythology', 'judeo-christian legend': 'Judeo-Christian Legends',
        'vietnamese': 'Vietnam', 'celtic mythology (latinized)': 'celtic mythology (latinized)', 'anglo-saxon mythology': 'anglo-saxon mythology',
        'manx': 'Manx', 'cornish': 'Cornish', 'ancient celtic (latinized)': 'ancient celtic (latinized)', 'khmer': 'Khmer',
        'ancient celtic': 'Ancient Celtic', 'germanic mythology': 'germanic mythology', 'slavic mythology': 'slavic mythology',
        'ancient germanic (latinized)': 'ancient germanic (latinized)', 'new world mythology': 'new world mythology', '?': 'other',
        'frisian': 'East Frisian', 'anglo-saxon (latinized)': 'anglo-saxon (latinized)',
        'greek mythology (anglicized)': 'greek mythology (anglicized)', 'korean': 'Korea', 'estonian': 'Estonia', 'thai': 'Thai',
        'medieval english': 'medieval english', 'mormon': 'Mormon', 'biblical (original)': 'biblical (original)', 'mythology': 'Mythology',
        'galician': 'Galician'
    }

    return originMap[origin]

def parseOrigins(origins):
    ret = []
    for origin in origins.split(", "):
        ret.append(translateOrigin(origin))
    return ret

def parseName(name):
    return name.replace(" (1)", "").replace(" (2)", "").replace(" (3)", "").capitalize()

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

        origins = []
        for originName in originNames:
            origins.append(f"{originName}:{gender}")

        if name in entries:
            entries[name].extend(origins)
        else:
            entries[name] = origins

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

