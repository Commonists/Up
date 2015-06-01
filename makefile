# Make does not offer a recursive wildcard function, so here's one:
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))

# How to recursively find all files that match a pattern
ALL_JAVAS := $(call rwildcard,org/,*.java)

all: up
up: compile up.jar
	@echo "Done!"

compile:
	javac $(ALL_JAVAS)

up.jar:
	jar cmf MANIFEST.MF up.jar org

clean: FRC
	-rm *.jar

# This pseudo target causes all targets that depend on FRC
# to be remade even in case a file with the name of the target exists.
# This works with any make implementation under the assumption that
# there is no file FRC in the current directory.
FRC:
