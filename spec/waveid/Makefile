# Copyright 2010 Google Inc. All Rights Reserved.
# Author: jcgregorio@google.com (Joe Gregorio)

default: waveidspec.html

.PHONY: clean
clean:
	-rm waveidspec.html
	-rm waveidspec.xml

waveidspec.html: waveidspec.xml
# If there's a syntax error, xml2rfc will, by default, bring up a
# dialog window if DISPLAY is set.  We just want the message on
# stderr, so we run it without DISPLAY.
	DISPLAY= xml2rfc $< $@

waveidspec.xml: waveidspec.rst
	python ../rst2rfc.py waveidspec.rst > waveidspec.xml
