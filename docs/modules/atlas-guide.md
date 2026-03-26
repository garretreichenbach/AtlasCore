# AtlasGuide

`smd_resource_id: 10000` · Depends on AtlasCore (`9999`)

AtlasGuide provides an in-game markdown guide dialog using StarMade's native GUI rendering system.

## Features

- Markdown documents rendered natively with StarMade's GUI (no external libraries)
- Scrollable two-panel layout: document list on the left, rich content on the right
- In-game `/guide` command and keybind (`GUIDE` top-bar button)
- Documents loaded from the mod jar at startup — no filesystem writes required

## Adding Guide Documents

Place `.md` files in `guide/docs/markdown/`. At build time, Gradle syncs them to `src/main/resources/docs/` and generates `docs.index`.

```
guide/
  docs/markdown/
    my-guide.md         ← your content
    server-rules.md
```

The first `# Heading` in each file becomes the document's title in the list.

## Markdown Support

| Element | Syntax |
|---------|--------|
| Headings | `# H1`, `## H2`, `### H3` |
| Bold | `**text**` or `__text__` |
| Italic | `*text*` or `_text_` |
| Bold+Italic | `***text***` |
| Inline code | `` `code` `` |
| Code block | ` ```lang ` … ` ``` ` |
| Bullet list | `- item` or `* item` |
| Ordered list | `1. item` |
| Separator | `---` or `***` |

## Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/guide` | `/glossar`, `/glossary` | Open the guide dialog |
