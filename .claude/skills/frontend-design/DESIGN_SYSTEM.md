# NeoMud Design System — Stone & Torchlight

This document defines the visual language for all NeoMud client UI panels. Every new panel, dialog, or overlay MUST follow these conventions to maintain cohesion across the game.

## Aesthetic Direction

**Dark medieval forge** — worn leather, forged iron, torchlight gold. Every panel feels like a stone-framed parchment illuminated by firelight. The aesthetic evokes '90s RPG inventory screens (Diablo, Baldur's Gate) translated into Compose.

## Color Palette

### Core Structural Colors (StoneTheme.kt)
```
frameDark:    #1A1510  — deep brown, primary container backgrounds
frameMid:     #3A3228  — medium brown, stone frame borders
frameLight:   #5A5040  — light brown, bevel highlight edges
frameAccent:  #8B7355  — tan, accent detail
innerShadow:  #0D0A08  — near-black, depth/shadow edges
metalGold:    #AA8844  — brass/rivet color for corner accents
runeGlow:     #446644  — muted green, inner glow mystical accent
panelBg:      #12100E  — darkest, dialog backgrounds
textPanelBg:  #0D0B09  — log background
```

### Extended Panel Palette
```
DeepVoid:          #080604  — near-black void, deepest backgrounds
IronDark:          #0D0B09  — stone-dark, row backgrounds
WornLeather:       #1A1510  — warm brown, gradient endpoints
BurnishedGold:     #CCA855  — headers, primary accent text
TorchAmber:        #BBA060  — section headers, stat inscriptions
EmberOrange:       #AA6B3A  — damage stats, warning accent
FrostSteel:        #7090AA  — armor stats, cool accent
VerdantUpgrade:    #44CC55  — improvement indicators, positive deltas
CrimsonDowngrade:  #CC4444  — downgrade indicators, negative deltas
BoneWhite:         #D8CCAA  — neutral item/body text
AshGray:           #5A5040  — dim labels, slot names, divider text
SlotVoid:          #0A0806  — empty slot backgrounds
FilledSlotEdge:    #7A6545  — equipped slot border
EmptySlotEdge:     #2A2218  — empty slot border
HighlightGold:     #FFD700  — selection highlight, pulsing borders
```

### Coin Colors
```
Copper:   #CD7F32
Silver:   #C0C0C0
Gold:     #FFD700
Platinum: #E5E4E2
```

### Semantic Colors
```
HP bar:   Green (#4CAF50) > 50%, Orange (#FF9800) > 25%, Red (#F44336) < 25%
MP bar:   Blue (#448AFF)
XP bar:   BurnishedGold (#CCA855)
Spells:   Blue (#88AAFF text, #448AFF mana cost)
```

## Frame Pattern

Every major panel/dialog uses a **stone frame** drawn with `drawBehind`:

1. **Frame body**: 4px `frameMid` border on all sides
2. **Outer bevel**: 1px `frameLight` on top + left (highlight)
3. **Outer shadow**: 1px `innerShadow` on bottom + right
4. **Inner shadow**: 1px `innerShadow` at inner bottom + right
5. **Inner glow**: 1px `runeGlow` at inner top + left (mystical accent)
6. **Corner rivets**: 3px radius `metalGold` circles at all four corners

```kotlin
private fun DrawScope.drawStoneFrame(borderPx: Float) {
    // Frame body fill (all 4 sides)
    // Outer bevel highlight (top + left = light)
    // Outer bevel shadow (bottom + right = dark)
    // Inner edge shadow (bottom + right inside)
    // Inner glow — runic green (top + left inside)
    // Corner rivets — metalGold circles
}
```

### Panel Background
Inside the stone frame, use a vertical gradient:
```
WornLeather → #100E0B → DeepVoid → #100E0B → WornLeather
```

### Backdrop
Modal panels use `Color.Black.copy(alpha = 0.92f)` with padding `8.dp`.

## Header Pattern

Every panel header includes:
1. **Unicode icon + title** in `BurnishedGold`, 17sp, Bold
2. **Stone-beveled close button**: 26dp box with `frameLight→frameDark` gradient, beveled edges, `✕` in `BoneWhite`
3. **Gold ornamental line** below: 1dp horizontal gradient `Transparent → BurnishedGold(0.5f) → Transparent`

## Section Dividers

### Runic Divider
```
──── ✦ ────
```
Horizontal gradient lines fading from transparent to `AshGray(0.4f)`, with `✦` character in `AshGray(0.5f)` centered.

### Gold Line
Simple 1dp `BurnishedGold(0.5f)` horizontal gradient.

## Button Styles

### Stone Close Button
26dp box, `frameLight→frameDark` vertical gradient, beveled edges drawn with `drawBehind`.

### Action Buttons (Equip, Buy, Sell)
Drawn with `drawBehind` for beveled edges:
- **Positive/Upgrade**: `VerdantUpgrade` top → darker green bottom, green bevel highlight
- **Neutral**: Blue gradient (`#1565C0 → #0D47A1`), blue bevel highlight
- **Negative/Downgrade**: `EmberOrange` top → darker amber bottom
- **Cancel/Iron**: `frameLight→frameDark` gradient, `BoneWhite` text
- **Disabled**: `frameDark → #0D0A08` gradient, `AshGray` text

All buttons have:
- Top-left: light bevel (accent color at 0.5f alpha)
- Bottom-right: dark bevel (`Color.Black(0.5f)`)

### Stone Tabs (Vendor)
Same beveled treatment as buttons. Selected: `frameLight→frameMid` + `BurnishedGold` border highlight. Unselected: `frameDark→#0D0A08`.

## Item Row Pattern

Used in vendor lists and bag item lists:
- Background: `Brush.horizontalGradient(#14110E → DeepVoid)`
- Top edge: `AshGray(0.15f)` light line
- Bottom edge: `innerShadow` shadow line
- RoundedCornerShape(6.dp)
- 6dp horizontal, 4dp vertical padding

## Stat Display

- **Stat badges**: `DeepVoid` background, 1dp border at `color(0.3f)`, 3dp rounded
- **Stats grid**: Each stat in its own DeepVoid box with AshGray border, name in AshGray 10sp, value in BurnishedGold 14sp Bold
- **Stat inscriptions**: 8sp `TorchAmber` Bold below paperdoll slots
- **Delta indicators**: `▲+N` in VerdantUpgrade, `▼-N` in CrimsonDowngrade, `=` in AshGray

## Progress Bars (Vitals)

Custom-drawn bars (NOT Material3 LinearProgressIndicator):
- Track: `DeepVoid` background with `AshGray(0.3f)` 1dp border, 2dp rounded
- Fill: horizontal gradient from `color(0.7f)` → `color(1.0f)`, 2dp rounded
- Label: 12sp BrightText, 100dp width

## Typography

| Usage | Size | Weight | Color |
|-------|------|--------|-------|
| Panel title | 17sp | Bold | BurnishedGold |
| Section header | 14sp | Bold | TorchAmber |
| Item name | 13-14sp | Bold | BoneWhite (normal), HighlightGold (selected) |
| Body text | 12sp | Normal | BrightText (#CCCCCC) |
| Stat text | 10-11sp | Bold | TorchAmber |
| Dim labels | 10-11sp | Normal | AshGray |
| Inscriptions | 8-9sp | Bold | TorchAmber |

## Spacing Rhythm

- **Panel padding**: 10dp inside stone frame
- **Between major sections**: 10dp + RunicDivider + 10dp
- **Between items**: 3dp
- **Header to content**: 6dp + gold line + 6dp
- **Label to value**: 4dp

## Animation

- **Pulsing highlight**: `InfiniteTransition`, alpha 0.3f ↔ 1.0f, 800ms tween, `RepeatMode.Reverse` — used on paperdoll slot borders when a bag item targets that slot
- **Consumable flash**: `animateFloatAsState`, 100ms tween, white overlay at 0.4f alpha

## Paperdoll-Specific

- **Body silhouette**: Faint `AshGray(0.08f)` humanoid outline drawn with `drawBehind` — head circle, shoulders, torso, arms, legs
- **Vertical spine**: 1px `AshGray(0.15f)` center line
- **Empty slot icons**: Unicode placeholders (☖ head, ◇ neck, ⚔ weapon, ♦ chest, ◗ shield, ✋ hands, ○ ring, ┃ legs, ▭ feet) in `EmptySlotEdge(0.6f)`
- **Slot sizes**: 68dp chest, 58dp standard, 50dp neck/hands/feet, 44dp ring
- **Filled slot**: diagonal gradient `#1E1810 → #120E0A`, 1dp `FilledSlotEdge` border
- **Empty slot**: diagonal gradient `#0C0A08 → #060504`, 1dp `EmptySlotEdge` border
- **Item name below slot**: 9sp `BoneWhite` when equipped, slot label in `AshGray` when empty

## What NOT to Use

- Material3 `Button` / `TextButton` — use custom stone-beveled boxes
- Material3 `TabRow` — use custom `VendorTab` composable
- Material3 `LinearProgressIndicator` — use custom drawn bars
- Cyan (#55FFFF) for panel borders — that's the old terminal style
- Yellow (#FFFF55) for section headers — use TorchAmber
- Flat solid backgrounds — always use gradients for depth
- `Color(0xFF333333)` for disabled — use `frameDark → #0D0A08`
