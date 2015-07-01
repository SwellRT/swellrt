FROM java:7
MAINTAINER P2Pvalue UCM Team <p2pv@ucm.es>

# SwellRT port
EXPOSE 9898

ENV home /usr/local/swellrt
ENV config_files server.config wiab-logging.conf jaas.config server.federation.config

RUN adduser --system --home $home swellrt \
    && addgroup --system swellrt

WORKDIR $home

RUN mkdir config log \
    && for i in $config_files ; do ln -s $home/config/$i . ; done

# I could not find a dawn way to set this in an ENV variable
ADD server.config wiab-logging.conf jaas.config server.federation.config $home/config/

ADD docker/* $home/
ADD war $home/war/
ADD dist/wave*jar $home/wave.jar

RUN chown -R swellrt:swellrt $home

VOLUME $home/config $home/log

USER swellrt

ENTRYPOINT ["./server.sh"]
