FROM java:7
MAINTAINER P2Pvalue UCM Team <p2pv@ucm.es>

ENV home /usr/local/swellrt

RUN adduser --system --home $home swellrt
RUN addgroup --system swellrt
WORKDIR $home

ADD dist/wave*jar $home/
ADD war $home/war

RUN chown -R swellrt:swellrt $home

USER swellrt


