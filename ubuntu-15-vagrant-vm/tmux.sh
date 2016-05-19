#!/bin/bash

tmux has-session -t LOG
if [[ $? != 0 ]] ; then
  tmux detach
  tmux new-session -s LOG -n infoshare-demo -d
  # kibana output
  tmux split-window -v -p 50 -t LOG 
  tmux send-keys -t LOG:0.1 'journalctl -f --since "10 minutes ago" -o cat _UID=`id -u elasticsearch`' C-m
  # elasticsearch output
  tmux split-window -v -p 50 -t LOG 
  tmux send-keys -t LOG:0.2 'journalctl -f --since "10 minutes ago" -o cat _UID=`id -u kibana`' C-m
  tmux send-keys -t LOG:0.0 'clear ; echo "infoshare" | figlet' C-m
  tmux select-pane -t LOG:0.0
fi
tmux attach -t LOG
