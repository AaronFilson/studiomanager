# 09 — Kiln firing-log import: research

Item 4 in [08](08-wave2-queue.md). No controllers are connected yet, so we have no sample files. Everything
here comes from public web sources, checked 2026-11-02. **[C]** means a source says so. **[I]** means we
inferred it. Most sources are user forums rather than vendor manuals, and several vendor pages
(Ceramic Arts Daily threads) returned 403 when fetched, so we only saw their search snippets.

## What the app stores today

`firing` (V6): kiln_id, kind (BISQUE/GLAZE), cone, status, notes, created_at, started_at,
finished_at. `kiln` keeps total_firings, and V19 links firings to calendar bookings. There is no
column yet for peak temperature, duration, error codes or a temperature curve.

## Per vendor

| Vendor / controller | Exports logs? | How | Format | Notes |
|---|---|---|---|---|
| Bartlett Genesis (also on L&L, Paragon Firefly, others) | Yes [C] | Menu > Configuration > Export Log File. Newer firmware: Configuration > Communications > Export Log. The controller serves the file over local Wi-Fi at an IP address shown on screen, with a 4-digit code [C] | CSV [C] | Keeps the last ~10 firings [C]. Samples every ~30 s [C]. Per-zone output power as % on-time [C]. A single log takes 1–2 minutes to download, and pulling several at once can hang the controller [C]. Exact headers not found |
| Bartlett / Skutt KilnLink (cloud) | View only, as far as we found [C/I] | App and web portal show graphs, start and end times, amps, volts [C] | No documented file export or API found [I] | Needs a paid service plan [C]. The KilnLink app needs a Skutt KMT/GMT touchscreen controller [C] |
| Skutt KilnMaster (older keypad) | No [I] | — | — | Only the touchscreen KMT goes through KilnLink |
| Orton / TAP (SDS Industries; also sold by Evenheat and others) | Yes [C] | TAP Kiln Control Mobile app: Event Logs > Email All Logs. Original TAP controller (software 6.0+/7.0+, on Wi-Fi): Menu > Event Logs > Offload Logs, to email or a flash drive. Only possible in Standby [C] | Format not stated [C]. CSV is likely [I] | The app shows data points by the minute [C] |
| Orton Autofire / Express, Paragon Sentry, Evenheat Rampmaster / Set-Pro (non-TAP) | Not found | — | — | We found no export documentation. We assume these keypad controllers don't log [I] |
| Paragon app / Sentry Xpress | Not found | — | — | Needs the vendor manual |
| Blaauw | Some form of log [C] | Proprietary Windows PC software that logs data and serves a web page, for up to 8 kilns [C] | Unknown | Dated article. No export documentation found |

## Public APIs

We found none. KilnLink, the TAP cloud and Paragon publish no developer API, and KilnLink sits
behind a paid plan [C]. Any integration would need a partnership with the vendor [I]. File import
is the only route open now.

## What we can build now

1. **Reuse the wave2-import CSV pipeline** (upload, then column mapping, then a preview with
   per-row errors) with a new "kiln log" target. Only the shape differs: one file is **one
   firing**, so we summarise many rows instead of creating one record per row.
2. **A tolerant parser.** Accept comma, semicolon or tab separators, and look for the header
   row anywhere in the first ~20 lines, because controllers often put metadata lines before
   the header [I]. Map columns by name, ignoring case: time / elapsed, temperature (one per
   zone), setpoint, segment, % output, and an error/event column. Ask whether units are °F
   or °C, and guess from the peak temperature.
3. **Map to the firing.** Fill in started_at and finished_at, plus new fields: peak_temp,
   temp_unit, duration, error_code, and source (GENESIS/TAP/OTHER). Link the log to an
   existing firing by kiln and time overlap, or create a COMPLETE firing. Store the curve as
   a compressed series (or as the raw file in the photo/upload storage) so it can be graphed
   later.
4. **Saved mapping presets** for each controller type. Ship Genesis and TAP presets marked
   "unverified" until a real file confirms them.
5. Write tests against **hand-made fixtures** that follow the documented traits (30 s rows,
   zone % columns). Swap them for real files when a pilot sends some.

## Open questions

- What are the exact Genesis CSV headers and metadata lines? Do they change with firmware?
  What is the column the forum user asked Bartlett about?
- What file format do TAP's emailed logs use: CSV, PDF or HTML?
- Can KilnLink export anything at all? Ask Bartlett/Skutt about partner API access.
- Do Genesis logs record segment numbers and error codes (e.g. E-1, FTH)?
- Ask a pilot studio, or the Ceramic Arts Daily grapher author, for sample files.

## Sources

- https://community.ceramicartsdaily.org/topic/17103-downloading-firing-data-from-genesis-digital-controller/
- https://community.ceramicartsdaily.org/topic/24138-anyone-have-update-on-downloading-data-from-genesis-controller/
- https://community.ceramicartsdaily.org/topic/41821-bartlett-genesis-controller-data-export-problem
- https://community.ceramicartsdaily.org/topic/39241-building-a-genesis-log-file-grapher-website-for-me-and-community-need-feedback/
- https://help.kilncontrol.com/knowledge/export-logs
- https://help.kilncontrol.com/knowledge/download-firing-logs
- https://help.kilncontrol.com/knowledge/where-do-i-access-firing-logs
- https://apps.apple.com/us/app/-/id1084198187 (TAP Kiln Control Mobile)
- https://www.theceramicshop.com/product/11670/kiln-link-premium-1-yr-plan/
- https://apps.apple.com/mx/app/kilnlink/id1422985590
- https://skutt.com/skutt-resources/operation/
- https://www.ceramicindustry.com/articles/92788-web-based-kiln-monitoring (Blaauw)
- https://www.ceramicindustry.com/articles/95468-ll-kilns-optional-genesis-touchscreen-control
