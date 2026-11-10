"""Evidence-based visual audit of the finished Create-family mockup PDF.

Keep the prior designs untouched. No UI implementation or gameplay behavior is
changed. V = visible artifact, D = design concern, S = state not demonstrated.
"""
from pathlib import Path
from collections import Counter
import json, hashlib, math
from PIL import Image, ImageDraw
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from pypdf import PdfReader, PdfWriter
from pypdf.generic import NameObject, BooleanObject

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'output/pdf/siu-create-family-window-design-review.pdf'
M=json.loads((ROOT/'output/create-ui-design/design-manifest.json').read_text())
CASES={s['id']:s for s in M['cases']}
OUT=ROOT/'output/create-ui-visual-audit';OUT.mkdir(parents=True,exist_ok=True)
TMP=ROOT/'build/create-ui-visual-audit';TMP.mkdir(exist_ok=True)
PDF=ROOT/'output/pdf/siu-create-family-visual-issues-audit.pdf'
REVIEWS=[]

def issue(kind,title,rect,seen,fix):return dict(kind=kind,title=title,rect=rect,seen=seen,fix=fix)
def review(id,detail,keep,accept,*findings):
    s=CASES[id];REVIEWS.append(dict(id=id,title=s['title'],source_page=list(CASES).index(id)+3,
       gui_size=s['gui_size'],detail=detail,keep=keep,accept=accept,findings=list(findings)))

review('pr-create-toolbox',[145,104,191,153],
 'The eight-way compartment layout is native. It must not be replaced by 32 unrelated storage slots.',
 'Use one complete inset or remove its old frame entirely; retain the actual toolbox variant/name.',
 issue('V','An incomplete second frame',[176,22,185,148],
  'The leather panel retains a bright right edge and a dark/bright bottom edge, but no equivalent top and left frame. It reads as a partly cut native window inside the SIU window.',
  'Remove all old outer-border pixels, or deliberately rebuild a closed, symmetric inset with matching padding.'),
 issue('D','Brass becomes a flat mustard surround',[4,3,187,20],
  'The broad flat gold chrome does not continue the dark leather surface or reproduce the thin, bright native gold title strip. The two skins meet at a raw rectangle.',
  'Choose the native leather as the body surface and use brass as a consistent accent; derive the surround from the native palette.'),
 issue('D','Variant identity is removed',[6,4,116,18],
  'The native title says Brown Toolbox and supplies an item model. The proposal says only Toolbox and removes the model. Color/custom-name identity is no longer represented.',
  'Keep the live native name. A small item/variant icon can carry identity without restoring the large floating model.'),
 issue('S','Compartment readability is unproven',[38,30,149,133],
  'Only eight empty summaries are shown. Aggregate quantities, filter ghosts and filled compartments are not demonstrated in this design.',
  'Review a filled toolbox at the same GUI scale, including counts and hover/transfer feedback. Do not treat this as evidence that counts are already missing.'))

review('pr-railways-mounted-toolbox',[145,104,191,153],
 'Reuse the native eight-compartment grouping and the mounted menu identity.',
 'Match the placed toolbox skin while preserving the mounted toolbox name and visible variant.',
 issue('V','The same open inset border',[176,22,185,148],
  'The copied leather panel has the same leftover right and bottom bevels as the placed toolbox. The nested frame is incomplete.',
  'Fix the shared panel construction once and apply the same complete edge treatment to both toolbox designs.'),
 issue('D','A generic mounted label replaces the variant',[6,4,144,18],
  'Mounted Toolbox distinguishes the context, but drops the native Brown Toolbox name and the visual item identity.',
  'Keep the native/custom name and add a small mounted-context indicator, instead of replacing one identity with the other.'),
 issue('D','Gold chrome and leather have no designed join',[8,20,184,31],
  'The dark board is a rectangular bitmap island in a much lighter shell. The join has neither a continuous native surface nor a complete intentional inset.',
  'Use the same palette, margins and inset rule as the placed toolbox; avoid a separate ad hoc mounted skin.'),
 issue('S','Only the empty mounted state is covered',[38,30,149,133],
  'No populated compartment, count or filter state is shown. The identical empty rendering does not establish visual behavior for mounted inventory contents.',
  'Capture a populated native mounted toolbox and compare the same data state before approving the final layout.'))

review('pr-create-schematicannon',[0,70,48,115],
 'Keep every material/checklist socket and the native progress, Play, Pause and Stop arrangement.',
 'Clean the panel edges, unify control styling and review the Settings state before using this as an implementation guide.',
 issue('V','Native side-frame remnants',[7,22,210,111],
  'Thin light/dark edge pixels remain on the sides of the copied machine board. They stop at the new body boundary and create an unfinished nested frame.',
  'Separate the machine art from its former outer frame. Either remove those strips or close the inset on all four sides.'),
 issue('D','Two incompatible control styles',[7,116,210,138],
  'Native playback controls are square gray icon buttons. Settings and Done are newly rounded, tinted text buttons with heavier shadows.',
  'Use one button metric and edge language. Keep native icons where they communicate better than a new large text button.'),
 issue('D','The footer adds a large quiet band',[63,116,171,138],
  'The window devotes a full-width lower strip to two corner buttons. It is visually detached from the dense machine controls above.',
  'Integrate the utility actions with the machine layout, or give the footer a consistent purpose and much tighter spacing.'),
 issue('S','The Settings view is described but not drawn',[7,119,60,138],
  'The report promises an internal Settings page, but shows no layout for that state. Its controls and expansion requirements cannot be reviewed here.',
  'Add the native Settings state and its proposed window state, including its return action and complete bounds.'))

review('pr-create-schematic-table',[147,14,224,45],
 'The filename picker, refresh, input, toggle and larger output are distinct native controls.',
 'Restore the complete filename outline and refresh button; remove all sampled world and old-footer pixels.',
 issue('V','Filename field is cut at the bottom and right',[29,21,178,40],
  'The original picker has a complete light lower edge and right edge. The copied field ends as a flat gray cut in the proposal; those finishing pixels are absent.',
  'Capture/render the full widget bounds, including its lower/right bevel. Preserve the native field height rather than cropping to the text.'),
 issue('V','Refresh includes its old outside surroundings',[196,20,218,41],
  'A dark block sits above/right of refresh. It is part of the old outside location, and the button surround is also cut short.',
  'Use the refresh widget or its clean sprite, not a screenshot rectangle that includes world pixels. Keep its complete button border.'),
 issue('V','The old gray footer survives as a patch',[21,51,198,80],
  'The input, check and output row sits on a large light-gray rectangle copied from the native footer. It clashes with the new blue body.',
  'Render the individual sockets, arrow and control on the intended body surface; do not carry the old footer background with them.'),
 issue('D','Actions and alignment are unclear',[7,87,219,113],
  'The picker, refresh and lower row start on different axes. A native check and a new Done button both look like confirmation, without a visual explanation of their separate roles.',
  'Align both rows to a deliberate grid and make the native operation distinguishable from closing/saving the window.'))

review('own-create-fabric-blueprint',[1,15,79,86],
 'The smaller overlapping result/reference cell also exists in the native UI. Its overlap is not a new defect.',
 'Keep all eleven native cells; finish the canvas and make recipe-edit actions visually subordinate to the recipe.',
 issue('D','A raw blueprint rectangle is pasted into flat blue',[8,21,165,83],
  'The bright patterned canvas has abrupt square boundaries against the flatter, darker SIU surround. It looks like a cropped tile rather than a finished work surface.',
  'Extend the native blue surface coherently or give the canvas a complete deliberate border with even padding.'),
 issue('D','Ghost editing has little visible context',[23,23,153,80],
  'The result and auxiliary cell look like ordinary empty slots. The removed blueprint model and generic gray cells leave little visual evidence that this is a recipe editor.',
  'Keep the native reference/ghost rendering and provide compact recipe identity through a title icon or small labels, without adding fake storage slots.'),
 issue('D','Footer labels are weak on light blue',[7,87,167,110],
  'Clear and Done use pale text on a light tinted face without a dark text shadow. Their rounded frame differs from the native socket/control vocabulary.',
  'Use legible text contrast and the same SIU/native button system chosen for the rest of the family.'),
 issue('S','Only the empty recipe is demonstrated',[108,39,154,81],
  'A filled recipe, actual result, alternate recipe/reference content and tooltip state are not shown.',
  'Review those states before changing the overlap or deciding which result cell may be removed.'))

review('pr-create-frogport',[136,37,180,84],
 'The final source image has the complete lower grid edge. Do not repeat the earlier, already-corrected bottom-border complaint.',
 'Remove the thin patterned fringe, restore an actual name presentation and follow SIU control rules consistently.',
 issue('V','A narrow native background fringe remains',[171,40,175,79],
  'The grid has a dotted/checkered strip along the right edge. It is native body background left beside the slot border, not an intentional new frame.',
  'Trim to the complete slot-grid boundary, retaining every border pixel but excluding the neighboring body texture.'),
 issue('D','The name row shows a label but no name',[8,21,174,40],
  'The new row says Name plus Edit, while the native edit pencil belongs to the visible title/name. The new row consumes height without displaying the value it edits.',
  'Show the current name with its edit affordance, preferably in the title or a compact field. Avoid an empty label-only utility row.'),
 issue('D','Header controls lack a family-wide rule',[7,3,178,19],
  'This is the only proposal with five SIU title controls; the other storage/configuration views mostly have only overflow and close. The visual rules are not explained.',
  'Use the existing SIU control-availability rules consistently for storage versus configuration screens. Do not invent sorting for ghost/reference data.'),
 issue('D','The extra Done row compounds the height',[7,85,175,108],
  'An added name row and a separate closing footer surround a very small 9x2 grid. The useful inventory is less dominant than it could be.',
  'Use tighter vertical structure after the name/edit behavior is settled; keep the full grid intact.'))

review('pr-create-redstone-requester',[0,86,59,118],
 'The paper hook, all nine request cells and both mode icons are native details worth keeping.',
 'Build one continuous wood surface and themed mode controls, then test long addresses and requested quantities.',
 issue('V','The copied wood panel has a hard seam',[8,21,222,88],
  'The rack/address bitmap has a different striped wood color from the flat outer surround. Its rectangle ends abruptly above the new footer.',
  'Continue the native wood material through the body, or use a complete inset with consistent margins instead of a bare crop.'),
 issue('D','Native mode controls clash with the new Done button',[7,96,45,114],
  'The two native square gray mode buttons sit beside a newly rounded brown Done button. Their different face colors, bevels and shadows make the footer look assembled from two UI systems.',
  'Keep the mode sprites but use one deliberate control skin and spacing system. The gray button faces alone are not proof of stray footer pixels.'),
 issue('S','Address and quantity extremes are unshown',[41,57,190,84],
  'The request row and destination are empty. The mockup does not establish how quantities or a long destination fit beside the complete paper art.',
  'Show a populated nine-entry request and a long address before approving the final field and row bounds.'))

review('pr-create-stock-ticker',[0,16,81,71],
 'Category creation must retain its native data and the category editor; this is not a new inventory grid.',
 'Remove the open board outline and design both a compact empty state and a real category list.',
 issue('V','The board outline is left unfinished',[8,21,193,65],
  'The copied board keeps a dark top and left line, while its right and bottom have no corresponding termination. The shape reads as an L-shaped crop remnant.',
  'Use a continuous wood body or a complete four-sided inset. Do not retain only the part of a native frame that happens to fit.'),
 issue('D','One plus tab still occupies a large blank area',[28,24,187,62],
  'The empty category view is shorter than native, but a single small plus control still sits in a wide, largely unused board plus a Done footer.',
  'Give the empty state an explicit Add Category affordance and size the body to that state.'),
 issue('S','Real category rows and overflow are not designed',[8,21,193,65],
  'Only the empty state is shown. There is no visual plan for several names/icons, category ordering or a long list.',
  'Supply populated, long-name and scroll/overflow states using the same frame before calling this view complete.'))

review('cplus-stock-category-editor',[5,38,96,69],
 'The icon socket and full name field are the essential native content; neither should become a decorative placeholder.',
 'Remove the doubled horizontal separation and make the editor belong visibly to its parent category view.',
 issue('V','Two separator lines are stacked',[8,54,179,64],
  'The bottom black line of the copied wood panel remains immediately above the new footer rule. That leaves an accidental double division.',
  'Remove the former panel boundary and use one intentional separator, or finish a complete inset with enough spacing.'),
 issue('D','The input panel uses a different wood treatment',[8,21,179,55],
  'The field sits on striped native wood while the title/footer are flat brown. The abrupt rectangular join makes the form look composited.',
  'Choose a continuous native wood surface or a clearly framed form area; keep the native field bevel complete.'),
 issue('D','Parent context and commit behavior are unspecified',[5,4,180,19],
  'Category Editor appears as a standalone window with close and Done. The design does not show its relationship to Stock Ticker or what returning looks like.',
  'Show the attached/child state, preserve the selected category and distinguish returning from accepting edits.'),
 issue('S','Long names and a chosen icon are unshown',[12,24,178,43],
  'The reference uses New Category and an empty icon. A real icon plus a long or edited name has not been reviewed.',
  'Validate those visual states and text selection/caret placement inside the unchanged field border.'))

review('own-create-stock-request-real',[127,155,241,201],
 'Keep the native stock lock, all nine basket entries, the paper destination and Send. The tall empty native list need not remain.',
 'Rebuild the viewport and footer from clean elements; no old outer-frame pixels may remain around Send or the basket.',
 issue('V','Old frame fragments surround the bottom actions',[8,166,234,194],
  'Black/gray strips and angled outside-frame pixels remain above and beside the destination/Send row. Those fragments do not join the new brown frame.',
  'Extract clean action/field sprites or render their actual widgets. Remove the old outer footer and world/background pixels.'),
 issue('V','Basket ends do not connect to the new structure',[19,129,224,166],
  'Short dark/gray vertical fragments hang from both rack ends. They belong to the old full-height assembly and now look cut off.',
  'Give the rack complete, symmetric end caps and deliberate clearance from the address row.'),
 issue('D','Search, results and basket use different axes',[14,21,229,127],
  'The search is deeply inset, the results extend wider, and the basket/address start elsewhere again. The manually drawn scrollbar is visually detached from the results edge.',
  'Establish common content rails and a complete viewport/scrollbar, then align the pinned basket and destination below it.'),
 issue('S','The one-item state does not cover stock browsing',[14,46,228,126],
  'Only one stock item and an empty basket are shown. Category groups, many results, selected quantities, a full basket and a long address are not demonstrated.',
  'Review those states; keep this as a promising compact direction rather than a finished browsing design.'))

review('own-create-gauge-set-item',[22,16,104,57],
 'The single monitor-item reference cell and both native pointers are correct. This is not the main gauge settings screen.',
 'Make the material continuous and reduce the chrome cost of this one-cell selector.',
 issue('V','The wood behind the socket is a cut rectangle',[26,21,98,53],
  'A small striped native wood patch floats in flat brown chrome. Its abrupt edges make the arrows and slot look pasted on.',
  'Continue the native wood across the body or put the picker in a complete, consistently padded inset.'),
 issue('D','Chrome outweighs the one-cell task',[7,57,118,79],
  'The GUI is 124x83, while the copied content is only 71x31. Title, padding and footer consume 52 of the 83 vertical GUI units.',
  'Use a compact picker structure and a clear accept/return affordance instead of a full-width mostly empty Done strip.'),
 issue('D','Gauge identity is reduced to a generic label',[5,4,118,18],
  'Monitor Item is concise, but the native gauge model and context are absent. Several similar pickers could look identical.',
  'Retain a small native gauge/item identity or parent context. Evaluate size at the shared GUI scale, not the enlarged PDF fit.'))

review('pr-create-filter',[0,64,103,93],
 'All eighteen ghost cells and both mode pairs remain present. Their contents are filter rules, not storage.',
 'Finish the grid surround, redraw mode surfaces consistently and improve the pale footer text.',
 issue('D','The grid has an arbitrary texture halo',[8,21,178,63],
  'A narrow checker-pattern rectangle surrounds the grid, then stops against flat parchment chrome. It is neither a full native panel nor a designed inset.',
  'Use one continuous body treatment or a properly closed grid inset with even margins.'),
 issue('V','The native gray mode strip is pasted into the footer',[7,71,86,89],
  'A light-gray rectangle, including the gap between mode pairs, remains behind the icons. The rest of the footer is parchment.',
  'Draw each mode control on the chosen skin while preserving its selection state and icon; remove the sampled footer background.'),
 issue('D','Clear and Done have weak text contrast',[96,71,180,90],
  'The labels are pale on a light parchment face and have no dark glyph shadow. Their rounded, heavy frame also clashes with the square mode buttons.',
  'Use readable dark text or a proper text shadow and one button vocabulary across the strip.'),
 issue('S','Mode and filled-entry readability are unshown',[11,23,175,61],
  'There is no populated filter, tooltip or alternate selected-mode example.',
  'Review item ghosts, long tooltips and each mode selection before approving this footer/grid combination.'))

review('pr-create-attribute-filter',[0,53,103,80],
 'The native Add, Add Inverted, rule-count badge and three matching modes must remain distinct. The visible badge count is not missing.',
 'Replace the mixed gray/footer patchwork with a coherent skin and demonstrate how existing rules are inspected.',
 issue('V','The footer is still a sampled gray band',[7,57,87,78],
  'The badge and three mode buttons sit on a large native light-gray strip inside the parchment footer. The strip includes old background pixels.',
  'Keep the native controls and badge rendering, but draw them individually on the intended footer surface.'),
 issue('D','The reference row is another texture island',[8,21,221,50],
  'A patterned native mat surrounds the reference/dropdown/Add row, then cuts to flat chrome. Its square edges do not match the rounded outer structure.',
  'Make a deliberate complete inset or continue the native material through the body. Keep the dropdown and both Add controls whole.'),
 issue('D','Pale text and mixed button frames weaken hierarchy',[143,57,222,77],
  'Clear and Done are light text on light parchment; native mode/Add controls are flat square gray. The two systems compete visually.',
  'Unify the surface/edge style and improve text contrast while keeping the native meaning of each control.'),
 issue('S','A count of one is not a review of existing rules',[7,58,29,78],
  'The view shows a candidate attribute and a count badge, but not the rule-inspection/dropdown states needed to understand what has already been added.',
  'Show several rules, inverted rules and the selection/dropdown states. Do not infer a functional rule-list failure from this still image.'))

review('pr-create-package-filter',[0,17,83,49],
 'The package icon, full address field and paper-end icon remain native elements; the cursor is an empty focused-state cue.',
 'Finish the address surface and use a compact, legible control arrangement without the sampled body halo.',
 issue('D','The old patterned body survives as a narrow mat',[8,21,201,45],
  'The icon and gray field are surrounded by a thin checker strip on a blue outer body. Its abrupt rectangle makes the field look pasted on.',
  'Use a continuous material or a complete intentional inset, keeping the field tip and both edge highlights.'),
 issue('D','Footer size and button style dominate a one-line editor',[7,53,202,72],
  'Two rounded text buttons and a full-width footer are added beneath a single field. The new pale text is weaker than the native field text.',
  'Use compact native/SIU actions with sufficient text contrast and a clear distinction between clearing the value and accepting it.'),
 issue('S','Address editing states are not demonstrated',[31,24,195,43],
  'The only content is a cursor. Long addresses, completions, selection and an unfocused field are not shown.',
  'Review those states with the paper-end icon visible and text clipped only inside the actual field interior.'))

review('pr-create-linked-controller',[0,15,69,80],
 'The four-plus-two grouping and its divider come from the native layout. Preserve them and all twelve frequency cells.',
 'Keep the grouping, but resolve the body/chrome join and show readable configured frequency/key states.',
 issue('D','Dark body and bright shell are not integrated',[8,21,165,75],
  'The native charcoal panel ends as a raw rectangle inside a broad mustard frame. It looks like two skins layered without an intentional inset.',
  'Use a complete dark inset or a dark body with controlled brass accents, while preserving the group separator.'),
 issue('D','Pale footer buttons do not match frequency controls',[7,83,167,102],
  'Clear and Done use the new rounded gold style. The cells, labels and group divider use native square gray/dark rendering.',
  'Apply the shared control and contrast rules without changing the frequency-cell backgrounds.'),
 issue('S','Actual key and frequency readability is unproven',[10,22,162,73],
  'The empty view has tiny key glyphs and no item frequencies. It does not establish whether real key names or item pairs remain readable.',
  'Show configured keys, all six populated pairs and hover information. Do not diagnose the native glyphs as lost labels from this empty state.'))

review('own-create-fabric-schedule',[0,25,45,52],
 'Keep the brass timeline, instruction card, native condition controls and the current-entry indicator.',
 'Restore the complete entry indicator, finish the shortened viewport and keep editor navigation visually connected.',
 issue('V','The left entry indicator is cut at the viewport edge',[7,33,15,47],
  'The native white/black current-entry chevron starts outside the copied body rectangle. The proposal leaves it running directly into the left crop.',
  'Include the entire indicator with a small margin, or render it separately on the reconstructed viewport edge.'),
 issue('D','The shortened timeline ends abruptly',[21,98,33,124],
  'The brass vertical strip is simply cut at the new lower body boundary. It has no designed end treatment or clear continuation into scrolling content.',
  'Rebuild the timeline with intentional caps/continuation and a complete viewport boundary; do not shorten it with a fixed crop.'),
 issue('V','Gray native mode-button backing remains',[7,131,68,150],
  'The loop/up/down controls carry a light-gray rectangle into the olive footer. The new Done action has a different frame treatment.',
  'Render the controls consistently on the selected surface while preserving the active loop color and disabled states.'),
 issue('S','Editor attachment and longer schedules are not shown',[17,28,216,95],
  'The next PDF page shows an independent editor, rather than the attached state promised in the text. There is also no multi-card/scroll example.',
  'Show the editor in context and a schedule long enough to scroll, including selection and return position.'))

review('schedule-editor',[3,40,43,69],
 'Instruction type, destination field and any type-specific item/reference cells are native controls.',
 'Restore the missing reference-cell edge and present the editor as a clear child/attached state.',
 issue('V','The reference socket loses its left edge',[8,45,28,65],
  'The native reference cell has a complete dark left border. The copied region begins at the item interior, leaving that border absent in the proposal.',
  'Include the one-pixel native frame around the complete cell, not just its item coordinates. Keep item content separate from the socket border.'),
 issue('D','The gray form is a hard cut inside olive chrome',[8,21,154,64],
  'The two fields sit on a rectangular gray background that stops abruptly at the surrounding olive body.',
  'Give the form a complete deliberate inset or render each field on a coherent body surface.'),
 issue('D','Parent/return context is absent',[5,4,155,19],
  'The heading says Instruction Editor, but no schedule, selected instruction or return affordance is visible. Delete and Apply have equal visual weight.',
  'Show the editor attached to its schedule or provide clear selected-entry context and return behavior; visually subordinate deletion.'),
 issue('S','Other instruction types are not represented',[8,43,154,65],
  'Travel to Station with an empty destination is only one editor state. Item-dependent conditions and wider controls are not shown.',
  'Review the largest relevant instruction/condition form before fixing the editor size.'))

review('createbigcannons-autocannon-ammo-container',[4,23,69,52],
 'The two sockets and capacity/status bar have distinct native ammunition roles; do not convert this to a generic storage grid.',
 'Restore comfortable inner margins and ammunition identity, and reduce the large one-action footer.',
 issue('D','Sockets are pressed against the lower crop',[17,27,65,48],
  'The complete lower socket edges are present, but almost touch the bottom of the olive rectangle. The row has much more room above than below.',
  'Preserve the borders and add deliberate equal breathing room around the whole ammunition row.'),
 issue('D','Olive material is an unframed island',[8,21,134,47],
  'The short dark green native panel sits in a much brighter mustard window. The transition is a raw square crop.',
  'Use a continuous native olive body or a complete inset with a restrained brass accent.'),
 issue('D','Identity and proportions are weakened',[5,4,138,19],
  'Ammo Container drops Autocannon from the native title and removes the item model. With 26 content units in a 78-unit-high window, chrome takes most of the height.',
  'Retain the native ammunition identity in a compact title/icon and tighten the Done structure. Verify capacity/count states separately.'))

for suffix,title,variant in [('timed','Timed Fuze','time'),('proximity','Proximity Fuze','distance'),('delayed-inertia','Delayed Inertia Fuze','inertia delay'),('delayed-impact','Delayed Impact Fuze','impact delay')]:
    review('item-createbigcannons-'+suffix+'-fuze',[20,33,164,74],
     'Keep the complete native ruler, marker and native unit/range semantics. The marker itself is not visibly clipped in this final asset.',
     'Reduce the empty chrome, make the '+variant+' setting explicit and integrate the ruler into a finished native-colored body.',
     issue('D','The native panel becomes a small texture island',[29,37,156,69],
      ('The blue proximity texture' if suffix=='proximity' else 'The dark native texture')+' is preserved only as a small raw rectangle around the ruler, surrounded by a large flat gold field.',
      'Make that native material a complete body/inset with designed margins, instead of a floating screenshot patch.'),
     issue('D','The proposed main rectangle is larger, not tighter',[7,80,180,104],
      'The recorded native main bounds are 179x83 GUI units; the proposal is 186x108. That is about 35% more rectangular area for the same ruler and an added value label.',
      'Use a compact value/ruler/action arrangement and compare it at the same GUI-pixel scale before judging size.'),
     issue('D','Value and variant cues need more care',[44,21,143,35],
      ('1 block is not labeled as detonation distance.' if suffix=='proximity' else '1s 0 ticks is displayed without a Fuze Time/delay label.')+' The removed native item model also carried variant identity.',
      'Use a compact setting label and native item/variant cue. Give the ruler a scroll/hover affordance without covering it.'),
     issue('S','Range extremes and adjustment state are unshown',[27,35,160,70],
      'Only one value is shown. The design does not demonstrate longer values, either end of the ruler, hover instructions or Shift adjustment.',
      'Review those visual states with native formatting. This still image does not establish whether interaction is broken.'))

review('item-createshufflefilter-shuffle-filter',[3,18,66,64],
 'All eighteen cells are visible in the final empty grid. Do not invent weights for the equal-shuffle variant.',
 'Keep the equal grid but finish its surround and resolve the low-contrast generic footer.',
 issue('D','Native slot shading and new frame look unrelated',[8,21,186,60],
  'The soft shaded grid is surrounded by a thin sampled parchment mat, then a crisp flat SIU skin. Their edge treatments do not form one coherent surface.',
  'Preserve the native cells while using a deliberate complete surround and consistent texture-to-frame transition.'),
 issue('D','Clear/Done dominate the small grid and read weakly',[7,68,188,87],
  'The pale labels on light parchment lack dark glyph shadows. A full-width lower band adds visual weight to two simple actions.',
  'Use the shared readable control skin and a tighter action layout.'),
 issue('S','The populated equal-choice state is absent',[11,23,182,58],
  'No item ghosts or selection/hover feedback are shown, so the distinction from a storage grid is not demonstrated.',
  'Show several configured entries with the same native item rendering and explain equal choice without adding fake item counts.'))

review('item-createshufflefilter-weighted-shuffle-filter',[5,42,68,89],
 '100 is a native weight, not an item-stack count. Keep it paired with its entry; all eighteen cell borders are present in the final image.',
 'Make weight meaning clear and verify second-row numbers before accepting the fixed body crop.',
 issue('D','The weight lacks an in-window explanation',[11,43,39,60],
  '100 appears beneath Stone, but the window itself does not identify it as a weight or show how it is adjusted.',
  'Add a compact Weight cue or a clear native hover/adjustment state; do not move it into an item-stack count corner.'),
 issue('D','Texture margins and footer styling are inconsistent',[8,21,202,85],
  'The patterned mat has uneven edge clearance and meets flat parchment chrome. The new rounded pale buttons do not match the native cell rendering.',
  'Use consistent padding, one finished body/inset and legible shared actions.'),
 issue('S','Second-row weights are not demonstrated',[11,76,201,86],
  'Only a top-row entry is populated. The fixed lower crop has not been shown with bottom-row weight numbers, longer values or adjustment feedback.',
  'Populate the final cell of each row and show all weight labels at the minimum and maximum supported formatting. Treat bottom-row clipping as a check, not a proven current defect.'))

review('cplus-typewriter',[210,66,252,108],
 'The five-row layout and blank keycaps are native. Hover/key mapping states provide information; blank caps alone are not evidence of missing letters.',
 'Finish the keyboard inset, style the actions consistently and show selected/hovered key states.',
 issue('V','The keyboard retains partial old frame edges',[241,22,247,102],
  'Dark/light native border strips remain at the right and bottom of the keyboard crop. They form a second asymmetric frame inside SIU.',
  'Keep either a fully closed native keyboard inset or clean keyboard art with one deliberately constructed border.'),
 issue('D','Reset looks like an ordinary primary action',[7,110,53,129],
  'Reset, Bindings and Done use the same large gold text-button treatment. Reset is visually as routine as navigation or completion.',
  'Subordinate reset and retain its native confirmation behavior. Use one legible action skin with clear hierarchy.'),
 issue('S','Keyboard feedback and editor relationship are absent',[13,25,240,96],
  'The clean keyboard shows no selected or hovered key, binding feedback or attached editor. The next pages are standalone snapshots.',
  'Show a key popover/selection and its transition to the key editor and bindings view, preserving all original key widths.'))

review('typewriter-key-editor',[72,17,128,49],
 'The final version has a complete key field and both frequency swatches. The old stray top stripe is already gone.',
 'Clarify selected-key/frequency context and make the editor visibly connected to the keyboard.',
 issue('D','The form remains an unframed gray cutout',[8,21,123,45],
  'The gray field/swatch strip has a raw rectangular edge inside the gold window. It has no complete inset treatment.',
  'Render the form on a coherent surface or give it a complete, consistently padded inset.'),
 issue('D','Frequency cells look like unexplained color chips',[84,22,122,43],
  'Two adjacent red/blue squares have no visible label or item-frequency example. Their role as editable references is not obvious in this still state.',
  'Keep native frequency rendering and add compact context/hover feedback; review a pair with real reference items.'),
 issue('D','Selected-key context and action hierarchy are weak',[7,53,124,72],
  'Key Editor does not name its parent/selected mapping beyond the tiny field content. Delete and Apply are visually equal peer buttons.',
  'Show the selected mapping and parent relationship, with deletion subordinate to accepting the edit. Do not change native commit behavior based on the mockup alone.'))

review('typewriter-bindings',[0,17,101,65],
 'The row contains key, two frequencies, Edit and Delete. No lower-row clipping is proven in the one-row final capture.',
 'Balance the row mat, unify nested actions and show more than one binding with scrolling/return context.',
 issue('D','The row mat has very uneven clearance',[8,21,230,59],
  'A broad dark strip remains above the gray row, while the lower edge is almost flush with the crop. It reads as a leftover slice of the native tall board.',
  'Construct a complete row/list surface with equal top/bottom padding rather than cutting a tall panel down to one row.'),
 issue('D','Nested actions use two different skins',[7,67,232,86],
  'Edit/Delete are square native gray icons, while Reset/Add/Done are new rounded gold text buttons. Reset also has the same prominence as routine actions.',
  'Use a consistent control vocabulary and hierarchy for row editing, list navigation and destructive reset.'),
 issue('S','A multi-row list and return path are not shown',[8,21,230,59],
  'The design only contains one mapping. There is no scrollbar, multi-row spacing, long key sequence or return-to-keyboard state.',
  'Review a list that actually scrolls and demonstrate editing a row and returning without losing selection/context.'))

assert len(REVIEWS)==27 and {r['id'] for r in REVIEWS}==set(CASES)
for r in REVIEWS:
    s=CASES[r['id']]
    assert hashlib.sha256((ROOT/s['mockup']).read_bytes()).hexdigest()==s['mockup_sha256']
    assert hashlib.sha256((ROOT/s['native']['capture']).read_bytes()).hexdigest()==s['native']['sha256']
    w,h=s['gui_size']
    for f in r['findings']:
        x0,y0,x1,y1=f['rect'];assert 0<=x0<x1<=w and 0<=y0<y1<=h,(r['id'],f['title'])

COUNTS=Counter(f['kind'] for r in REVIEWS for f in r['findings'])
DATA=dict(source_pdf=str(SOURCE.relative_to(ROOT)),source_sha256=hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
    reviewed_views=27,unique_container_classes=23,counts=dict(COUNTS),reviews=REVIEWS,
    scope='Static visual analysis of the final existing mockups, including native-reference comparison. No implementation or gameplay certification.')
(OUT/'findings.json').write_text(json.dumps(DATA,indent=2)+'\n')

for path,name in [('C:/Windows/Fonts/arial.ttf','Body'),('C:/Windows/Fonts/arialbd.ttf','Bold')]:pdfmetrics.registerFont(TTFont(name,path))
W,H=1190,842
C=canvas.Canvas(str(TMP/'audit-unoptimized.pdf'),pagesize=(W,H),pageCompression=1)
C.setTitle('Create Family - Visual Issues Audit')
C.setAuthor("Salt's Inventory Update")
INK='#213436';MUTED='#5d6e70';TEAL='#267977';LIGHT='#edf2f1';RED='#b7353f';AMBER='#99600c';PURPLE='#7356a2'
KCOL={'V':RED,'D':AMBER,'S':PURPLE}
KNAME={'V':'VISIBLE ARTIFACT','D':'DESIGN CONCERN','S':'STATE NOT SHOWN'}
PAGES=[]

def text(t,x,y,size=12,color=INK,bold=False):
    C.setFillColor(HexColor(color));C.setFont('Bold' if bold else 'Body',size);C.drawString(x,y,t)

def lines(t,width,size=11.5,bold=False):
    name='Bold' if bold else 'Body';out=[];line=''
    for word in t.split():
        candidate=(line+' '+word).strip()
        if pdfmetrics.stringWidth(candidate,name,size)>width and line:out.append(line);line=word
        else:line=candidate
    if line:out.append(line)
    return out

def para(t,x,y,width,size=11.5,leading=16,color=INK,bold=False):
    for line in lines(t,width,size,bold):text(line,x,y,size,color,bold);y-=leading
    return y

def card(x,y,w,h,color=LIGHT):
    C.setFillColor(HexColor(color));C.roundRect(x,y,w,h,8,stroke=0,fill=1)

def start(title,sub,section='VISUAL AUDIT'):
    text('SIU / CREATE FAMILY',42,807,10,TEAL,True);text(section,948,807,10,MUTED,True)
    text(title,42,764,28,INK,True);para(sub,42,737,1100,11.5,16,MUTED)

def end(name):
    n=len(PAGES)+1;PAGES.append(name)
    C.setStrokeColor(HexColor('#d8e1df'));C.setLineWidth(.7);C.line(42,40,1148,40)
    text('Audit of existing mockups | Colored marks are annotations, not redesigned UI',42,23,8.5,MUTED)
    C.setFont('Body',10);C.drawRightString(1148,23,str(n));C.showPage()

def image_in(im,box):
    if isinstance(im,(Path,str)):im=Image.open(im).convert('RGBA')
    x,y,w,h=box;s=min(w/im.width,h/im.height);iw,ih=im.width*s,im.height*s
    xx=x+(w-iw)/2;yy=y+(h-ih)/2
    C.drawImage(ImageReader(im),xx,yy,iw,ih,mask='auto')
    return xx,yy,iw,ih

def mock_image(r):return Image.open(ROOT/CASES[r['id']]['mockup']).convert('RGBA')

def detail_image(r,rect=None):
    rect=rect or r['detail'];return mock_image(r).crop(tuple(v*4 for v in rect))

def badge(n,x,y,color):
    C.setFillColor(HexColor(color));C.circle(x,y,9,stroke=0,fill=1)
    C.setFillColor(HexColor('#ffffff'));C.setFont('Bold',10);C.drawCentredString(x,y-3,str(n))

def annotated(r,box):
    xx,yy,iw,ih=image_in(mock_image(r),box);gw,gh=r['gui_size']
    placed=[]
    for i,f in enumerate(r['findings'],1):
        x0,y0,x1,y1=f['rect'];rx=xx+x0/gw*iw;ry=yy+(gh-y1)/gh*ih;rw=(x1-x0)/gw*iw;rh=(y1-y0)/gh*ih
        col=KCOL[f['kind']];C.setStrokeColor(HexColor(col));C.setLineWidth(1.3);C.rect(rx,ry,rw,rh,stroke=1,fill=0)
        bx=min(xx+iw-9,max(xx+9,rx+8));by=min(box[1]+box[3]-10,ry+rh+8)
        original=(bx,by)
        for attempt in range(30):
            if all(math.hypot(bx-px,by-py)>=21 for px,py in placed):break
            bx+=22
            if bx>xx+iw-9:bx=xx+9;by-=22
        assert all(math.hypot(bx-px,by-py)>=21 for px,py in placed),(r['id'],'annotation collision')
        if (bx,by)!=original:
            C.setLineWidth(.7);C.line(bx,by,original[0],original[1]-8)
        placed.append((bx,by))
        badge(i,bx,by,col)

def finding_block(f,i,x,y,width):
    col=KCOL[f['kind']];badge(i,x+9,y+1,col)
    text(KNAME[f['kind']],x+25,y+5,8.3,col,True)
    title_lines=lines(f['title'],width-25,12.5,True)
    yy=y-11
    for t in title_lines:text(t,x+25,yy,12.5,INK,True);yy-=15
    yy-=1
    yy=para(f['seen'],x+25,yy,width-25,11,15)
    yy=para('Correction: '+f['fix'],x+25,yy-3,width-25,10.8,14.5,MUTED)
    return yy-15

# Cover: the strongest current defects, rather than another untested redesign.
start('Create mockups: what needs to change','A visual audit of all 27 current comparison views, checked against the final PDF assets and native references.')
text('The problem is bigger than a few cropped pixels.',42,674,24,INK,True)
para('The proposals repeatedly carry pieces of old frames into new ones, mix unrelated surface and button styles, and spend too much space on generic chrome. A few widgets also have confirmed missing edges.',42,640,1040,16,23)
examples=[('pr-create-schematic-table','Picker / refresh: cut edges and outside pixels'),('own-create-stock-request-real','Stock Keeper: old footer fragments'),('schedule-editor','Instruction Editor: missing socket edge')]
for i,(id,title) in enumerate(examples):
    r=next(a for a in REVIEWS if a['id']==id);x=42+i*373;card(x,291,355,228)
    image_in(detail_image(r),(x+12,328,331,174));para(title,x+14,312,326,11.3,15,TEAL,True)
text('Three kinds of finding',42,242,14,INK,True)
for i,(k,desc) in enumerate([('V','Visible artifact: directly present in the final image.'),('D','Design concern: palette, hierarchy, spacing or clarity.'),('S','State not shown: needs a visual example; not a proven gameplay bug.')]):
    y=211-i*30;badge(i+1,51,y+1,KCOL[k]);text(desc,70,y-3,12,INK)
para(f"Review coverage: 27 views / 23 unique container screen classes. This report records {COUNTS['V']} visible-artifact findings, {COUNTS['D']} design concerns and {COUNTS['S']} state checks. Repeated family issues are recorded on each affected view.",42,95,1090,11.5,17,MUTED)
end('Scope and findings')

start('Repeated problems across the family','These are shared causes. Fixing them consistently matters more than individually nudging a few pixels.')
global_items=[
 ('01 / Panel pieces are being treated as finished skins','Screenshot rectangles include old borders, footer fills and outside pixels. When moved, they leave open bevels, hard seams and fragments. Use actual sprites/widgets and a complete body/inset construction.'),
 ('02 / Materials meet without a designed join','Patterned leather, wood, parchment and blue panels end against unrelated flat chrome. Derive colors from native assets and choose either a continuous surface or a fully finished inset.'),
 ('03 / Buttons use conflicting visual systems','Native square gray icons coexist with newly rounded tinted text buttons and heavy shadows. The custom text has no dark glyph shadow, making light-colored buttons particularly weak.'),
 ('04 / Window management and task actions lack a rule','Two-control and five-control title bars are mixed without a documented SIU availability rule. Done, Apply, checkmarks, Edit and close need clear visual roles; a screenshot cannot establish their commit semantics.'),
 ('05 / Small tasks acquire too much chrome','Several views allocate a full-width footer to one action. The four fuze main rectangles are about 35% larger in area than their recorded native bounds despite having the same ruler.'),
 ('06 / Identity and state coverage are reduced','Generic titles and removed item models can erase useful context. Empty grids, a single stock item or one rule/binding do not establish how filled, long-text, dropdown and scroll states will look.')]
y=679
for i,(head,body) in enumerate(global_items):
    x=42 if i<3 else 608;j=i if i<3 else i-3;yy=679-j*187
    text(head,x,yy,13,TEAL,True);para(body,x,yy-26,520,13,19)
para('The detailed pages distinguish visual evidence from design judgment. A state check is not a claim that slots, networking, focus or native actions are broken.',42,90,1080,12,18,MUTED)
end('Family-wide issues')

def luminance(rgb):
    v=[c/255 for c in rgb];v=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in v]
    return .2126*v[0]+.7152*v[1]+.0722*v[2]
def contrast(a,b):
    a,b=sorted([luminance(a),luminance(b)],reverse=True);return (a+.05)/(b+.05)

start('Scale and contrast are masking problems','The previous PDF fitted every picture independently. Large-looking text and slots are not, by themselves, proof of stretched artwork.')
text('Same GUI-pixel scale: Timed Fuze',42,683,15,TEAL,True)
s=CASES['item-createbigcannons-timed-fuze'];b=s['native']['bounds'];raw=Image.open(ROOT/s['native']['capture']).convert('RGBA')
crop=raw.crop((b['x']*2,b['y']*2,(b['x']+b['width'])*2,(b['y']+b['height'])*2))
text('Native main bounds: 179 x 83',42,654,12,INK,True)
text('Proposed main window: 186 x 108',608,654,12,INK,True)
C.drawImage(ImageReader(crop),80,409,358,166,mask='auto')
C.drawImage(ImageReader(Image.open(ROOT/s['mockup'])),650,409,372,216,mask='auto')
para('Both are displayed at 2 PDF points per GUI unit. The proposed main rectangle has about 35% more area. The native floating item model is outside the recorded main bounds and excluded from this size comparison.',42,379,1090,11.5,17,MUTED)
text('Pale custom button text on the generated flat face',42,317,15,TEAL,True)
text('Palette',42,285,10,INK,True);text('Rendered face RGB',244,285,10,INK,True);text('Sample',505,285,10,INK,True);text('Contrast with #EEEEEE',760,285,10,INK,True)
palettes=[('Brass',(202,166,107)),('Parchment',(195,177,148)),('Blue',(128,151,186)),('Wood',(157,114,89)),('Dark',(100,97,86))]
for i,(name,rgb) in enumerate(palettes):
    y=255-i*29;text(name,42,y,12);text(str(rgb),244,y,12)
    C.setFillColorRGB(*(x/255 for x in rgb));C.rect(499,y-7,130,25,stroke=0,fill=1);text('Clear / Done',509,y+1,11,'#eeeeee',True)
    text(f'{contrast(rgb,(238,238,238)):.2f} : 1',760,y,12)
para('These are flat-face contrast estimates from the existing drawing palette, not a compliance score or a complete readability test. The black outer bevel does not add a shadow to the letters. Dark glyphs or an appropriate text shadow would make the light faces easier to read.',42,84,1100,10.8,15,MUTED)
end('Pixel scale and contrast')

for n,r in enumerate(REVIEWS,4):
    C.bookmarkPage('audit-'+r['id']);s=CASES[r['id']];k=Counter(f['kind'] for f in r['findings'])
    C.addOutlineEntry(r['title'],'audit-'+r['id'],level=0,closed=False)
    start(r['title'],f"Source PDF page {r['source_page']} | Proposed GUI: {r['gui_size'][0]} x {r['gui_size'][1]} | {s['family']} | Native reference: {s['minecraft']} {s['loader']}")
    text('CURRENT MOCKUP / ANNOTATED',42,701,10,TEAL,True)
    text('WHAT LOOKS WRONG / HOW TO CORRECT IT',608,701,10,TEAL,True)
    card(42,370,532,315);annotated(r,(57,382,502,290))
    text('NATIVE CONTEXT',42,349,9.5,TEAL,True);text('DETAIL FROM CURRENT MOCKUP',313,349,9.5,TEAL,True)
    card(42,184,250,154);card(306,184,268,154)
    image_in(ROOT/s['native_report_crop'],(49,190,236,142));image_in(detail_image(r),(314,190,252,142))
    yy=674
    for i,f in enumerate(r['findings'],1):yy=finding_block(f,i,608,yy,540)
    assert yy>=175,(r['id'],'findings overflow',yy)
    y=para('Keep: '+r['keep'],42,151,1090,11.2,16,TEAL)
    y=para('Acceptance: '+r['accept'],42,y-5,1090,11.2,16,INK)
    assert y>64,(r['id'],'bottom overflow',y)
    text('Overview fitted for annotation; use page 3 for an equal-unit size comparison.',42,58,8.5,MUTED)
    end(r['title'])

start('Correction order','A consistent rebuild is preferable to another round of isolated crops. These are design corrections, not changes applied by this report.')
groups=[
 ('1 / Repair geometry and remove artifacts','Restore the Schematic Table field and refresh bounds; recover the Instruction Editor reference-cell edge; clean Stock Keeper footer/rack remnants; remove partial native frame strips and copied footer backgrounds. Use real widget/sprite bounds, not just item positions.'),
 ('2 / Resolve the visual system','Choose continuous native materials or complete insets, derive a coherent palette, unify button metrics/edges and fix pale text contrast. Follow SIU title-control availability rules for each screen type. Preserve native/custom titles and compact item identity.'),
 ('3 / Tighten structure and actions','Revisit one-action footers, label distinct native operations clearly and show attached editor relationships. Keep destructive reset/delete visually subordinate. Compare proposed size at a shared GUI scale before deciding a design is compact.'),
 ('4 / Review real data states','Populate both filter rows, all frequency pairs, a full request basket, multiple categories and bindings, and a scrolling schedule. Review long names/addresses, dropdowns, tooltips and state-specific controls before implementation.')]
y=679
for head,body in groups:
    text(head,42,y,16,TEAL,True);y=para(body,42,y-29,1090,13,20)-40
para('Completion criterion: every visible widget has complete borders and clear ownership of its background; every proposed interaction state has an actual native reference and a reviewed layout; the report no longer relies on prose to promise unshown designs.',42,98,1090,12,18,INK,True)
end('Correction order')

start('Native details that should not be “fixed” away','Several unusual shapes are native behavior/art. They need faithful rendering, not replacement with generic cells.')
protected=[
 ('Blueprint auxiliary result/reference cell','The smaller cell overlaps the larger result in the original. Preserve the actual native cell roles and overlap unless native behavior provides a reason to change them.'),
 ('Toolbox compartments','Eight summaries represent grouped internal storage. The empty ring is not evidence that 24 ordinary slots have been lost.'),
 ('Controller four-plus-two grouping','The separation/divider comes from native control grouping. It is not a stray line to erase just because the outer window changes.'),
 ('Typewriter blank caps and frequency colors','Blank keycaps are also visible in the native reference. Mapping/hover states need review; do not paint arbitrary letters or treat the red/blue frequency surfaces as random decorative colors.'),
 ('Weights and rule-count badge','Weighted Shuffle Filter shows weight 100, not a stack count. The Attribute Filter badge count of one is present in the final asset; its issue is presentation/context, not a proven missing count.'),
 ('Already-corrected edges','The final Frogport grid has its bottom border. The final Key Editor no longer contains the older stray top stripe. Older contact sheets are not the final artifact and were not used as current-defect evidence.')]
for i,(head,body) in enumerate(protected):
    x=42 if i<3 else 608;j=i if i<3 else i-3;y=679-j*185
    text(head,x,y,13,TEAL,True);para(body,x,y-26,520,12.5,18.5)
para('These checks prevent visual cleanup from erasing real native meaning. Static images cannot establish networking, click routing, live animation or save/cancel behavior.',42,89,1090,12,18,MUTED)
end('Preserve native meaning')

start('Review evidence and coverage','All 27 proposed views are accounted for. The scope is visual analysis of the final artifact, not gameplay certification.')
text('Reviewed source',42,675,14,TEAL,True)
para('output/pdf/siu-create-family-window-design-review.pdf',42,651,1090,12,18)
para('SHA-256: '+DATA['source_sha256'],42,628,1090,10.5,16,MUTED)
text('Method',42,582,14,TEAL,True)
para('Re-rendered the 33-page source PDF, inspected all 27 final mockup assets and compared each against its native capture. Verified native and mockup hashes. Inspected selected widgets at enlarged pixel scale and checked the composition crop bounds. Findings are categorized as visible artifacts, design concerns or state checks.',42,554,1090,13,20)
text('Per-view coverage / source PDF page numbers',42,479,14,TEAL,True)
for i,r in enumerate(REVIEWS):
    col=i//14;j=i%14;x=42+col*566;y=451-j*20
    text(f"{r['source_page']:02d}  {r['title']}",x,y,10.5)
text('Limits and saved audit data',42,144,14,TEAL,True)
para('No UI implementation was changed and no gameplay suite was run. Dynamic behavior and unshown states remain unverified. output/create-ui-visual-audit/findings.json preserves every finding, annotation region, source-page reference and acceptance note. This report does not replace the original PDF or its evidence.',42,116,1090,11.5,17,MUTED)
end('Evidence and coverage')
C.save()

# Explicitly avoid interpolation on pixel UI image XObjects.
reader=PdfReader(TMP/'audit-unoptimized.pdf');writer=PdfWriter();writer.clone_document_from_reader(reader)
for page in writer.pages:
    resources=page.get('/Resources',{});xs=resources.get('/XObject',{})
    for obj in xs.values():
        obj=obj.get_object()
        if obj.get('/Subtype')=='/Image':obj[NameObject('/Interpolate')]=BooleanObject(False)
with PDF.open('wb') as f:writer.write(f)
(OUT/'page-index.json').write_text(json.dumps({'pdf':str(PDF.relative_to(ROOT)),'pages':PAGES},indent=2)+'\n')
print(json.dumps({'pdf':str(PDF),'pages':len(PAGES),'views':len(REVIEWS),'finding_categories':dict(COUNTS)},indent=2))
