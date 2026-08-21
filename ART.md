# Artwork provenance

Every drawing bundled with EncounterDeck is **public domain or CC0**. Neither
imposes an attribution requirement, so the app ships no artwork credit and the
only licence notice on screen stays the SRD 5.1 one, which *is* required. This
file is courtesy and a paper trail, not a licence obligation.

Two sources:

- **LadyofHats** (`dnd_*`) - creature illustrations released under CC0 on
  Wikimedia Commons. Drawn for D&D subjects, so they cover the fantasy monsters
  no historical source has.
- **Pearson Scott Foresman** (`*_psf`) - an educational-illustration archive the
  publisher donated to the public domain. Clean isolated line art of real
  animals.

## Processing

The sources are black line art on white paper. Luminance is inverted into the
alpha channel, so ink becomes opaque and paper transparent, and RGB is
flattened to white; the app then tints the result to the current theme colour.
That is why the art suits a dark UI and carries no white plate behind it.

## Curation

Coverage is partial on purpose: 152 of 334 monsters. Automated matching was not
trustworthy enough to ship unreviewed - Commons matches words, not meaning, and
offered a gold coin for *Noble*, a garment fastening for *Frog*, a writing
script for *Deva*, and a machine part for *Planetar*. Every bundled image was
reviewed by eye. Rejections are recorded with reasons in `scripts/gen_art.py`
so a regeneration cannot quietly reintroduce them.

Monsters with no confident match have no art, and the detail screen falls back
to its text layout.

## Files

| file | licence | artist | source |
|---|---|---|---|
| `baboon_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Baboon_(PSF).png) |
| `bactrian_camel_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Bactrian_Camel_(PSF).png) |
| `badger_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Badger_(PSF).png) |
| `bald_eagle_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Bald_Eagle_(PSF).png) |
| `barbary_ape_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Barbary_Ape_(PSF).png) |
| `beetle_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Beetle_(PSF).svg) |
| `black_bear_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Black_bear_(PSF).png) |
| `boar_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Boar_(PSF).png) |
| `centipede_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Centipede_(PSF).png) |
| `deer_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Deer_(PSF).png) |
| `dnd_cockatrice` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Cockatrice.png) |
| `dnd_dragon` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Dragon.png) |
| `dnd_goblin` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Goblin.png) |
| `dnd_griffon` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Griffon.png) |
| `dnd_hippogriff` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Hippogriff.png) |
| `dnd_hobgoblin` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Hobgoblin.png) |
| `dnd_hydra` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Hydra.png) |
| `dnd_kobold` | CC0 | Image: LadyofHats Description in Engli | [Commons](https://commons.wikimedia.org/wiki/File:DnD_kobold.png) |
| `dnd_manticore` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Manticore.png) |
| `dnd_minotaur` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Minotaur.png) |
| `dnd_mummy` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Mummy.png) |
| `dnd_ogre` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Ogre.png) |
| `dnd_orc` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Orc.png) |
| `dnd_roc` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Roc.png) |
| `dnd_skelleton` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_skelleton.png) |
| `dnd_spectre` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Spectre.png) |
| `dnd_stone_elemental` | CC0 | LadyofHats | [Commons](https://commons.wikimedia.org/wiki/File:DnD_Stone_Elemental.png) |
| `dory_fish_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Dory_-_fish_(PSF).png) |
| `elk_1_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Elk_1_(PSF).png) |
| `fiddler_crab_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Fiddler_Crab_(PSF).png) |
| `goat_1_psf` | CC0 | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Goat_1_(PSF).png) |
| `grizzly_bear_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Grizzly_bear_(PSF).png) |
| `hawk_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Hawk_(PSF).png) |
| `hyena_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Hyena_(PSF).png) |
| `indian_elephant_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Indian_elephant_(PSF).png) |
| `insect_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Insect_(PSF).png) |
| `jackal_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Jackal_(PSF).png) |
| `leopard_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Leopard_(PSF).png) |
| `lion_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Lion_(PSF).png) |
| `mason_wasp_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Mason_Wasp_(PSF).png) |
| `monitor_lizard_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Monitor_Lizard_(PSF).png) |
| `mouse_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Mouse_(PSF).png) |
| `mule_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Mule_(PSF).png) |
| `octopus_2_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Octopus_2_(PSF).png) |
| `ostrich_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Ostrich_(PSF).png) |
| `plesiosaur_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Plesiosaur_(PSF).png) |
| `polar_bear_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Polar_Bear_(PSF).png) |
| `raven_1_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Raven_1_(PSF).png) |
| `rhinoceros_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Rhinoceros_(PSF).png) |
| `satyr_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Satyr_(PSF).png) |
| `scorpion_2_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Scorpion_2_(PSF).png) |
| `screech_owl_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Screech_owl_(PSF).png) |
| `seahorse_line_art_psf_s_820005_cropped` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Seahorse_(line_art)_(PSF_S-820005_(cropped)).png) |
| `shark_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Shark_(PSF).png) |
| `shetland_pony_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Shetland_Pony_(PSF).png) |
| `siamese_cat_psf` | Public domain | Pearson | [Commons](https://commons.wikimedia.org/wiki/File:Siamese_Cat_(PSF).png) |
| `spider_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Spider_(PSF).png) |
| `tiger_2_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Tiger_2_(PSF).png) |
| `toad_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Toad_(PSF).png) |
| `triceratops_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Triceratops_(PSF).png) |
| `tyrannosaurus_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Tyrannosaurus_(PSF).png) |
| `vulture_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Vulture_(PSF).png) |
| `weasel_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Weasel_(PSF).png) |
| `whale_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Whale_(PSF).png) |
| `wolf_psf` | Public domain | Pearson Scott Foresman | [Commons](https://commons.wikimedia.org/wiki/File:Wolf_(PSF).png) |
