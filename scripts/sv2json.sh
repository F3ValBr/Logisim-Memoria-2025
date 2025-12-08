#!/usr/bin/env bash
set -e

if [ -z "$1" ]; then
  echo "Uso: $0 archivo.sv [salida.json]"
  exit 1
fi

IN="$1"
OUT="${2:-${IN%.*}.json}"   # si no pasas segundo argumento, usa mismo nombre pero .json

yosys -q -p "
  read_verilog -sv \"$IN\";
  hierarchy -auto-top;
  proc;
  opt;
  memory_collect;
  opt;
  write_json \"$OUT\";
"
echo "JSON escrito en: $OUT"
