# anyedittools
"Swiss knife" for Eclipse

[AnyEdit plugin](http://andrei.gmxhome.de/anyedit/) adds several new tools to the context menu of text- based Eclipse editors, to output consoles, to Eclipse main menu and editor toolbar. 

* AnyEdit contributes Import/Export working sets wizards.

* AnyEdit adds four new context menu actions to "Compare With" and "Replace With" menus in both editors and files.

* AnyEdit can show the withespace (tabs vs spaces) in editors and may use custom project settings for tab`<->`spaces auto-convert-on-save feature.
* AnyEdit can perform tabs `<->` spaces convert on multiple files or entire directories and supports also file exclusion filter for tabs `<->` spaces action.
* AnyEdit allows you automatically remove trailing whitespaces and/or perform tabs `<->` spaces conversion on a "save" action in all text- based Eclipse editors. It could automatically create a new line at the end of the file if the last line was not terminated by new line.
* AnyEdit adds "Save All", "Open File" and "Color Whitespace in Editors" buttons to the global Eclipse toolbar and can remove "Print" button from it (because it is completely useless on Linux).
* AnyEdit adds "Open File", "Open Type" and "Save to file..." actions to supported output consoles and "Save to file..." toolbar button to the Console view.

AnyEdit adds "Open ... " context menu actions to supported text editors:
* "Open file under cursor"
  - useful by many include's in jsp's, file name/line in log messages etc. 
  - Works with run/debug and Ant console too, support "jump to line"!
* "Open type under cursor"
  - Useful for all xml's, tld's, jsp's etc files with referenced Java types. 
  - Works with run/debug and Ant console too!

AnyEdit adds "Convert ... " context menu actions to supported text editors:
* "Convert `->` Leading tabs to spaces"
  - (trailing whitespace will be removed automatically)
* "Convert `->` Leading spaces to tabs" 
  - (trailing whitespace will be removed automatically)
* "Convert `->` Chars to Html entities"
* "Convert `->` Html entities to chars"
  - useful if working in bilingual team to convert é's, ô's, ä's etc between html and other sources ;)
* "Convert `->` Camel `<->` Underscores" (convert words from/to "camel" notation, like thisIsMyFieldName `<->` this\_is\_my\_field\_name)
* "Convert `->` Camel `<->` Pascal" (convert words from "Camel" to "Pascal" notation and back, like
myVariable `<->` MyVariable)
* "Convert `->` Capitalize"
* "Convert `->` Invert case"
* "Convert `->` To upper case"
* "Convert `->` To lower case"
* "Convert `->` To / From Unicode notation"
* "Convert `->` To / From Base64"

*Latest version of AnyEdit requires Eclipse >= 3.8 and Java >= 1.7.*

You can install the latest plugin version from [Eclipse Marketplace](https://marketplace.eclipse.org/content/anyedit-tools).

Old versions can be downloaded from [bintray](https://bintray.com/iloveeclipse/plugins/AnyEdit/view/files).
The code was originally hosted on [code.google.com/p/anyedittools](http://code.google.com/p/anyedittools).

P.S.

If you have found AnyEdit plugin helpful, you can send me some money for a ~~beer~~ glass of wine via https://www.paypal.me/AndreyLoskutov. I do not rely on this but this feedback makes me think I do something useful.
