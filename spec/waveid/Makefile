# Copyright 2010 Google Inc. All Rights Reserved.
# Author: jcgregorio@google.com (Joe Gregorio)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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
