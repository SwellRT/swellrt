# Copyright 2009 Google Inc. All Rights Reserved.
# Author: ohler@google.com (Christian Ohler)
JING=~/src/jing-20081028/bin/jing.jar

default: wavespec.html

.PHONY: clean
clean:
	-rm wavespec.html waveschema.xml

wavespec.html: wavespec.xml waveschema.rnc ../../src/org/waveprotocol/wave/federation/federation.protodevel
# If there's a syntax error, xml2rfc will, by default, bring up a
# dialog window if DISPLAY is set.  We just want the message on
# stderr, so we run it without DISPLAY.
	DISPLAY= xml2rfc $< $@

wavespec.xml: wavespec.rst
	python ../rst2rfc.py --traceback wavespec.rst > wavespec.xml

testrnc:
	java -jar $(JING)  -c waveschema.rnc test/*.xml
