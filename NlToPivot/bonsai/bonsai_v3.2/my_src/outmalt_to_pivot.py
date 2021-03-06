#!/usr/bin/env python

import fileinput
import re

def get_string_for_id( searched_id ):
	regex = re.compile(searched_id + '	+[^\t]+	+([^\t]+)	+[A-Z]+	+[A-Z]+	+[^\t]+	+[01_]+	+(\d+)	+[^\t]+')
	result = regex.search(input)
	if result:
		return result.group(1)
	return null;


input = ""
for line in fileinput.input():
	input = input + line


# t1 --obj-> t2 --dep -> t3
# => t1: t3
regex_t2 = re.compile('(\d+)	+[^\t]+	+[^\t]+	+[A-Z]+	+[A-Z]+	+[^\t]+	+[01_]+	+(\d+)	+dep')
t2_lines = regex_t2.findall(input)
for t2_line in t2_lines:
	t2id = t2_line[0]
	t3id = t2_line[1]
	regex_t1 = re.compile('\d+	+[^\t]+	+([^\t]+)	+[A-Z]+	+[A-Z]+	+[^\t]+	+[01_]+	+' + t2id + '	+obj')
	t1_lines = regex_t1.findall(input)
	for t1_string in t1_lines:
		print t1_string + ": " + get_string_for_id( t3id ) + ". "


# t1 --mod-> t2
# => t1: t2
regex_t2 = re.compile('(\d+)	+[^\t]+	+[^\t]+	+[A-Z]+	+[A-Z]+	+[^\t]+	+[01_]+	+(\d+)	+mod')
t2_lines = regex_t2.findall(input)
for t2_line in t2_lines:
	t1id = t2_line[0]
	t2id = t2_line[1]
	print get_string_for_id( t1id ) + ": " + get_string_for_id( t2id )
