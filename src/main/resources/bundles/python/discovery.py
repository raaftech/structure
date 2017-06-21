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
    os.path.isfile(path)

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
            log_debug("Trying {}".format(allPossibleFiles))
            for possibleFile in allPossibleFiles:
                if checkIfFileExists(possibleFile):
                    score.success()
                else:
                    score.failure()

    # Check all commands
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

profiles = env[PROFILES_INDEX].split(",")
scores = {}
for profile in profiles:
    score = checkProfile(profile)
    scores[profile] = float(score[0])/score[1]

# get the profile with the highest score
bestProfile = max(scores.keys(), key=(lambda k: scores[k]))

# if that highest score is actually more than 0, print it
if scores[bestProfile] > 0.0:
    print(bestProfile)
else:
    print()
