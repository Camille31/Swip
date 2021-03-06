#!/bin/bash


# PREPROCESSING from RAW UTF-8 text
#----------------------------------

# - tokenization, compounds, segmentation into sentences
# Note : newlines are systematically interpreted as sentence frontiers.

# ----------------------------------------------
PREBIN=$BONSAI/src
MYPREBIN=$BONSAI/my_src

#set -x

# input = raw text
# -- tokenize, get compounds, segment into sentences
$PREBIN/do_tok.pl | $PREBIN/do_compounds.pl | $MYPREBIN/consume_gazetteer.py  | $PREBIN/do_seg.pl

