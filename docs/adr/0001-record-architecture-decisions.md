# 1. Record architecture decisions

- **Status:** accepted
- **Date:** 2026-10-19

## Context

This project is both a product for pottery studios and a portfolio piece. Decisions need to be
explainable later, to a contributor or in an interview, without reconstructing them from commits.

## Decision

Keep lightweight architecture decision records (ADRs) in `docs/adr`, one file per decision,
numbered in order. Each has a status, the context, the decision, its consequences, and a short
"In an interview" note: the one-paragraph version of the reasoning. A superseded ADR stays in
place, marked as superseded, with a link to its replacement.

## Consequences

- Writing the ADR is part of making a significant decision, not an afterthought.
- Planning documents (`docs/planning`) say *what* to build; ADRs say *why it's built this way*.
