FROM openjdk:11

#Installing virtmanager to be able to use kvm/qemu plugin
RUN apt-get update && apt-get -y install apt-utils && DEBIAN_FRONTEND=noninteractive apt-get -y install qemu-kvm libvirt-clients libvirt-daemon-system

RUN mkdir /app && mkdir /data

ADD ./*.jar /app/homedash.jar

ADD run.sh /run.sh

EXPOSE 4567
EXPOSE 4570


VOLUME "/data"


RUN chmod +x /run.sh

CMD "./run.sh"