# OnkyoRemote
http://obive.net/software/onkyoremote

This shows a menu in the menubar to control Onkyo and Integra receivers and preamplifiers.

# Dependencies
OnkyoRemote depends on a few forked libraries:

https://github.com/idontusenumbers/appbundle-maven-plugin
https://github.com/idontusenumbers/jEISCP

And a library-not-yet-in-maven:
https://github.com/blackears/svgSalamander

# Build
You'll need to grab a copy of all those dependencies and install them to your local maven

Then run `mvn package`

You should get a `.app` in the `target`

# License
Due to dependence on jEISCP, this code is under [GPL v3](http://www.gnu.org/licenses/gpl-3.0.html) until further notice.