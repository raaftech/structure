#!/usr/bin/env python2

from __future__ import print_function
import sys, os, re, itertools, shlex, subprocess

def eprint(*args, **kwargs):
    """Print to stderr"""
    print(*args, file=sys.stderr, **kwargs)

def die(reason="None given", exitcode=1):
    eprint("[ERROR] {}".format(reason))
    exit(exitcode)

def log_debug(message):
    eprint("[DEBUG] {}".format(message))

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
    """Check whether a process is listening on the given port."""
    eprint(port)

def checkIfFileExists(path):
    """Check whether a file exists at the given path."""
    return os.path.isfile(path)

def runCommand(command):
    """Run a given command and return its STDOUT output."""
    eprint(command)

def matchCommandOutput(command, match):
    """Run a given command and return whether or not its STDOUT matches param match."""
    prog = re.compile(match)
    stdout = subprocess.check_output(shlex.split(command)).strip()
    log_debug("Matching {} to {}".format(stdout, match))
    return prog.match(stdout) is not None

def checkProfile(profile):
    """Given a profile name, start checking it. Returns a tuple containing hits and total."""
    log_debug("Checking {}".format(profile))

    score = Score()

    filedirTypes = ["LOG", "DATA", "BINARY", "LIBRARY", "CONFIGURATION"]
    packagesKey = "{}_PACKAGES".format(profile)
    servicesKey = "{}_SERVICES".format(profile)
    portsKey = "{}_PORTS".format(profile)
    commandsIndexKey = "{}_COMMAND_INDEX".format(profile)

    # Check all possible file combinations
    for dirfileTuple in [("{}_{}_DIRECTORIES".format(profile, pfx), "{}_{}_FILES".format(profile, pfx))
                         for pfx in filedirTypes]:
        if (dirfileTuple[0] in env and dirfileTuple[1] in env):
            log_debug("Found entries in profiles for {} and {}, creating all possible entries."
                      .format(dirfileTuple[0], dirfileTuple[1]))
            allPossibleFiles = map('/'.join, itertools.product(env[dirfileTuple[0]].split(","),
                                                               env[dirfileTuple[1]].split(",")))
            for possibleFile in allPossibleFiles:
                if checkIfFileExists(possibleFile):
                    score.success()
                else:
                    score.failure()

    # Check all commands
    if commandsIndexKey in env:
        for commandTuple in [("{}_COMMAND_{}_COMMAND".format(profile, pfx),
                              "{}_COMMAND_{}_MATCHES".format(profile, pfx))
                             for pfx in env[commandsIndexKey].split(",")]:
            if (commandTuple[0] in env and commandTuple[1] in env):
                log_debug("Running {} to see if it matches {}".format(commandTuple[0], commandTuple[1]))
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
propMatcher = re.compile('(\w+)="(.*)"')

env = {}

if not os.path.isfile(profiles):
    die("Profile {} doesn't exist.".format(profiles))

with open(profiles, 'r') as f:
    for line in f:
        match = propMatcher.match(line.strip())
        if match != None:
            env[match.group(1)] = match.group(2)

PROFILES_INDEX = "PROFILES_INDEX"
if not PROFILES_INDEX in env:
    die("No {} in environment".format(PROFILES_INDEX))

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
indexedMatches = map(lambda profile: (profiles.index(profile), profile),
                     {profile: score for profile, score in scores.items() if score == 1}.keys())

# Get the most specific match based on its index in profiles

# get the profile with the highest score
if len(indexedMatches) > 0:
    print(max(indexedMatches, key=(lambda k: k[0]))[1])
else:
    log_debug("Found no profiles that match.")
    exit(0)
