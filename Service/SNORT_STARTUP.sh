#!/bin/bash
sudo snort -A console -i wlp3s0  -l ./logSnort -c /etc/snort/snort.conf 
