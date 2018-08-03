#!/vendor/bin/sh

rm /data/misc/camera/client_package_name
ln -s /etc/camera/client_package_name /data/misc/camera/client_package_name
chown cameraserver.audio /etc/camera/client_package_name
