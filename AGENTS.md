# CameraButton — agent rules

Shared rules and skills are gitignored links into agent-workflow (`bash ../agent-workflow/scripts/link-project.sh .`); app values live in `agent-kit.env`.

## Rules — `.agents/rules/`

| Rule | When to read |
| - | - |
| [project.md](.agents/rules/project.md) | *always* — this app's overrides of the shared rules and skills (theme, build, git, device notes); wins over them |
| [android-build.md](.agents/rules/android-build.md) | before calling any task done — the code must compile |
| [format.md](.agents/rules/format.md) | touching any Kotlin — `FORMAT_CHECK=0` here: match the file's style |
| [branch-pr-policy.md](.agents/rules/branch-pr-policy.md) | *always* — branches, commits and PRs (`GIT_FLOW` in `agent-kit.env`) |
| [artemis-mobile-testing.md](.agents/rules/artemis-mobile-testing.md) | *on demand* — using ARTEMIS MCP tools |
| [parallel-agents.md](.agents/rules/parallel-agents.md) | the user asks for agents in parallel worktrees |
| [compose-previews.md](.agents/rules/compose-previews.md) | *on demand* — rendering a `@Preview` (phone or Wear) to an image with `android studio render-compose-preview` |

## Skills — `.agents/skills/`

Shared: `android-device-test`, `compose-screenshot-test`, `material3-expressive`, `cmp-best-practices`, `fix-issue`, `artemis-key-rotation`, `workspace-cleanup`, `project-onboarding-setup`. Read a skill's `SKILL.md` when its task comes up.
