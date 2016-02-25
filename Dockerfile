FROM java:7
MAINTAINER P2Pvalue UCM Team <p2pv@ucm.es>

# SwellRT port
EXPOSE 9898

ENV home /usr/local/swellrt
ENV config_files server.config wiab-logging.conf jaas.config server.federation.config \
event.dispatch.config event.rules.config

RUN adduser --system --home $home swellrt \
    && addgroup --system swellrt

WORKDIR $home

RUN mkdir config log sessions attachments \
    && for i in $config_files ; do ln -s $home/config/$i . ; done \
    && ln -s $home/sessions $home/_sessions \
    && ln -s $home/attachments $home/_attachments

# I could not find a dawn way to set this in an ENV variable
# TODO: unify with config files
# Some of the files are distributed with a sample, and others are
# checked-in the code
# All of them should be in sample form and docker/config should
# contain save settings for the docker image
ADD server.config wiab-logging.conf jaas.config server.federation.config $home/config/

ADD docker/config/* $home/config/
ADD docker/home/* $home/
ADD war $home/war/
ADD dist/wave-in-a-box-server*jar $home/wave.jar

RUN chown -R swellrt:swellrt $home

VOLUME $home/config $home/log $home/sessions $home/attachments

USER swellrt

ENTRYPOINT ["./server.sh"]
