# AtlasGuide

## Overview

AtlasGuide is the in-game documentation viewer you are reading right now.
It displays markdown documents registered by any installed Atlas module.

## Opening the Guide

- Type `/guide` in chat
- Click the **GUIDE** button in the top-right player menu

## Searching

Use the search bar at the top of the topic list to filter by title.
Search is case-insensitive and matches any part of the title.

## For Server Admins

You can add or edit guide documents without recompiling any jar.
Drop `.md` files into:

```
mods/AtlasGuide/docs/
```

Documents placed in subdirectories are grouped by that subdirectory name.
For example, `docs/server/rules.md` appears under the **Server** category.

## Markdown Support

The guide renders standard markdown including:

- Headings (`#` through `######`)
- **Bold**, *italic*, ~~strikethrough~~, `inline code`
- Bullet and numbered lists
- Code blocks
- Tables
- Blockquotes
- Block sprite previews via `![block:BlockName]`
