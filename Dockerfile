FROM java:8
MAINTAINER SwellRT Community <swellrt@gmail.com>

# SwellRT port
EXPOSE 9898
# JVM Remote Monitoring
EXPOSE 5000

ENV home /usr/local/swellrt/

RUN adduser --system --home $home swellrt \
    && addgroup --system swellrt

WORKDIR $home

# Create all possible folders used in SwellRT server
# Actually most of them are not used as long as MongoDB is
# the default storage but this way is more robust.

RUN mkdir config \
 logs \
 sessions \ 
 avatars \ 
 attachments \
 certificates \
 accounts \
 deltas \
 indexes

# Add config files
ADD wave/config  $home/config/

# Add some scripts
ADD scripts/docker/home/* $home/

# Add static resources
ADD wave/war $home/war/

# Add static resources
ADD wave/webapp $home/webapp/

# Add runtime
ADD wave/build/libs/swellrt.jar $home/swellrt.jar

# Set permissions
RUN chown -R swellrt:swellrt $home

# All folders can be exported
VOLUME $home/config \
 $home/logs \
 $home/sessions \
 $home/avatars \
 $home/attachments \
 $home/certificates \
 $home/accounts \
 $home/deltas \
 $home/indexes \
 $home/webapp
 

USER swellrt

ENTRYPOINT ["./server.sh"]
