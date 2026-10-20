---
name: feature-flow-with-git
description: Run one feature isolated in a branch plus worktree and finish with a PR. Wraps git environment setup, the feature-flow role loop (spec -> implement -> validate -> commit), and gated push + PR creation. Use when the user asks to run a feature with git isolation, inside a worktree, or to end with a pull request.
---

# Feature Flow With Git

This skill owns the git topology around a single feature: branch, worktree, and final PR. In the middle it delegates the whole role loop to `../feature-flow/SKILL.md`, which keeps owning spec, implementation, validation, acceptance persistence, and the Conventional Commit.

It is independent from `github-flow`: do not run both skills in the same execution. They share the same deterministic identities, so a duplicate request still resolves to reuse instead of new artifacts.

## Inputs

If any required input is missing, ask before mutating anything.

1. `feature_id`: from `feature_list.json`. If not provided, use the feature-flow selection rules (`in_progress` first, then the first dependency-ready feature).
2. `base_branch`: branch that work is based on and that the PR targets. Resolve from `git remote get-url origin` and confirm when unclear.
3. Push permission: granted per run by the user; the default is denied.

## Hard Rules

- One feature per execution. Never two features in the same run.
- All work branches from one common base SHA fixed at the start: `git fetch origin <base_branch>` then `base_sha=$(git rev-parse origin/<base_branch>)`.
- Reuse before creation. All lookups are read-only: `git worktree list`, `git branch --list "feature/<feature-id>"`, `gh pr list --state all --head "feature/<feature-id>" --base <base_branch>`. Create only when absent. Never delete or rewrite existing work: no worktree removal, branch deletion, force push, or PR closing.
- `feature-flow` owns the role loop, acceptance persistence, and commit. Subagents still never commit. Do not duplicate its rules here.
- Do not push without explicit push permission from the user for this run.
- Do not open or reuse a PR unless the push happened and the feature status is `accepted` in `feature_list.json`. `passing` is not accepted.
- Never merge a PR and never enable auto-merge.
- If a precondition fails or validation ends in `block`, stop and report before mutating anything further.

## Deterministic Identities (reuse keys)

- Branch: `feature/<feature-id>`.
- Worktree: `<worktrees-root>/<feature-id>` where `<worktrees-root>` is `../<base-checkout-dirname>-worktrees`, outside the base checkout.
- PR: head `feature/<feature-id>` into `base_branch`, title `[<feature-id>] <feature title from feature_list.json>`, body with the acceptance evidence.

## Flow

### 0. Preconditions (read-only)

1. Read `feature_list.json` and verify every id in `depends_on` has status `accepted`.
2. Verify the base checkout is clean: `git status --porcelain` has no staged, modified, or deleted tracked files (untracked files are allowed and reported), no merge/rebase in progress, and `base_branch` resolves.
3. If push permission was granted, verify `gh auth status` can manage PRs in the repository. Without permission, skip gh checks; the run stays local.
4. Resolve reuse targets for branch, worktree, and PR.

If any precondition fails, report and stop before mutating anything.

### 1. Git environment

1. Fix the common base SHA: `git fetch origin <base_branch>` and `base_sha=$(git rev-parse origin/<base_branch>)`.
2. Create the worktree and branch from `base_sha` only if absent:
   - Neither exists: `git worktree add <worktrees-root>/<feature-id> -b feature/<feature-id> <base_sha>`
   - Branch exists, worktree missing: `git worktree add <worktrees-root>/<feature-id> feature/<feature-id>`
   - Worktree exists: reuse it as-is; do not recreate or move it.
3. Move this session into the worktree with `session_move` so planner, implementer, and validator run inside the isolated worktree. Report the new directory.

### 2. Feature flow

Invoke `../feature-flow/SKILL.md` for the selected `feature_id` only, until it reaches `accept` or `block`, working inside the worktree. It owns:

- spec creation or refresh,
- implementation and self-verification,
- independent validation and the repair loop,
- acceptance persistence in `feature_list.json`,
- the Conventional Commit,
- the user-facing handoff.

Collect its outcome: verdict, commit hash and subject, and evidence summary. If it stops at `block` or without acceptance, skip step 3 and report with `pr: none`.

### 3. Publish (push gate)

1. If push permission was not explicitly granted, stop: report the local commit and `pr: none`. Without permission there is no push, and without push there is no PR.
2. Verify the feature status is `accepted` in `feature_list.json`. If not, refuse with `pr: none`.
3. Push the branch: `git push -u origin feature/<feature-id>`.
4. Reuse the existing PR when `gh pr list --state all --head "feature/<feature-id>" --base <base_branch>` returns one; otherwise create it with `gh pr create --base <base_branch> --head feature/<feature-id> --title "[<feature-id>] <feature title>"` and the acceptance evidence in the body.
5. Never merge. Report the PR URL.

## Output Report

Header: feature id, repository, base branch, base SHA, push permission (granted/denied), reused vs created artifacts, worktree directory.

Per run, one block with exactly these fields:

- `feature`: feature id.
- `session`: this session running in the worktree directory, or `none` before the move.
- `branch`: `feature/<id> @ <base_sha>`.
- `status`: feature status or gate verdict: `blocked`, `in_progress`, `passing`, `accepted`, `published`, `refused`.
- `commit`: hash and subject, or `none`.
- `evidence`: verification or acceptance summary.
- `pr`: PR URL when a PR exists or was published, otherwise `none`.
