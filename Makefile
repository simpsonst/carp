## Copyright 2021,2022, Lancaster University
## All rights reserved.
## 
## Redistribution and use in source and binary forms, with or without
## modification, are permitted provided that the following conditions are
## met:
## 
##  * Redistributions of source code must retain the above copyright
##    notice, this list of conditions and the following disclaimer.
## 
##  * Redistributions in binary form must reproduce the above copyright
##    notice, this list of conditions and the following disclaimer in the
##    documentation and/or other materials provided with the
##    distribution.
## 
##  * Neither the name of the copyright holder nor the names of its
##    contributors may be used to endorse or promote products derived
##    from this software without specific prior written permission.
## 
## THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
## "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
## LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
## A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
## OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
## SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
## LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
## DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
## THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
## (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
## OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
## 
## 
## Author: Steven Simpson <https://github.com/simpsonst>

all::

FIND=find
PRINTF=printf
XARGS=xargs
SED=sed

PREFIX=/usr/local

VWORDS:=$(shell src/getversion.sh --prefix=v MAJOR MINOR PATCH)
VERSION:=$(word 1,$(VWORDS))
BUILD:=$(word 2,$(VWORDS))

## Provide a version of $(abspath) that can cope with spaces in the
## current directory.
myblank:=
myspace:=$(myblank) $(myblank)
MYCURDIR:=$(subst $(myspace),\$(myspace),$(CURDIR)/)
MYABSPATH=$(foreach f,$1,$(if $(patsubst /%,,$f),$(MYCURDIR)$f,$f))

-include $(call MYABSPATH,config.mk)
-include carp-env.mk

SELECTED_JARS += carp_annot
trees_carp_annot += annot

SELECTED_JARS += carp_model
trees_carp_model += model

SELECTED_JARS += carp_modelsyn
trees_carp_modelsyn += modelsyn

SELECTED_JARS += carp_syntax
trees_carp_syntax += syntax

SELECTED_JARS += carp_aproc
trees_carp_aproc += aproc

SELECTED_JARS += carp_core
trees_carp_core += core

SELECTED_JARS += carp_rt
trees_carp_rt += runtime

trees_tests += tests
roots_tests=$(found_tests)
deps_tests += annot
deps_tests += syntax
deps_tests += model
deps_tests += modelsyn
statics_tests += uk/ac/lancs/carp/syntax/example1.rpc

roots_runtime=$(found_runtime)
deps_runtime += core
deps_runtime += annot
deps_runtime += model

roots_modelsyn=$(found_modelsyn)
deps_modelsyn += syntax
deps_modelsyn += model
deps_modelsyn += annot

roots_model=$(found_model)
deps_model += syntax
deps_model += annot

roots_annot=$(found_annot)

roots_aproc=$(found_aproc)
deps_aproc += annot
deps_aproc += model
deps_aproc += syntax
deps_aproc += modelsyn

roots_syntax=$(found_syntax)
statics_syntax += uk/ac/lancs/carp/syntax/doc/ents.properties

roots_core=$(found_core)
deps_core += model
deps_core += syntax
deps_core += annot

JARDEPS_SRCDIR=src/java/tree
JARDEPS_MERGEDIR=src/java/merge


jars += $(SELECTED_JARS)

DOC_PKGS += uk.ac.lancs.carp.deploy
DOC_PKGS += uk.ac.lancs.carp.component
DOC_PKGS += uk.ac.lancs.carp.component.std
DOC_PKGS += uk.ac.lancs.carp.codec
DOC_PKGS += uk.ac.lancs.carp.codec.std
DOC_PKGS += uk.ac.lancs.carp.model
DOC_PKGS += uk.ac.lancs.carp.model.std
DOC_PKGS += uk.ac.lancs.carp.model.syntax
DOC_PKGS += uk.ac.lancs.carp.model.syntax.std
DOC_PKGS += uk.ac.lancs.carp.syntax
DOC_PKGS += uk.ac.lancs.carp.syntax.doc
DOC_PKGS += uk.ac.lancs.carp.map
DOC_PKGS += uk.ac.lancs.carp.errors
DOC_PKGS += uk.ac.lancs.carp.runtime
DOC_PKGS += uk.ac.lancs.carp

DOC_OVERVIEW=src/java/overview.html
DOC_CLASSPATH += $(jars:%=$(JARDEPS_OUTDIR)/%.jar)
DOC_SRC=$(call jardeps_srcdirs4jars,$(jars))
DOC_CORE=carp

include jardeps.mk
-include jardeps-install.mk

blank:: clean

clean:: tidy

tidy::
	@$(PRINTF) 'Removing detritus\n'
	@$(FIND) . -name "*~" -delete

all:: installed-jars

installed-jars:: $(SELECTED_JARS:%=$(JARDEPS_OUTDIR)/%.jar)
installed-jars:: $(SELECTED_JARS:%=$(JARDEPS_OUTDIR)/%-src.zip)

install:: install-jars

install-jars:: $(SELECTED_JARS:%=install-jar-%)

install-jar-%::
	@$(call JARDEPS_INSTALL,$(PREFIX)/share/java,$*,$(version_$*))


all:: BUILD VERSION
distclean:: blank
	$(RM) BUILD VERSION
MYCMPCP=$(CMP) -s '$1' '$2' || $(CP) '$1' '$2'
.PHONY: prepare-version
mktmp:
	@$(MKDIR) tmp/
prepare-version: mktmp
	$(file >tmp/BUILD,$(BUILD))
	$(file >tmp/VERSION,$(VERSION))
BUILD: prepare-version
	@$(call MYCMPCP,tmp/BUILD,$@)
VERSION: prepare-version
	@$(call MYCMPCP,tmp/VERSION,$@)

YEARS=2021,2022

update-licence:
	$(FIND) . -name '.git' -prune -or -type f -print0 | $(XARGS) -0 \
	$(SED) -i 's/Copyright\s\+[0-9,]\+\sLancaster University/Copyright $(YEARS), Lancaster University/g'

test-syntax: $(JARDEPS_OUTDIR)/tests.jar
test-syntax: CLASSPATH += $(JARDEPS_OUTDIR)/tests.jar
test-syntax: $(JARDEPS_OUTDIR)/carp_annot.jar
test-syntax: CLASSPATH += $(JARDEPS_OUTDIR)/carp_annot.jar
test-syntax: $(JARDEPS_OUTDIR)/carp_syntax.jar
test-syntax: CLASSPATH += $(JARDEPS_OUTDIR)/carp_syntax.jar
test-syntax: $(JARDEPS_OUTDIR)/carp_model.jar
test-syntax: CLASSPATH += $(JARDEPS_OUTDIR)/carp_model.jar
test-syntax: $(JARDEPS_OUTDIR)/carp_modelsyn.jar
test-syntax: CLASSPATH += $(JARDEPS_OUTDIR)/carp_modelsyn.jar

test-syntax:
	java -ea -cp "$(subst $(jardeps_space),:,$(CLASSPATH))" \
		uk.ac.lancs.carp.syntax.TestSyntax

test-names: $(JARDEPS_OUTDIR)/carp_annot.jar
test-names: CLASSPATH += $(JARDEPS_OUTDIR)/carp_annot.jar

test-names:
	java -ea -cp "$(subst $(jardeps_space),:,$(CLASSPATH))" \
		uk.ac.lancs.carp.map.ExternalName

JAVACFLAGS += -Xlint:deprecation
