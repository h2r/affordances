NDIR=$(dirname "$(readlink -fn "$0")")
 cd "$BINDIR"
 java -Xmx1024M -jar craftbukkit-1.5.2-R1.0.jar -o true
