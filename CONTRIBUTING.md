Contributing to Structure
=========================

When working on Structure there are a few rules to follow regarding the creation
of new issues, branches, merges, etc.
We have these rules to make sure we keep the repository clean and have a common
workflow for everyone working on Structure.

These rules are not cast in stone. Any changes or improvements can be discussed
either in person, or by creating an issue in
[Gitlab](https://gitlab.com/raaftech/structure/issues).

We have a few scripts that should be installed in your local Structure
repository. See
[hooks](https://gitlab.com/raaftech/scripts/tree/master/python/hooks).

Issues
------

Work on Structure always begins with an issue in the issue tracker. If there
is no issue for the work you want to do yet, please create one.

Note that we have one special type of issue, which is `epic`. These
issues are used to track separate, smaller issues which all work towards a
common goal, like a big feature.
An epic issue might be "Implement discovery", for instance.
Epics should only be created after they have been discussed. Otherwise keep
the issues small and manageable. This allows us to merge to master often.

When creating a new issue make sure the title of the issue is short and to the
point. The description field is for more detailed information and should always
be used.

Make sure to assign your issue to a person right away. If you don't know
who that person might be yet, assign the ticket to yourself. That way you'll
have a reminder that the issue will still have to be properly assigned.

Use the labels. If you're missing a label, create one!

Due date may also be used, but only if you came to an agreement about a due date
with the assignee.

Weights and milestones are not regularly used right now. We'll need to discuss
their use.

When a new issue has been created, it'll always be referred to by the ID Gitlab
generated for it.

Branches
--------

The Structure repository has three different types of branches, these are
the `master` branch, `issue` branches, and `release` branches.

### master

The `master` branch is considered stable, and should never point at a broken
commit. A broken commit is defined as a commit that breaks our tests, which can
be seen by the build status for a commit in Gitlab, or as a commit that breaks
functionality.
The latter is harder to detect, because we don't have thorough tests. Ideally
we will test everything in the future so broken commits are easier to detect.

Every commit in the `master` branch should start with the issue ID, followed by
a colon, and a short description, e.g. "#20: Added CONTRIBUTING documentation".
Please follow the following rules regarding commit messages:

* Start the subject with the issue ID in the form "#ID: "
* The commit subject should be about _what_ changed
* Keep the subject line short, 50-75 is ideal (`git log` doesn't line wrap)
* Capitalize the subject
* Do not end the subject with a period
* Line wrap the body at 80 characters
* The body should explain the _why+how_ of your commit

Because commits start with a '#', and this is the default character git uses
to denote a comment, you might have some trouble with the first rule.
This can be fixed using `git config --local core.commentchar \!`.

When you accidentally broke `master`, you're expected to clean up your broken
commit. If you use *squashed commits* (which you should, I'll come back to this
later) this is as simple as `git revert <hash>`. When you notice someone else
broke `master`, you might revert their commit and notify them, or if they broke
it more thoroughly ask them to fix it themselves.

### issue branches

When working on a particular issue, we'll always work in a specific issue branch
which should be named `issue/ID`. You are encouraged to commit and push often.
Breaking stuff in issue branches is totally fine, as they will never be released
directly.

The same rules for commit message apply in issue branches, except for the first
rule, which is changed to:

* Never start the subject with the issue master

### release branches

Release branches will be named `release/major.minor.patch`, following
[semantic versioning](http://semver.org)
is `master`. Issue branches may never be directly merged to release branches. In
a later stadium we might introduce `hotfix` branches, but not yet.

Release branches should be tagged with a signed tag, which we'll call "signing
off" going forward. Not everyone is allowed to sign off. This enables us to
decide on who may or may not release new versions to the public.

### merging from an issue branch to master

Issue branches will contain a lot of small commits and history. The history is
fine, but the small commits isn't for master. They make broken states in master
*way* harder to fix, because someone needs to figure out which commits depend on
each other, and it'll just lead to a big mess.

Introducing "merge commits". When you are done with your issue branch you can
merge it with master using a merge commit.

``` git checkout master git merge --squash issue/20 git commit ```

Please don't use `git commit -m` after a merge squash. If you use `git commit`
all the commit messages from your issue branch will be included in the new
commit body.

<img src="https://cloud.githubusercontent.com/assets/143418/14123752/eef5aef6-f5b6-11e5-81f5-466b3a4d3a3a.png" width="50%" />
