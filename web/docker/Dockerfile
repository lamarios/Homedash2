FROM openjdk:10

RUN mkdir /app && mkdir /data

ADD ./*.jar /app/homedash.jar

ADD run.sh /run.sh

EXPOSE 4567

VOLUME "/data"


RUN chmod +x /run.sh

CMD "./run.sh"