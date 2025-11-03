#!/usr/bin/env python3

import pycountry
import sys

'''
This script processes the common-forenames-by-country.csv
file and converts it to the Baby Name app format.
'''

input_path = "common-forenames-by-country.csv"
output_path = "converted_common-forenames-by-country.csv"

# translate special char sequences to UTF-8
def mapNames(name):
    for key, value in translations.items():
        if key in name:
            name = name.replace(key, chr(value))
    return name

header = [
    "# https://sigpwned.com/2023/07/15/popular-names-by-country-dataset/",
    "# License: CC0-1.0"
]

countries = {}
for country in pycountry.countries:
    countries[country.alpha_2] = country.name

def translateCountry(code):
    return countries.get(code).split(",")[0]

def translateRegion(region):
    if region in ['', 'Excluding Basque Country & Catalonia']:
        return None

    if region == 'Arab world':
        return "Arab"

    return region

def translatePopulation(population):
    if population in ['', 'General population', 'Babies born', 'Babies born among Finnish speakers']:
        return None

    if population in ['Christian Arab boys' 'Christian Arab girls']:
        return "Christian Arab"

    if population in ['Druze boys', 'Druze girls']:
        return "Druze"

    if population in ['Muslim boys', 'Muslim girls']:
        return "Muslim"

    if population in ['Jewish boys', 'Jewish girls']:
        return "Jewish"

    return population

def mapNames(localized_name, romanized_name):
    if localized_name == romanized_name:
        return [localized_name]
    else:
        return [localized_name, romanized_name]

def mapOrigins(country, region, population):
    country = translateCountry(country)
    region = translateRegion(region)
    population = translatePopulation(population)
    ret = []
    if country:
        ret.append(country)
    if region:
        ret.append(region)
    if population:
        ret.append(population)
    return ret

entries = {}
alternatives = {}

with open(input_path) as file:
    text = file.read()
    lines = text.splitlines()

    for linenum, line in enumerate(lines):
        if linenum == 0 or len(line) == 0 or line[0] == '#':
            continue

        toks = line.split(",")
        assert(len(toks) == 12)

        country = toks[0].strip() # ['AD', 'AE', 'AL', 'AM', ...]
        country_group = toks[1].strip() # [1, 2, 3, 4]
        region = toks[2].strip() # ['', 'Dubai', 'Vienna', 'Québec', 'Basque Country', 'Catalonia', 'Excluding Basque Country & Catalonia', 'Faroe Islands', 'England', 'Wales', 'Northern Ireland', 'Scotland', 'Tahiti', 'Kama region', 'Arab world', 'Moscow']
        population = toks[3].strip() # ['', 'General population', 'Babies born', 'Coptic Christians', 'Babies born among Finnish speakers', 'Christian Arab boys', 'Christian Arab girls', 'Druze boys', 'Druze girls', 'Muslim boys', 'Muslim girls', 'Jewish boys', 'Jewish girls', 'Māori']
        note = toks[4].strip() # ['', 'Census', 'BabyCenter', 'Unofficial', 'Privately compiled', 'Official civil registry figures']
        year = toks[5].strip() #['2018', '2015', '2019', '2020', '2021', '2022', '2005', '2010', '', '2004', '2012', '2011', '2007', '2014', '2017', '2009', '2013', '1990']
        index = toks[7].strip() # ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22']
        #name_group = toks[8].strip()
        gender = toks[9].strip()
        localized_name = toks[10].strip()
        romanized_name = toks[11].strip()

        if romanized_name != localized_name:
            alternatives[romanized_name] = localized_name

        origins = []
        for originName in mapOrigins(country, region, population):
            origins.append(f"{originName}:{gender}")

        if romanized_name in entries:
            entries[romanized_name].extend(origins)
        else:
            entries[romanized_name] = origins


for name, origins in entries.items():
    # make sure origins are distinct
    entries[name] = list(set(origins))

def get_alternative(name):
    if name in alternatives:
        return "Romanized version of " + alternatives[name] 
    else:
        return ""

def toLines(entries):
  lines = []
  for name, origins in entries.items():
    lines.append(name + ";" + ",".join(origins) + ";" + get_alternative(name))
  return lines

with open(output_path, "w") as f:
  f.write("\n".join(header) + "\n")
  lines = toLines(entries)
  f.write("\n".join(lines))
  print(f"Wrote {output_path}")
