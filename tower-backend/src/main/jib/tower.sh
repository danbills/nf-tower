# Launch backend server
exec java \
  -Dcom.sun.management.jmxremote \
  -Dmicronaut.config.files=tower.yml \
  ${JAVA_OPTS} \
  -cp /app/resources:/app/classes:/app/libs/* \
  io.seqera.tower.Application
