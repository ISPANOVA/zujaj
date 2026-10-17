"""Builds the clock fonts used by live TextClocks.

Latin fonts: lining figures, subset to ASCII, and Arabic-Indic/Persian digit code points
mapped to the Latin digit glyphs (so a phone in Arabic still shows 123).
Arabic fonts: ASCII digits mapped to Arabic-Indic glyphs (so the clock always shows ١٢٣).
Run: python3 tools/gen_clock_fonts.py <google-fonts ofl dir> <res/font dir>
"""
import sys, os
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools import subset

OFL, OUT = sys.argv[1], sys.argv[2]
RES = OUT

LATIN = [
    # name, source, variable axes or None
    ("clk_label", f"{RES}/manrope_semibold.ttf", None),
    ("clk_manrope", f"{RES}/manrope_bold.ttf", None),
    ("clk_playfair", f"{RES}/playfair_bold.ttf", None),
    ("clk_bodoni", f"{RES}/bodoni_bold.ttf", None),
    ("clk_shoulders", f"{RES}/shoulders_medium.ttf", None),
    ("clk_oswald", f"{RES}/oswald_light.ttf", None),
    ("clk_anton", f"{OFL}/anton/Anton-Regular.ttf", None),
    ("clk_bebas", f"{OFL}/bebasneue/BebasNeue-Regular.ttf", None),
    ("clk_archivo", f"{OFL}/archivoblack/ArchivoBlack-Regular.ttf", None),
    ("clk_orbitron", f"{OFL}/orbitron/Orbitron[wght].ttf", {"wght": 600}),
    ("clk_michroma", f"{OFL}/michroma/Michroma-Regular.ttf", None),
    ("clk_dmserif", f"{OFL}/dmserifdisplay/DMSerifDisplay-Regular.ttf", None),
    ("clk_abril", f"{OFL}/abrilfatface/AbrilFatface-Regular.ttf", None),
    ("clk_poiret", f"{OFL}/poiretone/PoiretOne-Regular.ttf", None),
    ("clk_sixcaps", f"{OFL}/sixcaps/SixCaps.ttf", None),
    ("clk_righteous", f"{OFL}/righteous/Righteous-Regular.ttf", None),
    ("clk_comfortaa", f"{OFL}/comfortaa/Comfortaa[wght].ttf", {"wght": 700}),
    ("clk_unbounded", f"{OFL}/unbounded/Unbounded[wght].ttf", {"wght": 600}),
    ("clk_syne", f"{OFL}/syne/Syne[wght].ttf", {"wght": 700}),
]
ARABIC = [
    ("clk_ar_label", f"{RES}/cairo_bold.ttf"),
    ("clk_ar_cairo", f"{RES}/cairo_bold.ttf"),
    ("clk_ar_amiri", f"{RES}/amiri_bold.ttf"),
    ("clk_ar_rakkas", f"{RES}/rakkas.ttf"),
    ("clk_ar_kufi", f"{RES}/reem_kufi_medium.ttf"),
]

def lnum_map(f):
    """digit glyph -> lining glyph from the GSUB 'lnum' feature, if any."""
    out = {}
    if "GSUB" not in f: return out
    g = f["GSUB"].table
    if not g.FeatureList: return out
    for fr in g.FeatureList.FeatureRecord:
        if fr.FeatureTag != "lnum": continue
        for li in fr.Feature.LookupListIndex:
            lk = g.LookupList.Lookup[li]
            for st in lk.SubTable:
                if lk.LookupType == 7: st = st.ExtSubTable
                if hasattr(st, "mapping"): out.update(st.mapping)
    return out

def cmaps(f):
    return [t for t in f["cmap"].tables if t.isUnicode()]

for name, src, axes in LATIN:
    f = TTFont(src)
    if axes: f = instancer.instantiateVariableFont(f, axes)
    best = f.getBestCmap()
    lm = lnum_map(f)
    digits = {i: lm.get(best[0x30 + i], best[0x30 + i]) for i in range(10)}
    for t in cmaps(f):
        for i in range(10):
            t.cmap[0x30 + i] = digits[i]
            t.cmap[0x660 + i] = digits[i]
            t.cmap[0x6F0 + i] = digits[i]
    opts = subset.Options(); opts.layout_features = ["*"]; opts.name_IDs = ["*"]; opts.notdef_outline = True
    sub = subset.Subsetter(opts)
    sub.populate(unicodes=list(range(0x20, 0x7F)) + list(range(0x660, 0x66A)) + list(range(0x6F0, 0x6FA)))
    sub.subset(f)
    f.save(f"{OUT}/{name}.ttf")
    print(name, os.path.getsize(f"{OUT}/{name}.ttf"))

for name, src in ARABIC:
    f = TTFont(src)
    best = f.getBestCmap()
    for t in cmaps(f):
        for i in range(10):
            t.cmap[0x30 + i] = best[0x660 + i]
    f.save(f"{OUT}/{name}.ttf")
    print(name, os.path.getsize(f"{OUT}/{name}.ttf"))
