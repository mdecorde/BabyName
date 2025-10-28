#!/usr/bin/env python3

import sys

'''
Script to process 0717-182/nam_dict.txt from https://www.heise.de/ct/ftp/07/17/182/
License of data: GFDL-1.2-or-later
'''

if len(sys.argv) != 3:
    print(f"Usage: {sys.argv[0]} <input-file> <output-file>")
    print()
    print("This script processes 0717-182/nam_dict.txt")
    print("from https://www.heise.de/ct/ftp/07/17/182/")
    exit(1)

input_path = sys.argv[1]
output_path = sys.argv[2] 


# returns percent value
# 1 (= rare) to D (=13) => (16 * (2 ** (rarity - 13)) percent until next rarity level
# E.g. 7 means 7 means that the correspondig first name has an absolute frequency between 0.25% and 0.5%.
def get_rarity(c):
    return int(c, 16)

translations = {
    "<A/>": 256,
    "<a/>": 257,
    "<Â>": 258,
    "<â>": 259,
    "<A,>": 260,
    "<a,>": 261,
    "<C´>": 262,
    "<c´>": 263,
    "<C^>": 268,
    "<CH>": 268,
    "<c^>": 269,
    "<ch>": 269,
    "<d´>": 271,
    "<Ð>": 272,
    "<DJ>": 272,
    "<ð>": 273,
    "<dj>": 273,
    "<E/>": 274,
    "<e/>": 275,
    "<E°>": 278,
    "<e°>": 279,
    "<E,>": 280,
    "<e,>": 281,
    "<Ê>" : 282,
    "<ê>": 283,
    "<G^>": 286,
    "<g^>": 287,
    "<G,>": 290,
    "<g´>": 291,
    "<I/>": 298,
    "<i/>": 299,
    "<I°>": 304,
    "<i>": 305,
    "<IJ>": 306,
    "<ij>": 307,
    "<K,>": 310,
    "<k,>": 311,
    "<L,>": 315,
    "<l,>": 316,
    "<L´>": 317,
    "<l´>": 318,
    "<L/>": 321,
    "<l/>": 322,
    "<N,>": 325,
    "<n,>": 326,
    "<N^>": 327,
    "<n^>": 328,
    "<Ö>": 336,
    "<ö>": 337,
    "Œ": 338,
    "<OE>": 338,
    "œ": 339,
    "<oe>": 339,
    "<R^>": 344,
    "<r^>": 345,
    "<S,>": 350,
    "<s,>": 351,
    "Š": 352,
    "<S^>": 352,
    "<SCH>": 352,
    "<SH>": 352,
    "š": 353,
    "<s^>": 353,
    "<sch>": 353,
    "<sh>": 353,
    "<T,>": 354,
    "<t,>": 355,
    "<t´>": 357,
    "<U/>": 362,
    "<u/>": 363,
    "<U°>": 366,
    "<u°>": 367,
    "<U,>": 370,
    "<u,>": 371,
    "<Z°>": 379,
    "<z°>": 380,
    "<Z^>": 381,
    "<z^>": 382
}

originColums = ["Great Britain", "Ireland", "U.S.A.", "Italy", "Malta", "Portugal", "Spain", "France", "Belgium", "Luxembourg", "the Netherlands", "East Frisia", "Germany", "Austria", "Swiss", "Iceland", "Denmark", "Norway", "Sweden", "Finland", "Estonia", "Latvia", "Lithuania", "Poland", "Czech Republic", "Slovakia", "Hungary", "Romania", "Bulgaria", "Bosnia and Herzegovina", "Croatia", "Kosovo", "Macedonia", "Montenegro", "Serbia", "Slovenia", "Albania", "Greece", "Russia", "Belarus", "Moldova", "Ukraine", "Armenia", "Azerbaijan", "Georgia", "Kazakhstan/Uzbekistan,etc.", "Turkey", "Arabia/Persia", "Israel", "China", "India/Sri Lanka", "Japan", "Korea", "Vietnam", "other countries"]

def translateOrigin(origin):
    if origin == "other countries":
        return "other"
    return origin

def mapNames(name):
    if '+' in name:
        # E.g "Jun+Wei" represents the names "Jun-Wei", "Jun Wei" and "Junwei".
        ret = []
        ret.append(name.replace("+", "-"))
        ret.append(name.replace("+", " "))
        tokens = name.split("+")
        assert(len(tokens) == 2)
        ret.append(tokens[0] + tokens[1])
        return ret
    else:
        return [name]

# translate special char sequences to UTF-8
def translateName(name):
    for key, value in translations.items():
        if key in name:
            name = name.replace(key, chr(value))

    # fix broken encoding of š
    name = name.encode().replace(b"\xc2\x9a", b"\xc5\xa1").decode("utf-8")
    return name

header = [
    "# List of first names and gender from ftp://ftp.heise.de/pub/ct/listings/0717-182.zip",
    "# Copyright (c): 2007-2008: Jörg MICHAEL, Adalbert-Stifter-Str. 11, 30655 Hannover, Germany",
    "# License: GFDL-1.2-or-later",
    "###########################################################################################"
]

names = {}
references = {}

def hasDuplicateOrigins(origins1, origins2):
    originNames1 = list(map(lambda e: e.split(":")[0] + e.split(":")[1], origins1))
    originNames2 = list(map(lambda e: e.split(":")[0] + e.split(":")[1], origins2))

with open(input_path, encoding='iso-8859-1') as file:
    text = file.read()
    lines = text.splitlines()

    for line in lines:
        if len(line) == 0 or line[0] == '#':
            continue

        gender = line[0:3].strip()
        name = translateName(line[3:29].strip())
        sort = line[29]

        if sort == "+":
            continue

        if len(name) == 0:
            print(f"[Warning] empty line: {line}")
            continue

        origins = []
        originsSet = set()
        for i in range(0, len(originColums)):
            c = line[30 + i]
            if c != ' ':
                origin = originColums[i]
                rarity = get_rarity(c)
                if origin == "Kazakhstan/Uzbekistan,etc.":
                    for country in ["Kazakhstan", "Kyrgyzstan", "Tajikistan", "Turkmenistan", "Uzbekistan"]:
                        origins.append(f"{country}:{gender}:{rarity}")
                elif origin == "India/Sri Lanka":
                    for country in ["India", "Sri Lanka"]:
                        origins.append(f"{country}:{gender}:{rarity}")
                else:
                    origins.append(f"{translateOrigin(origin)}:{gender}:{rarity}")


        if gender != "=":
            for m in mapNames(name):
                if m in names:
                    if hasDuplicateOrigins(names[m], origins):
                        print(f"[Warning] conflicting origins for {m}: {names[m]} vs. {origins}")
                    names[m].extend(origins)
                else:
                    names[m] = list(origins)
        else:
            # short/long name information
            references[name] = origins


'''
for name, origins in references.items():
    def dupOriginsWithGender(origins, gender):
        ret = []
        for origin in origins:
            ret.append(origin.replace(":=:", f":{gender}:"))
        return ret

    def getGender(origins):
        gender = None
        for origin in origins:
            toks = origin.split(":")
            assert(len(toks) >= 2)
            if gender is None:
                gender = toks[1]
            else:
                assert(gender == toks[1])
        assert(gender is not None)
        return gender

    def mergeOrigins(existingOrigins, newOrigins):
        newOrigins = dupOriginsWithGender(newOrigins, getGender(existingOrigins))

        for origin1 in existingOrigins:
            originName1 = origin.split(":")[0]
            found = False
            for origin2 in newOrigins:
                originName2 = origin2.split(":")[0]
                if originName1 == originName2:
                    found = True
                    break
            if not found:
                print("new origin for {originName1}") 

        return newOrigins

    tok = name.split(" ")
    assert(len(tok) == 2)
    shortName = tok[0].strip()
    longName = tok[1].strip()

    shortNameOrigins = names[shortName]
    longNameOrigins = names[longName]

    if shortNameOrigins and longNameOrigins:
        assert(getGender(shortNameOrigins) == getGender(longNameOrigins))

    if shortNameOrigins:
        updatedOrigins = dupOriginsWithGender(origins, getGender(shortNameOrigins))
        #names[shortName] =
        mergeOrigins(shortNameOrigins, updatedOrigins)

    if longNameOrigins:
        updatedOrigins = dupOriginsWithGender(origins, getGender(longNameOrigins))
        #names[shortName] =
        mergeOrigins(longNameOrigins, updatedOrigins)
'''

def toLines(names):
  lines = []
  for name, origins in names.items():
    lines.append(name + ";" + ",".join(origins) + ";")
  return lines

with open(output_path, "w") as f:
  f.write("\n".join(header) + "\n")
  lines = toLines(names)
  f.write("\n".join(lines))
  print(f"Wrote {output_path}")
