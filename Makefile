SVNVERSION = $(svn info src | grep Revision | awk '{print $$2}')
PACKAGE_NAME = GoBibleCreator-svn.zip

GBCORE_FILES= GoBibleCore2.jar ui.properties Icon.png MANIFEST.MF
REFERENCE_FILES = USFMSettings.txt GBC_2.4_Readme.txt collections_example.txt  README

$(PACKAGE_NAME): dist/GoBibleCreator.jar dist
	-rm $(PACKAGE_NAME)
	cd dist; zip ../$(PACKAGE_NAME) `find . -type f -not -path '*/.svn/*' -and -not -name '.htaccess'`

dist/GoBibleCreator.jar: compile otherfiles dist
	jar cfm dist/GoBibleCreator.jar src/Manifest -C bin .

.PHONY: otherfiles clean test dist bin

compile: bin
	make -C src version_number
	JAVAC_FLAGS="-d ../bin" make -C src compile

bin:
	-rm -rf bin/
	mkdir -p bin

dist: 
	-rm -rf dist/
	mkdir -p dist/GoBibleCore
	mkdir -p dist/Reference
	# icons, reference, gobiblecore
	cp -r icons dist/Icons
	for f in $(REFERENCE_FILES); do cp src/$$f dist/Reference/`basename $$f` ; done
	for f in $(GBCORE_FILES); do cp src/GoBibleCore/$$f dist/GoBibleCore/`basename $$f` ; done

otherfiles:
	mkdir -p bin/gobiblecreator
	cp src/gobiblecreator/version_info bin/gobiblecreator

test:
	make -C tests

clean:
	-rm -rf bin dist

