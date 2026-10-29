#!/bin/sh
# The sh code below runs the first Python it finds. Python skips it.
''':'
for python in python3 python python2; do
  command -v "$python" >/dev/null 2>&1 && exec "$python" "$0" "$@"
done
echo "No Python interpreter found." >&2
exit 1
'''

import os
import re
import socket
import sys


def eprint(msg):
    """Print to stderr"""
    sys.stderr.write("%s\n" % (msg))

def die(reason="None given", exitcode=1):
    eprint("[ERROR] %s" % (reason))
    sys.exit(exitcode)

def log_debug(message):
    eprint("[DEBUG] %s" % (message))

def cartesian_product(xs, ys):
    product = []
    for x in xs:
        for y in ys:
            product.append([x, y])
    return product

class Score:
    """A simple class to keep a score."""

    def __init__(self):
        self.good = 0
        self.total = 0

    def success(self):
        self.good += 1
        self.total += 1

    def failure(self):
        self.total += 1

    def state(self):
        return (self.good, self.total)

def hasPackage(name):
    """Check whether a package with the given name is installed."""
    eprint(name)

def hasService(name):
    """Check whether a service with the name is installed."""
    eprint(name)

def isPortOpen(port):
    """Check whether a process is listening on the given TCP port."""
    # A listener on all addresses also listens on the loopback address
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        try:
            s.connect(("127.0.0.1", int(port)))
            return 1
        except socket.error:
            return 0
    finally:
        s.close()

def checkIfFileExists(path):
    """Check whether a file exists at the given path."""
    return os.path.isfile(path)

def runCommand(command):
    """Run a given command and return its STDOUT output."""
    eprint(command)

def matchCommandOutput(command, match):
    """Run a given command and return whether or not its STDOUT matches param match."""
    stdout = os.popen(command).read().strip()
    log_debug("Matching %s to %s" % (stdout, match))
    return match in stdout

def checkProfile(profile):
    """Given a profile name, start checking it. Returns a tuple containing hits and total."""
    log_debug("Checking %s" % (profile))

    score = Score()

    filedirTypes = ["LOG", "DATA", "BINARY", "LIBRARY", "CONFIGURATION"]
    packagesKey = "%s_PACKAGES" % (profile)
    servicesKey = "%s_SERVICES" % (profile)
    portsKey = "%s_PORTS" % (profile)
    commandsIndexKey = "%s_COMMAND_INDEX" % (profile)

    # Check all possible file combinations
    for dirfileTuple in [("%s_%s_DIRECTORIES" % (profile, pfx), "%s_%s_FILES" % (profile, pfx))
                         for pfx in filedirTypes]:
        if (dirfileTuple[0] in env and dirfileTuple[1] in env):
            log_debug("Found entries in profiles for %s and %s, creating all possible entries." %
                      (dirfileTuple[0], dirfileTuple[1]))
            possibleLocationsPerFile = {}
            for file in env[dirfileTuple[1]].split(","):
                product = cartesian_product(env[dirfileTuple[0]].split(','), [file])
                possibleLocationsPerFile.update({file: map('/'.join, product)})

            for (file, locations) in possibleLocationsPerFile.items():
                anyMatched = 0
                for location in locations:
                    anyMatched |= checkIfFileExists(location)
                if anyMatched:
                    score.success()
                else:
                    score.failure()

    # Check all ports
    if portsKey in env:
        for port in env[portsKey].split(","):
            if isPortOpen(port):
                score.success()
            else:
                score.failure()

    # Check all commands
    if commandsIndexKey in env:
        for commandTuple in [("%s_COMMAND_%s_COMMAND" % (profile, pfx),
                              "%s_COMMAND_%s_MATCHES" % (profile, pfx))
                             for pfx in env[commandsIndexKey].split(",")]:
            if (commandTuple[0] in env and commandTuple[1] in env):
                log_debug("Running %s to see if it matches %s" % (commandTuple[0], commandTuple[1]))
                if matchCommandOutput(env[commandTuple[0]], env[commandTuple[1]]):
                    log_debug("success")
                    score.success()
                else:
                    score.failure()

    return score.state()

## main entry point ##

if len(sys.argv) < 2:
    die("No profile file passed to script.")

profiles = sys.argv[1]
propMatcher = re.compile(r'(\w+)="(.*)"')

env = {}

if not os.path.isfile(profiles):
    die("Profile %s doesn't exist." % (profiles))

f = open(profiles, 'r')
for line in f:
    match = propMatcher.match(line.strip())
    if match != None:
        env[match.group(1)] = match.group(2)
f.close()

PROFILES_INDEX = "PROFILES_INDEX"
if not PROFILES_INDEX in env:
    die("No %s in environment" % (PROFILES_INDEX))

# profiles are sorted to their depth in the tree meaning that more specific profiles
# come later in the list (e.g. Debian8 would be later than UnixLike)
profiles = env[PROFILES_INDEX].split(",")

scores = {}
for profile in profiles:
    if len(profile) > 0:
      score = checkProfile(profile)
      if score[1] > 0:
          scores[profile] = float(score[0])/score[1]
      else:
          scores[profile] = 0

# First create a new list of profile names that have the maximum score of 1, then
# map those profile names to a tuple containing their index in the profiles list + identity.
def idxProfileTuple(profile):
    return (profiles.index(profile[0]), profile)

def matches(item):
    return item[1] == 1

indexedMatches = [idxProfileTuple(item) for item in scores.items() if matches(item)]
# Get the most specific match based on its index in profiles

# get the profile with the highest score
def findMax(indexToProfile):
    maxIndex = -1
    max = None
    for t in indexToProfile:
        if t[0] > maxIndex:
            maxIndex = t[0]
            max = t[1]
    return max[0]

if len(indexedMatches) > 0:
    sys.stdout.write("%s\n" % (findMax(indexedMatches)))
else:
    log_debug("Found no profiles that match.")
    sys.exit(0)
