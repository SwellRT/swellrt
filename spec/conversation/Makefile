# Copyright 2009 Google Inc. All Rights Reserved.
# Author: ohler@google.com (Christian Ohler)

default: convspec.html

.PHONY: clean
clean:
	-rm convspec.html

convspec.html: convspec.xml 
# If there's a syntax error, xml2rfc will, by default, bring up a
# dialog window if DISPLAY is set.  We just want the message on
# stderr, so we run it without DISPLAY.
	DISPLAY= xml2rfc $< $@

convspec.xml: convspec.rst
	python ../rst2rfc.py convspec.rst > convspec.xml
