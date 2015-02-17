#!/bin/bash

echo_success() {
  echo -n $"[ OK ]"
  return 0
}

echo_failure() {
  echo -n $"[FAILED]"
  return 1
}

