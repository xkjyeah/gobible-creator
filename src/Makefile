
# generate this with find -name '*.java | sed -e 's@^\./@@'
JAVA_FILES=OsisConverter.java \
usfm/MacroTextSymbol.java \
usfm/NumericTextSymbol.java \
usfm/SymbolType.java \
usfm/TagOpenSymbol.java \
usfm/TagCloseSymbol.java \
usfm/TextSymbol.java \
usfm/USFMSymbol.java \
USFMLex.java \
USFMParse.java \
GoBibleCreator.java \
jolon/xml/CDATA.java \
jolon/xml/XMLAttribute.java \
jolon/xml/XMLCompatible.java \
jolon/xml/XMLObject.java \
jolon/xml/XMLParser.java \
MyManifest.java \
ThmlConverter.java

GoBibleCreator.jar: compile version_number Manifest USFMSettings.txt
	jar cfm GoBibleCreator.jar Manifest *.class jolon usfm USFMSettings.txt version gobiblecreator

.PHONY: version_number compile version_number

compile: $(JAVA_FILES)
	javac $(JAVAC_FLAGS) $(JAVA_FILES)

version_number:
	mkdir -p version
	[ ! x$(RELEASE) = x1 ] && echo "SVNRevision: `svn info | grep 'Revision' | awk ' { print $$2 }'`" > gobiblecreator/version_info \
	|| echo "" > gobiblecreator/version_info


USFMLex.java: usfm.flex
	jflex -d . usfm.flex

clean:
	rm *.class
	rm usfm/*.class
	       rm version/*.class
	rm GoBibleCreator.jar
