#!/usr/bin/env python3
"""Wrap web/game.html (a page fragment) into the full offline page the Android app loads."""
import pathlib, re
root = pathlib.Path(__file__).resolve().parent.parent
frag = (root / "web" / "game.html").read_text(encoding="utf-8")
m = re.search(r"<title>.*?</title>", frag, re.S | re.I)
title = m.group(0) if m else "<title>Beat Tutor</title>"
body = frag.replace(title, "", 1)
page = (
    '<!doctype html><html lang="en"><head><meta charset="utf-8">'
    '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover,user-scalable=no">'
    '<meta name="theme-color" content="#100d1a">' + title +
    "<style>:root{color-scheme:dark}body{margin:0}img{max-width:100%}[hidden]{display:none!important}</style>"
    "</head><body>" + body + "</body></html>"
)
out = root / "app" / "src" / "main" / "assets" / "index.html"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(page, encoding="utf-8")
print("wrote", out.relative_to(root), len(page), "bytes")
