
 java \
 -Xms512m \
 -Xmx1024m \
 -Xss256k \
 -XX:MetaspaceSize=256m \
 -XX:MaxMetaspaceSize=256m \
 -Dlogging.path=${logging.path} \
 -Dlogging.file=archimedes.log \
 -jar \
 archimedes-${1.0.0}.jar