---
status: Living
updated_at: "2026-11-02"
---

# Domain Context — trimly

## Glossary

- disabled link — a short link the owner has switched off; visitors get "link not found" until the owner re-enables it. NOT a deleted link (the record and its code stay; it can be re-enabled).
- owner — the person who operates trimly and holds the owner credential; the only one who can create, list, disable and re-enable short links. NOT a visitor, and NOT a future invited user.
- short code — the 7-character public identifier that makes up the path of a short link. NOT the link's internal identifier, which is never shown publicly.
- short link — a short code bound to one target URL, created by the owner; opening it sends the visitor to the target URL. NOT the target URL itself.
- target URL — the long http/https address a short link sends visitors to. NOT the short link.
- visitor — anyone who opens a short link, without authenticating. NOT the owner.

## Invariants

- A short code is issued once and never points to a different target URL.
- A short link's target URL never changes after creation.
