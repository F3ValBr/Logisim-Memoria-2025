#!/usr/bin/env python3
import subprocess
import sys
import os

def main():
    if len(sys.argv) < 2:
        print(f"Uso: {sys.argv[0]} archivo.sv [salida.json]")
        sys.exit(1)

    in_file = sys.argv[1]

    # si no hay segundo argumento, reemplazar extensión por .json
    if len(sys.argv) >= 3:
        out_file = sys.argv[2]
    else:
        base, _ = os.path.splitext(in_file)
        out_file = base + ".json"

    # comando yosys equivalente al de bash
    yosys_cmd = [
        "yosys",
        "-q",
        "-p",
        f"""
            read_verilog -sv "{in_file}";
            hierarchy -auto-top;
            proc;
            opt;
            memory_collect;
            opt;
            write_json "{out_file}";
        """
    ]

    try:
        subprocess.run(yosys_cmd, check=True)
        print(f"JSON escrito en: {out_file}")
    except FileNotFoundError:
        print("ERROR: Yosys no está instalado o no está en el PATH.")
        sys.exit(1)
    except subprocess.CalledProcessError:
        print("ERROR: Yosys falló durante la conversión.")
        sys.exit(1)

if __name__ == "__main__":
    main()
