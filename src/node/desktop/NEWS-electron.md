## RStudio "Electron" Release Notes

- NOTE: Once verified against an Electron build move into the actual NEWS file
  (tracked by https://github.com/rstudio/rstudio/issues/9429).

### Desktop-Specific Bugfixes

- RStudio Desktop on Linux crashes when switching between monitors (#1743)
- macOS display switching leaves blurred UI (#2282)
- menu bar text too small (1.2.637 with dual monitors) (#2905)
- Text scaling on Windows 10 doesn't seem to work at 125% (#2914)
- RStudio: scaling on second monitor (#3966)
- Rstudio Desktop Windows doesn't display .mp4 files (#4019)
- RStudio Crashes after it is started Crashed Thread: Chrome_InProcGpuThread (#4034)
- Cursor persists on previous line with new OpenGL rendering engine (#4390)
- Paste a file from Windows Explorer (#4572)
- Pressing "alt gr" jumps one character back (#4584)
- Scroll bar has a habit of sticking (#4691)
- RStudio Desktop 1.2.1335 on 4K dell laptop screen issue (#4834)
- VoiceOver not properly tracking focus for Mac Desktop IDE (#4862)
- Failure when "copy image" to word (#5103)
- RStudio crash after mac wakes up (#5221)
- Support RStudio on Wayland (#5333)
- Zeroes display differently before & after decimal point (#5664)
- RStudio crashes on Mac after disconnecting external monitors/dock (#5749)
- Checked menu items not read correctly by Narrator/NVDA on Windows (#5833)
- Live regions not being announced on Windows Desktop IDE (#5895)
- Cannot switch NVDA into navigator mode in Windows IDE (#5912)
- Appearance pane (in Options) takes a long time to load (#6268)
- NVDA / Narrator misreading menu accelerator keys (#6406)
- No scrollbar in source editor on Mac desktop (#6432)
- Shiny numeric inputs increment twice on arrow click (#6953)
- JAWS doesn't read "Alt" when it is the first thing in a menu shortcut (#6456)
- RStudio Desktop on Linux stores state in folders differing only in case (#6979)
- Long lines are printed as a "K" (#6995)
- RStudio crashes when laptop computer is suspended (#7034)
- On macOS paste shortcut fails in Save Dialogue (#7125)
- Keep brightness of editor pane text regardless of active line (#7651)
- Second monitor ON/OFF crashes Rstudio (#7673)
- 5-6 tab key hits to get focus on filename input box in save file dialog (#7748)
- Special characters from Windows pop-up tool are not inserted (#7954)
- Incorrectly colored items in additional source panes for dark themes (#8016)
- Separator lines are missing from the IDE Toolbar's New File menu on MacOS 11 Big Sur (#8130)
- Typing gets laggy over time on macOS (#8442)
- Scrolling leads to brighter-colored and bolder text (#8853)
- Crash at startup: OpenGL context creation failed (#9451)
- Source pane vertical scrollbox unusably thin on multi-monitor set-up (#9534)
- Sometimes the arrows don't appear when clicking on a pane border in RStudio Desktop (#9723)
- HTML input with type='checkbox'/'radio' doesn't render correctly in viewer (#9730)
- Password UI removes single space after commas and periods (#10164)
- Improve accessibility of mutually-exclusive menu items (#10876)
